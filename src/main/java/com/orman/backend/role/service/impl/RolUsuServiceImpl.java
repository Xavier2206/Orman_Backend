package com.orman.backend.role.service.impl;

import com.orman.backend.config.OrmanTimeConfig;
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
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolUsuServiceImpl implements RolUsuService {

    private static final short ACTIVO = 1;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final RolUsuRepository rolUsuRepository;
    private final EntityManager entityManager;
    private final OwnerProtectionService ownerProtectionService;

    @Override
    @Transactional
    public RolUsuResponse assign(String login, Integer codr) {
        Usuario usuario = findUsuario(login);
        Rol rol = findRol(codr);
        if (!"PROPIETARIO".equals(rol.getNombre()) && !"INQUILINO".equals(rol.getNombre())) {
            throw new BusinessRuleException("El Rol no pertenece al catálogo permitido.");
        }
        if (!isActivo(rol.getEstado())) {
            throw new BusinessRuleException("No se puede asignar un Rol inactivo.");
        }

        RolUsuId id = new RolUsuId(usuario.getLogin(), rol.getCodr());
        if (rolUsuRepository.existsById(id)) {
            throw duplicateAssignment();
        }

        try {
            RolUsu saved = rolUsuRepository.saveAndFlush(new RolUsu(usuario, rol));
            entityManager.refresh(saved);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw translateConstraint(exception);
        }
    }

    @Override
    @Transactional
    public void remove(String login, Integer codr) {
        ownerProtectionService.assertCanRemoveAssignment(login, codr);
        Usuario usuario = findUsuario(login);
        Rol rol = findRol(codr);
        RolUsuId id = new RolUsuId(usuario.getLogin(), rol.getCodr());
        if (!rolUsuRepository.existsById(id)) {
            throw new ResourceNotFoundException("La asignaciÃ³n de Rol no existe.");
        }
        rolUsuRepository.deleteById(id);
        rolUsuRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolUsuResponse> listByUsuario(String login) {
        Usuario usuario = findUsuario(login);
        return rolUsuRepository.findByIdLoginOrderByFechaAsignacionAsc(usuario.getLogin()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolUsuResponse> listByRol(Integer codr) {
        Rol rol = findRol(codr);
        return rolUsuRepository.findByIdCodrOrderByFechaAsignacionAsc(rol.getCodr()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Usuario findUsuario(String login) {
        return usuarioRepository.findById(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private Rol findRol(Integer codr) {
        return rolRepository.findById(codr)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
    }

    private boolean isActivo(Short estado) {
        return Short.valueOf(ACTIVO).equals(estado);
    }

    private RolUsuResponse toResponse(RolUsu rolUsu) {
        return new RolUsuResponse(rolUsu.getUsuario().getLogin(), rolUsu.getRol().getCodr(),
                rolUsu.getRol().getNombre(), OrmanTimeConfig.ormanLocalToOffset(rolUsu.getFechaAsignacion()));
    }

    private ConflictException duplicateAssignment() {
        return new ConflictException("El Rol ya estÃ¡ asignado al Usuario.");
    }

    private ConflictException translateConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && "pk_rolusu".equals(constraintViolation.getConstraintName())) {
                return duplicateAssignment();
            }
            cause = cause.getCause();
        }
        throw exception;
    }
}
