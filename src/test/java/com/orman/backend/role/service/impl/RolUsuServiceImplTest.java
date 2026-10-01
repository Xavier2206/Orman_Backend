package com.orman.backend.role.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.authorization.service.OwnerProtectionService;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.response.RolUsuResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolUsu;
import com.orman.backend.role.entity.RolUsuId;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolUsuServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private RolUsuRepository rolUsuRepository;
    @Mock private EntityManager entityManager;
    @Mock private OwnerProtectionService ownerProtectionService;
    @InjectMocks private RolUsuServiceImpl service;

    @Test
    void assignsActiveRolAndReturnsOnlyAssignmentFields() {
        Usuario usuario = usuario("usuario.demo");
        Rol rol = rol(3, "INQUILINO", (short) 1);
        RolUsu assignment = assignment(usuario, rol);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(3)).thenReturn(Optional.of(rol));
        when(rolUsuRepository.existsById(new RolUsuId("usuario.demo", 3))).thenReturn(false);
        when(rolUsuRepository.saveAndFlush(any(RolUsu.class))).thenReturn(assignment);

        RolUsuResponse response = service.assign("usuario.demo", 3);

        assertThat(response.login()).isEqualTo("usuario.demo");
        assertThat(response.codr()).isEqualTo(3);
        assertThat(response.nombreRol()).isEqualTo("INQUILINO");
        assertThat(response.fechaAsignacion()).isNotNull();
        verify(entityManager).refresh(assignment);
    }

    @Test
    void rejectsInactiveOrDuplicateAssignmentAndMissingResources() {
        Usuario usuario = usuario("usuario.demo");
        Rol inactive = rol(3, "INQUILINO", (short) 0);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(3)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.assign("usuario.demo", 3)).isInstanceOf(BusinessRuleException.class);

        Rol active = rol(3, "INQUILINO", (short) 1);
        when(rolRepository.findById(3)).thenReturn(Optional.of(active));
        when(rolUsuRepository.existsById(new RolUsuId("usuario.demo", 3))).thenReturn(true);
        assertThatThrownBy(() -> service.assign("usuario.demo", 3)).isInstanceOf(ConflictException.class);

        when(usuarioRepository.findById("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listByUsuario("inexistente")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsRoleOutsideFixedCatalogue() {
        Usuario usuario = usuario("usuario.demo");
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(3)).thenReturn(Optional.of(rol(3, "ELECTRICISTA", (short) 1)));
        assertThatThrownBy(() -> service.assign("usuario.demo", 3))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removesAndListsAssignments() {
        Usuario usuario = usuario("usuario.demo");
        Rol rol = rol(3, "INQUILINO", (short) 1);
        RolUsu assignment = assignment(usuario, rol);
        RolUsuId id = new RolUsuId("usuario.demo", 3);
        when(usuarioRepository.findById("usuario.demo")).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(3)).thenReturn(Optional.of(rol));
        when(rolUsuRepository.existsById(id)).thenReturn(true);

        service.remove("usuario.demo", 3);
        verify(rolUsuRepository).deleteById(id);
        verify(rolUsuRepository).flush();

        when(rolUsuRepository.findByIdLoginOrderByFechaAsignacionAsc("usuario.demo"))
                .thenReturn(List.of(assignment));
        assertThat(service.listByUsuario("usuario.demo")).extracting(RolUsuResponse::nombreRol)
                .containsExactly("INQUILINO");
    }

    private Usuario usuario(String login) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        return usuario;
    }

    private Rol rol(Integer codr, String nombre, short estado) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "codr", codr);
        rol.setNombre(nombre);
        rol.setEstado(estado);
        return rol;
    }

    private RolUsu assignment(Usuario usuario, Rol rol) {
        RolUsu assignment = new RolUsu(usuario, rol);
        ReflectionTestUtils.setField(assignment, "fechaAsignacion", LocalDateTime.of(2026, 8, 2, 12, 0));
        return assignment;
    }
}
