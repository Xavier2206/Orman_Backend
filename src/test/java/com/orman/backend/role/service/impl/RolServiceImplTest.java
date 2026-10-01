package com.orman.backend.role.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.mapper.RolMapper;
import com.orman.backend.role.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    @InjectMocks private RolServiceImpl service;

    @Test
    void getsFixedRoleAndRejectsUnknownId() {
        Rol rol = rol(1, "PROPIETARIO", (short) 1);
        RolResponse response = new RolResponse(1, "PROPIETARIO", (short) 1);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(rolMapper.toResponse(rol)).thenReturn(response);
        when(rolRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(service.get(1)).isEqualTo(response);
        assertThatThrownBy(() -> service.get(99)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchesInDatabaseWithNormalizedQueryAndBuildsGlobalResumen() {
        Rol rol = rol(1, "INQUILINO", (short) 1);
        RolResponse response = new RolResponse(1, "INQUILINO", (short) 1);
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("nombre").ascending());
        when(rolRepository.search("inq", (short) 1, pageable))
                .thenReturn(new PageImpl<>(List.of(rol), pageable, 1));
        when(rolMapper.toResponse(rol)).thenReturn(response);
        when(rolRepository.count()).thenReturn(5L);
        when(rolRepository.countByEstado((short) 1)).thenReturn(3L);
        when(rolRepository.countByEstado((short) 0)).thenReturn(2L);

        assertThat(service.list(" inq ", (short) 1, pageable).content()).containsExactly(response);
        assertThat(service.resumen()).isEqualTo(new RolResumenResponse(5, 3, 2));
    }

    @Test
    void supportsSummariesWithOnlyOneEstado() {
        when(rolRepository.count()).thenReturn(2L);
        when(rolRepository.countByEstado((short) 1)).thenReturn(2L);
        when(rolRepository.countByEstado((short) 0)).thenReturn(0L);
        assertThat(service.resumen()).isEqualTo(new RolResumenResponse(2, 2, 0));

        when(rolRepository.countByEstado((short) 1)).thenReturn(0L);
        when(rolRepository.countByEstado((short) 0)).thenReturn(2L);
        assertThat(service.resumen()).isEqualTo(new RolResumenResponse(2, 0, 2));
    }

    private Rol rol(Integer codr, String nombre, short estado) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "codr", codr);
        rol.setNombre(nombre);
        rol.setEstado(estado);
        return rol;
    }
}
