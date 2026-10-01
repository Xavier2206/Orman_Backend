package com.orman.backend.role.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.role.dto.response.RolMeResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolMe;
import com.orman.backend.role.entity.RolMeId;
import com.orman.backend.role.repository.RolMeRepository;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.service.RolMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolMeServiceImpl implements RolMeService {
    private static final short ACTIVO = 1;
    private final RolRepository rolRepository;
    private final MenuRepository menuRepository;
    private final RolMeRepository rolMeRepository;

    @Override @Transactional public RolMeResponse assign(Integer codr, Integer codm) {
        Rol rol = findRol(codr); Menu menu = findMenu(codm);
        if (!"PROPIETARIO".equals(rol.getNombre()) && !"INQUILINO".equals(rol.getNombre())) throw new BusinessRuleException("El Rol no pertenece al catálogo permitido."); assertActivo(rol.getEstado(), "Rol"); assertActivo(menu.getEstado(), "Menú");
        RolMeId id = new RolMeId(codr, codm);
        if (rolMeRepository.existsById(id)) throw duplicate();
        try { return toResponse(rolMeRepository.saveAndFlush(new RolMe(rol, menu))); }
        catch (DataIntegrityViolationException exception) { throw duplicate(); }
    }
    @Override @Transactional public void remove(Integer codr, Integer codm) {
        findRol(codr); findMenu(codm); RolMeId id = new RolMeId(codr, codm);
        if (!rolMeRepository.existsById(id)) throw new ResourceNotFoundException("La asignación Rol–Menú no existe.");
        rolMeRepository.deleteById(id); rolMeRepository.flush();
    }
    @Override @Transactional(readOnly = true) public List<RolMeResponse> listByRol(Integer codr) {
        findRol(codr); return rolMeRepository.findByIdCodr(codr).stream().sorted(Comparator.comparing(r -> r.getMenu().getNombre())).map(this::toResponse).toList();
    }
    @Override @Transactional(readOnly = true) public List<RolMeResponse> listByMenu(Integer codm) {
        findMenu(codm); return rolMeRepository.findByIdCodm(codm).stream().sorted(Comparator.comparing(r -> r.getRol().getNombre())).map(this::toResponse).toList();
    }
    private Rol findRol(Integer codr) { return rolRepository.findById(codr).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado.")); }
    private Menu findMenu(Integer codm) { return menuRepository.findById(codm).orElseThrow(() -> new ResourceNotFoundException("Menú no encontrado.")); }
    private void assertActivo(Short estado, String recurso) { if (!Short.valueOf(ACTIVO).equals(estado)) throw new BusinessRuleException("No se puede asignar un " + recurso + " inactivo."); }
    private ConflictException duplicate() { return new ConflictException("El Menú ya está asignado al Rol."); }
    private RolMeResponse toResponse(RolMe value) { return new RolMeResponse(value.getRol().getCodr(), value.getRol().getNombre(), value.getRol().getEstado(), value.getMenu().getCodm(), value.getMenu().getNombre(), value.getMenu().getEstado()); }
}
