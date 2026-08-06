package com.orman.backend.menu.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.menu.dto.response.MeProResponse;
import com.orman.backend.menu.entity.MePro;
import com.orman.backend.menu.entity.MeProId;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.repository.MeProRepository;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.menu.service.MeProService;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.repository.ProcesoRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeProServiceImpl implements MeProService {
    private static final short ACTIVO = 1;
    private final MenuRepository menuRepository;
    private final ProcesoRepository procesoRepository;
    private final MeProRepository meProRepository;
    @Override @Transactional public MeProResponse assign(Integer codm, Integer codp) {
        Menu menu = findMenu(codm); Proceso proceso = findProceso(codp); assertActivo(menu.getEstado(), "Menú"); assertActivo(proceso.getEstado(), "Proceso");
        MeProId id = new MeProId(codm, codp); if (meProRepository.existsById(id)) throw duplicate();
        try { return toResponse(meProRepository.saveAndFlush(new MePro(menu, proceso))); } catch (DataIntegrityViolationException exception) { throw duplicate(); }
    }
    @Override @Transactional public void remove(Integer codm, Integer codp) {
        findMenu(codm); findProceso(codp); MeProId id = new MeProId(codm, codp);
        if (!meProRepository.existsById(id)) throw new ResourceNotFoundException("La asignación Menú–Proceso no existe.");
        meProRepository.deleteById(id); meProRepository.flush();
    }
    @Override @Transactional(readOnly = true) public List<MeProResponse> listByMenu(Integer codm) {
        findMenu(codm); return meProRepository.findByIdCodm(codm).stream().sorted(Comparator.comparing(v -> v.getProceso().getNombre())).map(this::toResponse).toList();
    }
    @Override @Transactional(readOnly = true) public List<MeProResponse> listByProceso(Integer codp) {
        findProceso(codp); return meProRepository.findByIdCodp(codp).stream().sorted(Comparator.comparing(v -> v.getMenu().getNombre())).map(this::toResponse).toList();
    }
    private Menu findMenu(Integer codm) { return menuRepository.findById(codm).orElseThrow(() -> new ResourceNotFoundException("Menú no encontrado.")); }
    private Proceso findProceso(Integer codp) { return procesoRepository.findById(codp).orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado.")); }
    private void assertActivo(Short estado, String recurso) { if (!Short.valueOf(ACTIVO).equals(estado)) throw new BusinessRuleException("No se puede asignar un " + recurso + " inactivo."); }
    private ConflictException duplicate() { return new ConflictException("El Proceso ya está asignado al Menú."); }
    private MeProResponse toResponse(MePro value) { return new MeProResponse(value.getMenu().getCodm(), value.getMenu().getNombre(), value.getMenu().getEstado(), value.getProceso().getCodp(), value.getProceso().getNombre(), value.getProceso().getEnlace(), value.getProceso().getEstado()); }
}
