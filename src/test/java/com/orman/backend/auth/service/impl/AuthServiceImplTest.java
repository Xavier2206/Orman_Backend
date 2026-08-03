package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final Clock UTC_CLOCK = Clock.fixed(Instant.parse("2026-08-03T12:30:45Z"), ZoneOffset.UTC);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Clock clock;
    @InjectMocks private AuthServiceImpl service;

    @Test
    void authenticatesActiveUsuarioAndUpdatesUltimoAccesoInUtc() {
        Usuario usuario = usuario("Usuario.Demo", (short) 1, persona(7, (short) 1));
        when(clock.instant()).thenReturn(UTC_CLOCK.instant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(usuarioRepository.findById("Usuario.Demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-ficticia", "bcrypt-hash")).thenReturn(true);

        LoginResponse response = service.login(new LoginRequest(" Usuario.Demo ", "clave-ficticia"));

        assertThat(response).isEqualTo(new LoginResponse("Usuario.Demo", 7));
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.of(2026, 8, 3, 12, 30, 45));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rejectsUnknownUsuarioAfterExecutingBcryptComparison() {
        when(usuarioRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(" inexistente ", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(anyString(), anyString());
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWrongPasswordAndPreservesUltimoAcceso() {
        Usuario usuario = usuario("usuario.demo", (short) 1, persona(7, (short) 1));
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(" clave-ficticia ", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("usuario.demo", " clave-ficticia ")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(usuario.getUltimoAcceso()).isNull();
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void rejectsInactiveUsuarioInactivePersonaAndMissingPersonaWithoutPersistingAccess() {
        Usuario inactiveUsuario = usuario("usuario.inactivo", (short) 0, persona(7, (short) 1));
        Usuario inactivePersona = usuario("persona.inactiva", (short) 1, persona(8, (short) 0));
        Usuario missingPersona = usuario("sin.persona", (short) 1, null);
        when(usuarioRepository.findById("usuario.inactivo")).thenReturn(Optional.of(inactiveUsuario));
        when(usuarioRepository.findById("persona.inactiva")).thenReturn(Optional.of(inactivePersona));
        when(usuarioRepository.findById("sin.persona")).thenReturn(Optional.of(missingPersona));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("usuario.inactivo", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginRequest("persona.inactiva", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginRequest("sin.persona", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(inactiveUsuario.getUltimoAcceso()).isNull();
        assertThat(inactivePersona.getUltimoAcceso()).isNull();
        assertThat(missingPersona.getUltimoAcceso()).isNull();
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
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
