package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.RefreshTokenService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-03T12:30:45Z");
    private static final UUID SID = UUID.fromString("be6a3aa2-321f-4fb5-b07d-c0ca8adf30f6");

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SesionUsuarioRepository sesionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private ApplicationEventPublisher eventPublisher;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("test-only-secret-with-at-least-32-bytes", "issuer", 15, 30);
        service = new AuthServiceImpl(usuarioRepository, sesionRepository, passwordEncoder, jwtService,
                refreshTokenService, properties, Clock.fixed(NOW, ZoneOffset.UTC), eventPublisher);
    }

    @Test
    void createsMobileSessionWithTrimmedDeviceDataAndHashedRefreshToken() {
        Usuario usuario = usuario("Usuario.Demo", (short) 1, persona(7, (short) 1));
        when(usuarioRepository.findByLoginForUpdate("Usuario.Demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(true);
        when(refreshTokenService.generate(org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenAnswer(invocation -> new RefreshTokenService.GeneratedRefreshToken(
                        invocation.<UUID>getArgument(0) + ".secret", "sha256-hash"));
        when(jwtService.generateAccessToken(anyString(), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn("access-token");

        AuthResult result = service.login(request(" Usuario.Demo ", " device-1 ", " Android ", ClientType.MOBILE));

        ArgumentCaptor<SesionUsuario> captor = ArgumentCaptor.forClass(SesionUsuario.class);
        verify(sesionRepository).save(captor.capture());
        SesionUsuario persisted = captor.getValue();
        assertThat(persisted.getDeviceId()).isEqualTo("device-1");
        assertThat(persisted.getDeviceName()).isEqualTo("Android");
        assertThat(persisted.getRefreshTokenHash()).isEqualTo("sha256-hash");
        assertThat(persisted.getRefreshTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(persisted.getFechaExpiracion()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).plusDays(30));
        assertThat(result.response().refreshToken()).isEqualTo(result.refreshToken());
        assertThat(result.response().expiresIn()).isEqualTo(900);
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.of(2026, 8, 3, 8, 30, 45));
    }

    @Test
    void replacesOnlyPreviousActiveSessionForSameDevice() {
        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        SesionUsuario previous = session(usuario, "old-hash", "device-1", ClientType.WEB);
        when(usuarioRepository.findByLoginForUpdate("usuario.demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(true);
        when(sesionRepository.findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull("usuario.demo", "device-1"))
                .thenReturn(Optional.of(previous));
        when(refreshTokenService.generate(org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(new RefreshTokenService.GeneratedRefreshToken(SID + ".secret", "new-hash"));
        when(jwtService.generateAccessToken(anyString(), org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn("jwt");

        service.login(request("usuario.demo", "device-1", "Browser", ClientType.WEB));

        assertThat(previous.getMotivoRevocacion()).isEqualTo(RevocationReason.REPLACED_BY_NEW_LOGIN);
        assertThat(previous.getFechaRevocacion()).isNotNull();
        verify(sesionRepository).saveAndFlush(previous);
    }

    @Test
    void rotatesRefreshHashVersionAndLastUse() {
        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        SesionUsuario session = session(usuario, "old-hash", "mobile-1", ClientType.MOBILE);
        when(refreshTokenService.extractSid("old-token")).thenReturn(SID);
        when(sesionRepository.findBySidForUpdate(SID)).thenReturn(Optional.of(session));
        when(refreshTokenService.matches("old-token", "old-hash")).thenReturn(true);
        when(refreshTokenService.generate(SID))
                .thenReturn(new RefreshTokenService.GeneratedRefreshToken("new-token", "new-hash"));
        when(jwtService.generateAccessToken("usuario.demo", SID)).thenReturn("new-access");

        AuthResult result = service.refresh("old-token", ClientType.MOBILE);

        assertThat(session.getRefreshTokenHash()).isEqualTo("new-hash");
        assertThat(session.getRefreshTokenVersion()).isEqualTo(2);
        assertThat(session.getUltimoUso()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(result.response().accessToken()).isEqualTo("new-access");
        assertThat(result.response().refreshToken()).isEqualTo("new-token");
    }

    @Test
    void revokesSessionWhenRefreshDoesNotMatchCurrentHash() {
        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        SesionUsuario session = session(usuario, "current-hash", "mobile-1", ClientType.MOBILE);
        when(refreshTokenService.extractSid("reused-token")).thenReturn(SID);
        when(sesionRepository.findBySidForUpdate(SID)).thenReturn(Optional.of(session));
        when(refreshTokenService.matches("reused-token", "current-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("reused-token", ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(session.getMotivoRevocacion()).isEqualTo(RevocationReason.REFRESH_REUSE);
        verify(jwtService, never()).generateAccessToken(anyString(), org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void marksExpiredSessionAndRejectsInactivePrincipals() {
        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        SesionUsuario expired = session(usuario, "hash", "mobile-1", ClientType.MOBILE);
        ReflectionTestUtils.setField(expired, "fechaExpiracion", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(refreshTokenService.extractSid("expired-token")).thenReturn(SID);
        when(sesionRepository.findBySidForUpdate(SID)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("expired-token", ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(expired.getMotivoRevocacion()).isEqualTo(RevocationReason.EXPIRED);

        SesionUsuario inactive = session(usuario("usuario.demo", (short) 0, persona(7, (short) 1)),
                "hash", "mobile-1", ClientType.MOBILE);
        when(refreshTokenService.extractSid("inactive-token")).thenReturn(SID);
        when(sesionRepository.findBySidForUpdate(SID)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.refresh("inactive-token", ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(inactive.isRevoked()).isFalse();
    }

    @Test
    void rejectsUnknownOrInvalidCredentialsWithoutCreatingSession() {
        when(usuarioRepository.findByLoginForUpdate("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(request(" inexistente ", "device", "Phone", ClientType.MOBILE)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches(anyString(), anyString());
        verify(sesionRepository, never()).save(org.mockito.ArgumentMatchers.any());

        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        when(usuarioRepository.findByLoginForUpdate("usuario.demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(false);
        assertThatThrownBy(() -> service.login(request("usuario.demo", "device", "Phone", ClientType.MOBILE)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsInactiveUserOrPersonWithoutCreatingSession() {
        Usuario inactiveUser = usuario("inactive.user", (short) 0, persona(7, (short) 1));
        when(usuarioRepository.findByLoginForUpdate("inactive.user")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(true);
        assertThatThrownBy(() -> service.login(request("inactive.user", "browser", "Browser", ClientType.WEB)))
                .isInstanceOf(InvalidCredentialsException.class);

        Usuario inactivePerson = usuario("inactive.person", (short) 1, persona(8, (short) 0));
        when(usuarioRepository.findByLoginForUpdate("inactive.person")).thenReturn(Optional.of(inactivePerson));
        assertThatThrownBy(() -> service.login(request("inactive.person", "phone", "Phone", ClientType.MOBILE)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(sesionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void webOwnerLoginAuthenticatesDirectlyAndCreatesSession() {
        Usuario usuario = usuario("owner.web", (short) 1, persona(7, (short) 1));
        when(usuarioRepository.findByLoginForUpdate("owner.web")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(true);
        when(refreshTokenService.generate(org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(new RefreshTokenService.GeneratedRefreshToken(SID + ".secret", "refresh-hash"));
        when(jwtService.generateAccessToken(anyString(), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn("access-token");

        AuthResult result = service.login(request("owner.web", "browser", "Browser", ClientType.WEB));

        assertThat(result.response().status()).isEqualTo("AUTHENTICATED");
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().refreshToken()).isNull();
        assertThat(result.refreshToken()).isEqualTo(SID + ".secret");
        verify(sesionRepository).save(org.mockito.ArgumentMatchers.any(SesionUsuario.class));
    }

    private LoginRequest request(String login, String deviceId, String deviceName, ClientType type) {
        return new LoginRequest(login, "clave-ficticia", deviceId, deviceName, type);
    }

    private SesionUsuario session(Usuario usuario, String hash, String deviceId, ClientType type) {
        LocalDateTime created = LocalDateTime.of(2026, 8, 1, 12, 0);
        return new SesionUsuario(SID, usuario, hash, deviceId, "Device", type, created, created.plusDays(30));
    }

    private Usuario usuario(String login, short estado, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd("bcrypt-hash");
        usuario.setEstado(estado);
        usuario.setPersona(persona);
        return usuario;
    }

    private Persona persona(Integer codper, short estado) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        persona.setEstado(estado);
        return persona;
    }
}
