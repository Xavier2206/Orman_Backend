package com.orman.backend.person.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.mapper.PersonaMapper;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaServiceImplTest {

    @Mock private PersonaRepository personaRepository;
    @Mock private PersonaMapper personaMapper;
    @Mock private EntityManager entityManager;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SessionService sessionService;
    @InjectMocks private PersonaServiceImpl service;

    @Test
    void createsPersonaUsingMapperAndRepository() {
        CreatePersonaRequest request = createRequest(" CI-001 ");
        Persona persona = persona(1, (short) 1);
        PersonaResponse response = response(1, (short) 1);
        when(personaRepository.existsByCi("CI-001")).thenReturn(false);
        when(personaMapper.toEntity(request)).thenReturn(persona);
        when(personaRepository.saveAndFlush(persona)).thenReturn(persona);
        when(personaMapper.toResponse(persona)).thenReturn(response);

        assertThat(service.create(request)).isEqualTo(response);
        verify(personaRepository).existsByCi("CI-001");
        verify(personaMapper).toEntity(request);
        verify(personaRepository).saveAndFlush(persona);
        verify(entityManager).refresh(persona);
        verify(personaMapper).toResponse(persona);
    }

    @Test
    void rejectsDuplicateCiOnCreate() {
        CreatePersonaRequest request = createRequest("CI-001");
        when(personaRepository.existsByCi("CI-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);
        verify(personaRepository).existsByCi("CI-001");
        verify(personaMapper, never()).toEntity(any());
    }

    @Test
    void getsExistingPersonaAndRejectsMissingOne() {
        Persona persona = persona(2, (short) 1);
        PersonaResponse response = response(2, (short) 1);
        when(personaRepository.findById(2)).thenReturn(Optional.of(persona));
        when(personaMapper.toResponse(persona)).thenReturn(response);

        assertThat(service.get(2)).isEqualTo(response);
        when(personaRepository.findById(3)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(3)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsMappedPage() {
        Persona persona = persona(4, (short) 1);
        PersonaResponse response = response(4, (short) 1);
        PageRequest pageable = PageRequest.of(1, 2);
        when(personaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(persona), pageable, 3));
        when(personaMapper.toResponse(persona)).thenReturn(response);

        var page = service.list(pageable);

        assertThat(page.content()).containsExactly(response);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void updatesPersonaAndUsesMapperAndRepository() {
        Persona persona = persona(5, (short) 1);
        UpdatePersonaRequest request = updateRequest(" CI-005 ");
        PersonaResponse response = response(5, (short) 1);
        when(personaRepository.findById(5)).thenReturn(Optional.of(persona));
        when(personaRepository.existsByCiAndCodperNot("CI-005", 5)).thenReturn(false);
        when(personaRepository.saveAndFlush(persona)).thenReturn(persona);
        when(personaMapper.toResponse(persona)).thenReturn(response);

        assertThat(service.update(5, request)).isEqualTo(response);
        verify(personaMapper).update(persona, request);
        verify(personaRepository).saveAndFlush(persona);
    }

    @Test
    void rejectsDuplicateCiOnUpdateAndMissingPersona() {
        Persona persona = persona(6, (short) 1);
        UpdatePersonaRequest request = updateRequest("CI-006");
        when(personaRepository.findById(6)).thenReturn(Optional.of(persona));
        when(personaRepository.existsByCiAndCodperNot("CI-006", 6)).thenReturn(true);

        assertThatThrownBy(() -> service.update(6, request)).isInstanceOf(ConflictException.class);
        verify(personaMapper, never()).update(any(), any());
        when(personaRepository.findById(7)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(7, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesPhysicallyAndRejectsMissingPersona() {
        Persona persona = persona(8, (short) 1);
        when(personaRepository.findById(8)).thenReturn(Optional.of(persona));

        service.delete(8);
        verify(personaRepository).delete(persona);
        when(personaRepository.findById(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(9)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatesPersonaAndIsIdempotent() {
        Persona active = persona(10, (short) 1);
        Persona inactive = persona(11, (short) 0);
        when(personaRepository.findById(10)).thenReturn(Optional.of(active));
        when(personaRepository.findById(11)).thenReturn(Optional.of(inactive));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personaMapper.toResponse(active)).thenReturn(response(10, (short) 0));
        when(personaMapper.toResponse(inactive)).thenReturn(response(11, (short) 0));

        assertThat(service.deactivate(10).estado()).isZero();
        assertThat(active.getEstado()).isZero();
        assertThat(service.deactivate(11).estado()).isZero();
        assertThat(inactive.getEstado()).isZero();
        verify(personaRepository).save(active);
        verify(personaRepository).save(inactive);
    }

    @Test
    void rejectsMissingPersonaOnDeactivate() {
        when(personaRepository.findById(12)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate(12)).isInstanceOf(ResourceNotFoundException.class);
        verify(personaRepository, never()).save(any());
    }

    @Test
    void activatesPersonaAndIsIdempotent() {
        Persona inactive = persona(13, (short) 0);
        Persona active = persona(14, (short) 1);
        when(personaRepository.findById(13)).thenReturn(Optional.of(inactive));
        when(personaRepository.findById(14)).thenReturn(Optional.of(active));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personaMapper.toResponse(inactive)).thenReturn(response(13, (short) 1));
        when(personaMapper.toResponse(active)).thenReturn(response(14, (short) 1));

        assertThat(service.activate(13).estado()).isEqualTo((short) 1);
        assertThat(inactive.getEstado()).isEqualTo((short) 1);
        assertThat(service.activate(14).estado()).isEqualTo((short) 1);
        assertThat(active.getEstado()).isEqualTo((short) 1);
    }

    @Test
    void rejectsMissingPersonaOnActivate() {
        when(personaRepository.findById(15)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.activate(15)).isInstanceOf(ResourceNotFoundException.class);
    }

    private CreatePersonaRequest createRequest(String ci) {
        return new CreatePersonaRequest(ci, "Nombre", null, null, "F", "1", null, "70000000", "A", null);
    }

    private UpdatePersonaRequest updateRequest(String ci) {
        return new UpdatePersonaRequest(ci, "Nombre", null, null, "F", "1", null, "70000000", "A", null);
    }

    private Persona persona(Integer codper, short estado) {
        Persona persona = new Persona();
        org.springframework.test.util.ReflectionTestUtils.setField(persona, "codper", codper);
        persona.setEstado(estado);
        return persona;
    }

    private PersonaResponse response(Integer codper, short estado) {
        return new PersonaResponse(codper, "CI-" + codper, "Nombre", null, null, 'F', estado, null,
                "70000000", 'A', null, LocalDateTime.of(2026, 1, 1, 0, 0));
    }
}
