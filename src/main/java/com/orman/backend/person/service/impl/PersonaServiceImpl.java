package com.orman.backend.person.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.mapper.PersonaMapper;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonaServiceImpl implements PersonaService {
    private final PersonaRepository personaRepository;
    private final PersonaMapper personaMapper;
    private final EntityManager entityManager;
    private final UsuarioRepository usuarioRepository;
    private final SessionService sessionService;

    @Transactional
    public PersonaResponse create(CreatePersonaRequest request) {
        if (personaRepository.existsByCi(request.ci().trim())) throw new ConflictException("El CI ya está registrado.");
        try {
            Persona persona = personaRepository.saveAndFlush(personaMapper.toEntity(request));
            entityManager.refresh(persona);
            return personaMapper.toResponse(persona);
        }
        catch (DataIntegrityViolationException exception) { throw new ConflictException("El CI ya está registrado."); }
    }
    @Transactional(readOnly = true) public PersonaResponse get(Integer codper) { return personaMapper.toResponse(find(codper)); }
    @Transactional(readOnly = true) public PageResponse<PersonaResponse> list(Pageable pageable) {
        Page<PersonaResponse> page = personaRepository.findAll(pageable).map(personaMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
    @Transactional public PersonaResponse update(Integer codper, UpdatePersonaRequest request) {
        Persona persona = find(codper);
        if (personaRepository.existsByCiAndCodperNot(request.ci().trim(), codper)) throw new ConflictException("El CI ya está registrado.");
        try {
            personaMapper.update(persona, request);
            Persona saved = personaRepository.saveAndFlush(persona);
            revokeIfInactive(saved);
            return personaMapper.toResponse(saved);
        }
        catch (DataIntegrityViolationException exception) { throw new ConflictException("El CI ya está registrado."); }
    }
    @Transactional public PersonaResponse deactivate(Integer codper) { return changeStatus(codper, Short.valueOf((short) 0)); }
    @Transactional public PersonaResponse activate(Integer codper) { return changeStatus(codper, Short.valueOf((short) 1)); }
    @Transactional public void delete(Integer codper) { personaRepository.delete(find(codper)); }
    private PersonaResponse changeStatus(Integer codper, Short estado) {
        Persona persona = find(codper);
        persona.setEstado(estado);
        Persona saved = personaRepository.save(persona);
        revokeIfInactive(saved);
        return personaMapper.toResponse(saved);
    }
    private void revokeIfInactive(Persona persona) {
        if (Short.valueOf((short) 0).equals(persona.getEstado())) {
            usuarioRepository.findByPersonaCodper(persona.getCodper())
                    .ifPresent(usuario -> sessionService.revokeAll(
                            usuario.getLogin(), RevocationReason.PERSON_DISABLED));
        }
    }
    private Persona find(Integer codper) { return personaRepository.findById(codper).orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada.")); }
}
