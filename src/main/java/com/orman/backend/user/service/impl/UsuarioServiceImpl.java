package com.orman.backend.user.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.authorization.service.OwnerProtectionService;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.mapper.UsuarioMapper;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final SessionService sessionService;
    private final OwnerProtectionService ownerProtectionService;

    @Override
    @Transactional
    public UsuarioResponse create(CreateUsuarioRequest request) {
        String login = request.login().trim();
        if (usuarioRepository.existsById(login)) {
            throw duplicateLogin();
        }
        Persona persona = findPersona(request.codper());
        if (usuarioRepository.existsByPersonaCodper(persona.getCodper())) {
            throw personaAlreadyAssigned();
        }

        Usuario usuario = usuarioMapper.toEntity(request, persona);
        usuario.setPasswd(passwordEncoder.encode(request.password()));
        try {
            Usuario saved = usuarioRepository.saveAndFlush(usuario);
            entityManager.refresh(saved);
            return usuarioMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translateConstraint(exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse get(String login) {
        return usuarioMapper.toResponse(findUsuario(login));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> list(Pageable pageable) {
        return pageResponse(usuarioRepository.findAll(pageable).map(usuarioMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listCommon(Pageable pageable) {
        return pageResponse(usuarioRepository.findAllCommon(pageable).map(usuarioMapper::toResponse));
    }

    private PageResponse<UsuarioResponse> pageResponse(Page<UsuarioResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional
    public UsuarioResponse update(String login, UpdateUsuarioRequest request) {
        if (Short.valueOf(INACTIVO).equals(request.estado())) {
            ownerProtectionService.assertCanDeactivateUser(login);
        }
        Usuario usuario = findUsuario(login);
        usuarioMapper.update(usuario, request);
        Usuario saved = usuarioRepository.saveAndFlush(usuario);
        if (Short.valueOf(INACTIVO).equals(saved.getEstado())) {
            sessionService.revokeAll(login, RevocationReason.USER_DISABLED);
        }
        return usuarioMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UsuarioResponse deactivate(String login) {
        ownerProtectionService.assertCanDeactivateUser(login);
        return changeStatus(login, INACTIVO);
    }

    @Override
    @Transactional
    public UsuarioResponse activate(String login) {
        return changeStatus(login, ACTIVO);
    }

    @Override
    @Transactional
    public void changePassword(String login, ChangePasswordRequest request) {
        Usuario usuario = findUsuario(login);
        usuario.setPasswd(passwordEncoder.encode(request.newPassword()));
        usuarioRepository.saveAndFlush(usuario);
        sessionService.revokeAll(login, RevocationReason.PASSWORD_CHANGED);
    }

    private UsuarioResponse changeStatus(String login, short estado) {
        Usuario usuario = findUsuario(login);
        usuario.setEstado(estado);
        Usuario saved = usuarioRepository.save(usuario);
        if (estado == INACTIVO) {
            sessionService.revokeAll(login, RevocationReason.USER_DISABLED);
        }
        return usuarioMapper.toResponse(saved);
    }

    private Usuario findUsuario(String login) {
        return usuarioRepository.findById(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private Persona findPersona(Integer codper) {
        return personaRepository.findById(codper)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada."));
    }

    private ConflictException translateConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                if ("pk_usuarios".equals(constraintViolation.getConstraintName())) {
                    return duplicateLogin();
                }
                if ("uk_usuarios_codper".equals(constraintViolation.getConstraintName())) {
                    return personaAlreadyAssigned();
                }
            }
            cause = cause.getCause();
        }
        throw exception;
    }

    private ConflictException duplicateLogin() {
        return new ConflictException("El login ya está registrado.");
    }

    private ConflictException personaAlreadyAssigned() {
        return new ConflictException("La Persona ya tiene un Usuario.");
    }
}
