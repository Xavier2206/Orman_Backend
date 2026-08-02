package com.orman.backend.user.service.impl;

import com.orman.backend.common.dto.PageResponse;
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
        Page<UsuarioResponse> page = usuarioRepository.findAll(pageable).map(usuarioMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional
    public UsuarioResponse update(String login, UpdateUsuarioRequest request) {
        Usuario usuario = findUsuario(login);
        usuarioMapper.update(usuario, request);
        return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse deactivate(String login) {
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
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse changeStatus(String login, short estado) {
        Usuario usuario = findUsuario(login);
        usuario.setEstado(estado);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
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
