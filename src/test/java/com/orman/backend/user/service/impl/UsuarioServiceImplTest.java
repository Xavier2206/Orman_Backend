package com.orman.backend.user.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.mapper.UsuarioMapper;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;
    @Mock private SessionService sessionService;
    @InjectMocks private UsuarioServiceImpl service;

    @Test
    void createsUsuarioWithBcryptHash() {
        CreateUsuarioRequest request = request(" usuario.demo ", 7);
        Persona persona = persona(7);
        Usuario usuario = usuario("usuario.demo", persona, (short) 1);
        UsuarioResponse response = response("usuario.demo", (short) 1, 7);
        when(usuarioRepository.existsById("usuario.demo")).thenReturn(false);
        when(personaRepository.findById(7)).thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByPersonaCodper(7)).thenReturn(false);
        when(usuarioMapper.toEntity(request, persona)).thenReturn(usuario);
        when(passwordEncoder.encode("contraseña-ficticia")).thenReturn("bcrypt-hash");
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        assertThat(service.create(request)).isEqualTo(response);
        assertThat(usuario.getPasswd()).isEqualTo("bcrypt-hash");
        verify(passwordEncoder).encode("contraseña-ficticia");
        verify(entityManager).refresh(usuario);
    }

    @Test
    void rejectsDuplicateLoginAndPersonaAlreadyAssigned() {
        when(usuarioRepository.existsById("usuario.demo")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("usuario.demo", 7))).isInstanceOf(ConflictException.class);
        verify(personaRepository, never()).findById(any());

        when(usuarioRepository.existsById("otro.usuario")).thenReturn(false);
        when(personaRepository.findById(7)).thenReturn(Optional.of(persona(7)));
        when(usuarioRepository.existsByPersonaCodper(7)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("otro.usuario", 7))).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsMissingPersonaAndMissingUsuario() {
        when(usuarioRepository.existsById("usuario.demo")).thenReturn(false);
        when(personaRepository.findById(7)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request("usuario.demo", 7))).isInstanceOf(ResourceNotFoundException.class);

        when(usuarioRepository.findById("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("inexistente")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getsAndListsUsuariosWithoutExposingPassword() {
        Usuario usuario = usuario("usuario.demo", persona(7), (short) 1);
        UsuarioResponse response = response("usuario.demo", (short) 1, 7);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);
        assertThat(service.get("usuario.demo")).isEqualTo(response);

        PageRequest pageable = PageRequest.of(0, 20);
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuario), pageable, 1));
        assertThat(service.list(pageable).content()).containsExactly(response);
        assertThat(UsuarioResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("passwd", "password", "hash");
    }

    @Test
    void updatesAndChangesStatusIdempotently() {
        Usuario usuario = usuario("usuario.demo", persona(7), (short) 1);
        UsuarioResponse inactive = response("usuario.demo", (short) 0, 7);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Usuario updated = invocation.getArgument(0);
            UpdateUsuarioRequest update = invocation.getArgument(1);
            updated.setEstado(update.estado());
            return null;
        }).when(usuarioMapper).update(any(Usuario.class), any(UpdateUsuarioRequest.class));
        when(usuarioMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario mapped = invocation.getArgument(0);
            return response(mapped.getLogin(), mapped.getEstado(), mapped.getPersona().getCodper());
        });

        assertThat(service.update("usuario.demo", new UpdateUsuarioRequest((short) 0))).isEqualTo(inactive);
        verify(usuarioMapper).update(usuario, new UpdateUsuarioRequest((short) 0));
        assertThat(service.deactivate("usuario.demo").estado()).isZero();
        assertThat(service.deactivate("usuario.demo").estado()).isZero();
        assertThat(service.activate("usuario.demo").estado()).isEqualTo((short) 1);
    }

    @Test
    void changesPasswordAndRejectsMissingUsuario() {
        Usuario usuario = usuario("usuario.demo", persona(7), (short) 1);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva-clave")).thenReturn("nuevo-bcrypt");

        service.changePassword("usuario.demo", new ChangePasswordRequest("nueva-clave"));

        assertThat(usuario.getPasswd()).isEqualTo("nuevo-bcrypt");
        verify(usuarioRepository).saveAndFlush(usuario);
        verify(sessionService).revokeAll("usuario.demo",
                com.orman.backend.auth.model.RevocationReason.PASSWORD_CHANGED);
        when(usuarioRepository.findById("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changePassword("inexistente", new ChangePasswordRequest("nueva-clave")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateUsuarioRequest request(String login, Integer codper) {
        return new CreateUsuarioRequest(login, "contraseña-ficticia", null, codper);
    }

    private Persona persona(Integer codper) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        return persona;
    }

    private Usuario usuario(String login, Persona persona, short estado) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd("bcrypt-existente");
        usuario.setPersona(persona);
        usuario.setEstado(estado);
        usuario.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 0, 0));
        return usuario;
    }

    private UsuarioResponse response(String login, short estado, Integer codper) {
        return new UsuarioResponse(login, estado, codper, LocalDateTime.of(2026, 1, 1, 0, 0), null);
    }
}
