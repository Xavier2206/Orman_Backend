package com.orman.backend.person.service.impl;

import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.authorization.service.AuthorizationService;
import com.orman.backend.authorization.service.OwnerProtectionService;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaActionsResponse;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.PersonaResumenResponse;
import com.orman.backend.person.dto.PersonaSearchCriteria;
import com.orman.backend.person.dto.PersonaUsuarioResponse;
import com.orman.backend.person.dto.PersonaUsuarioRow;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.mapper.PersonaMapper;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.repository.PersonaResumenProjection;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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
    private final OwnerProtectionService ownerProtectionService;
    private final AuthorizationService authorizationService;
    private final RolUsuRepository rolUsuRepository;

    @Override
    @Transactional
    public PersonaResponse create(CreatePersonaRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    public PersonaResponse create(CreatePersonaRequest request, Authentication authentication) {
        if (personaRepository.existsByCi(request.ci().trim())) {
            throw new ConflictException("El CI ya está registrado.");
        }
        try {
            Persona persona = personaRepository.saveAndFlush(personaMapper.toEntity(request));
            entityManager.refresh(persona);
            return toResponse(persona, authentication);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El CI ya está registrado.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse get(Integer codper) {
        return personaMapper.toResponse(find(codper));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse get(Integer codper, Authentication authentication) {
        return toResponses(List.of(find(codper)), authentication).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResumenResponse resumen() {
        PersonaResumenProjection resumen = personaRepository.findResumen();
        return new PersonaResumenResponse(resumen.getTotalPersonas(), resumen.getActivas(),
                resumen.getInactivas(), resumen.getConUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PersonaResponse> list(Pageable pageable) {
        Page<PersonaResponse> page = personaRepository.findAll(pageable).map(personaMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PersonaResponse> list(PersonaSearchCriteria criteria, Pageable pageable,
                                               Authentication authentication) {
        Page<Persona> page = personaRepository.search(criteria.q(), criteria.tipoPersona(), criteria.estado(), pageable);
        return new PageResponse<>(toResponses(page.getContent(), authentication), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional
    public PersonaResponse update(Integer codper, UpdatePersonaRequest request) {
        return update(codper, request, null);
    }

    @Override
    @Transactional
    public PersonaResponse update(Integer codper, UpdatePersonaRequest request, Authentication authentication) {
        if ("0".equals(request.estado())) {
            ownerProtectionService.assertCanDeactivatePerson(codper);
        }
        Persona persona = find(codper);
        if (personaRepository.existsByCiAndCodperNot(request.ci().trim(), codper)) {
            throw new ConflictException("El CI ya está registrado.");
        }
        try {
            personaMapper.update(persona, request);
            Persona saved = personaRepository.saveAndFlush(persona);
            revokeIfInactive(saved);
            return toResponse(saved, authentication);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El CI ya está registrado.");
        }
    }

    @Override
    @Transactional
    public PersonaResponse deactivate(Integer codper) {
        return deactivate(codper, null);
    }

    @Override
    @Transactional
    public PersonaResponse deactivate(Integer codper, Authentication authentication) {
        ownerProtectionService.assertCanDeactivatePerson(codper);
        return changeStatus(codper, (short) 0, authentication);
    }

    @Override
    @Transactional
    public PersonaResponse activate(Integer codper) {
        return activate(codper, null);
    }

    @Override
    @Transactional
    public PersonaResponse activate(Integer codper, Authentication authentication) {
        return changeStatus(codper, (short) 1, authentication);
    }

    @Override
    @Transactional
    public void delete(Integer codper) {
        ownerProtectionService.assertCanDeactivatePerson(codper);
        personaRepository.delete(find(codper));
    }

    private PersonaResponse changeStatus(Integer codper, short estado, Authentication authentication) {
        Persona persona = find(codper);
        persona.setEstado(estado);
        Persona saved = personaRepository.save(persona);
        revokeIfInactive(saved);
        return toResponse(saved, authentication);
    }

    private PersonaResponse toResponse(Persona persona, Authentication authentication) {
        return toResponses(List.of(persona), authentication).getFirst();
    }

    private List<PersonaResponse> toResponses(List<Persona> personas, Authentication authentication) {
        if (personas.isEmpty()) {
            return List.of();
        }
        List<Integer> codpers = personas.stream().map(Persona::getCodper).toList();
        Map<Integer, PersonaUsuarioRow> usuarios = usuarioRepository.findSummariesByPersonaCodperIn(codpers).stream()
                .collect(Collectors.toMap(PersonaUsuarioRow::codper, Function.identity()));
        Set<Integer> activeOwners = Set.copyOf(rolUsuRepository.findActiveOwnerPersonCodpers(codpers));
        boolean lastOwnerInPage = !activeOwners.isEmpty() && rolUsuRepository.countActiveOwners() <= 1;
        boolean owner = authorizationService.isOwner(authentication);

        return personas.stream().map(persona -> {
            PersonaUsuarioRow usuario = usuarios.get(persona.getCodper());
            PersonaUsuarioResponse usuarioResponse = usuario == null ? null
                    : new PersonaUsuarioResponse(usuario.login(), usuario.estado());
            boolean canManage = owner;
            boolean active = Short.valueOf((short) 1).equals(persona.getEstado());
            boolean lastOwner = activeOwners.contains(persona.getCodper()) && lastOwnerInPage;
            boolean canChangePassword = usuario != null && authorizationService.isSelfOrOwner(authentication,
                    usuario.login());
            PersonaActionsResponse actions = new PersonaActionsResponse(canManage, canManage && active && !lastOwner,
                    canManage && !active, canManage && !lastOwner, canManage && usuario == null, canChangePassword);
            return personaMapper.toResponse(persona, usuarioResponse, actions);
        }).toList();
    }

    private void revokeIfInactive(Persona persona) {
        if (Short.valueOf((short) 0).equals(persona.getEstado())) {
            usuarioRepository.findByPersonaCodper(persona.getCodper())
                    .ifPresent(usuario -> sessionService.revokeAll(usuario.getLogin(), RevocationReason.PERSON_DISABLED));
        }
    }

    private Persona find(Integer codper) {
        return personaRepository.findById(codper)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada."));
    }
}
