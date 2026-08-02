package com.orman.backend.role.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.mapper.RolMapper;
import com.orman.backend.role.repository.RolRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceImplTest {

    @Mock private RolRepository rolRepository;
    @Mock private RolMapper rolMapper;
    @Mock private EntityManager entityManager;
    @InjectMocks private RolServiceImpl service;

    @Test
    void createsRolWithNormalizedNameAndDatabaseDefault() {
        CreateRolRequest request = new CreateRolRequest(" administrador ", null);
        Rol rol = rol(1, "ADMINISTRADOR", (short) 1);
        RolResponse response = new RolResponse(1, "ADMINISTRADOR", (short) 1);
        when(rolMapper.normalizeNombre(" administrador ")).thenReturn("ADMINISTRADOR");
        when(rolRepository.existsByNombre("ADMINISTRADOR")).thenReturn(false);
        when(rolMapper.toEntity(request)).thenReturn(rol);
        when(rolRepository.saveAndFlush(rol)).thenReturn(rol);
        when(rolMapper.toResponse(rol)).thenReturn(response);

        assertThat(service.create(request)).isEqualTo(response);
        verify(entityManager).refresh(rol);
    }

    @Test
    void rejectsDuplicateNombreAndMissingRol() {
        when(rolMapper.normalizeNombre("ADMINISTRADOR")).thenReturn("ADMINISTRADOR");
        when(rolRepository.existsByNombre("ADMINISTRADOR")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateRolRequest("ADMINISTRADOR", null)))
                .isInstanceOf(ConflictException.class);
        verify(rolMapper, never()).toEntity(any());

        when(rolRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesListsAndChangesStateIdempotently() {
        Rol rol = rol(1, "ADMINISTRADOR", (short) 1);
        RolResponse active = new RolResponse(1, "ADMINISTRADOR", (short) 1);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(rolMapper.toResponse(rol)).thenReturn(active);
        when(rolRepository.saveAndFlush(rol)).thenReturn(rol);
        when(rolRepository.save(rol)).thenReturn(rol);
        when(rolMapper.normalizeNombre("SUPERVISOR")).thenReturn("SUPERVISOR");
        when(rolRepository.existsByNombreAndCodrNot("SUPERVISOR", 1)).thenReturn(false);

        assertThat(service.update(1, new UpdateRolRequest("SUPERVISOR"))).isEqualTo(active);
        verify(rolMapper).update(rol, new UpdateRolRequest("SUPERVISOR"));
        assertThat(service.deactivate(1)).isEqualTo(active);
        assertThat(rol.getEstado()).isZero();
        assertThat(service.activate(1)).isEqualTo(active);
        assertThat(rol.getEstado()).isEqualTo((short) 1);

        when(rolRepository.findAllByOrderByNombreAsc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(rol), PageRequest.of(0, 20), 1));
        assertThat(service.list(PageRequest.of(0, 20)).content()).containsExactly(active);
    }

    private Rol rol(Integer codr, String nombre, short estado) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "codr", codr);
        rol.setNombre(nombre);
        rol.setEstado(estado);
        return rol;
    }
}
