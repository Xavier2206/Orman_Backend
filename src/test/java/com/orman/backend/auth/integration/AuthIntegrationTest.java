package com.orman.backend.auth.integration;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class AuthIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RolService rolService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void authenticatesWithBcryptNormalizesLoginAndUpdatesUltimoAccesoWithoutRoles() {
        UsuarioResponse usuario = createUsuario("AUTH-LOGIN-1", "Usuario.Auth", "clave-ficticia");

        LoginResponse response = authService.login(new LoginRequest(" Usuario.Auth ", "clave-ficticia"));

        Usuario persisted = usuarioRepository.findById(usuario.login()).orElseThrow();
        assertThat(response).isEqualTo(new LoginResponse("Usuario.Auth", usuario.codper()));
        assertThat(passwordEncoder.matches("clave-ficticia", persisted.getPasswd())).isTrue();
        assertThat(persisted.getUltimoAcceso()).isNotNull();
        assertThat(persisted.getUltimoAcceso()).isBeforeOrEqualTo(LocalDateTime.now(java.time.Clock.systemUTC()));
        assertThat(LoginResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .containsExactly("login", "codper");
        assertThat(LoginResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("authenticated", "password", "passwd", "hash", "persona", "roles");
    }

    @Test
    void rejectsInvalidCredentialsWithoutChangingUltimoAcceso() {
        UsuarioResponse usuario = createUsuario("AUTH-LOGIN-2", "usuario.invalido", "clave-ficticia");

        assertThatThrownBy(() -> authService.login(new LoginRequest("usuario.invalido", "clave-incorrecta")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("usuario.no.existe", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);
        usuarioService.deactivate(usuario.login());
        assertThatThrownBy(() -> authService.login(new LoginRequest(usuario.login(), "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(usuarioRepository.findById(usuario.login()).orElseThrow().getUltimoAcceso()).isNull();
    }

    @Test
    void rejectsInactivePersonaAndIgnoresActiveOrInactiveRolesDuringLogin() {
        UsuarioResponse personaInactiveUsuario = createUsuario("AUTH-LOGIN-3", "persona.inactiva", "clave-ficticia");
        personaService.deactivate(personaInactiveUsuario.codper());
        assertThatThrownBy(() -> authService.login(new LoginRequest("persona.inactiva", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(usuarioRepository.findById("persona.inactiva").orElseThrow().getUltimoAcceso()).isNull();

        UsuarioResponse usuarioConRoles = createUsuario("AUTH-LOGIN-4", "usuario.roles", "clave-ficticia");
        RolResponse activo = rolService.create(new CreateRolRequest("AUTH ACTIVO", null));
        RolResponse inactivo = rolService.create(new CreateRolRequest("AUTH INACTIVO", null));
        rolUsuService.assign(usuarioConRoles.login(), activo.codr());
        rolUsuService.assign(usuarioConRoles.login(), inactivo.codr());
        rolService.deactivate(inactivo.codr());

        assertThat(authService.login(new LoginRequest("usuario.roles", "clave-ficticia")))
                .isEqualTo(new LoginResponse("usuario.roles", usuarioConRoles.codper()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5') AND success", Integer.class))
                .isEqualTo(5);
    }

    @Test
    void keepsLoginCaseSensitiveAndPasswordUntrimmed() {
        createUsuario("AUTH-LOGIN-5", "Usuario.Case", "clave-ficticia");

        assertThatThrownBy(() -> authService.login(new LoginRequest("usuario.case", "clave-ficticia")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("Usuario.Case", " clave-ficticia ")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private UsuarioResponse createUsuario(String ci, String login, String password) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona ficticia", null, null,
                "F", null, null, "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, password, null, persona.codper()));
    }
}
