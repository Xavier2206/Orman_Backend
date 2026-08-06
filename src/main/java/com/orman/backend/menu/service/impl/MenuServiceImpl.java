package com.orman.backend.menu.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.request.UpdateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResponse;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.mapper.MenuMapper;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.menu.service.MenuService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;
    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final EntityManager entityManager;

    @Override @Transactional
    public MenuResponse create(CreateMenuRequest request) {
        String nombre = menuMapper.normalizeNombre(request.nombre());
        if (menuRepository.existsByNombre(nombre)) throw duplicateNombre();
        try {
            Menu saved = menuRepository.saveAndFlush(menuMapper.toEntity(request));
            entityManager.refresh(saved);
            return menuMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) { throw duplicateNombre(); }
    }
    @Override @Transactional(readOnly = true)
    public MenuResponse get(Integer codm) { return menuMapper.toResponse(findMenu(codm)); }
    @Override @Transactional(readOnly = true)
    public PageResponse<MenuResponse> list(Pageable pageable) {
        Page<MenuResponse> page = menuRepository.findAllByOrderByNombreAsc(pageable).map(menuMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
    @Override @Transactional
    public MenuResponse update(Integer codm, UpdateMenuRequest request) {
        Menu menu = findMenu(codm);
        String nombre = menuMapper.normalizeNombre(request.nombre());
        if (menuRepository.existsByNombreAndCodmNot(nombre, codm)) throw duplicateNombre();
        try { menuMapper.update(menu, request); return menuMapper.toResponse(menuRepository.saveAndFlush(menu)); }
        catch (DataIntegrityViolationException exception) { throw duplicateNombre(); }
    }
    @Override @Transactional public MenuResponse activate(Integer codm) { return changeStatus(codm, ACTIVO); }
    @Override @Transactional public MenuResponse deactivate(Integer codm) { return changeStatus(codm, INACTIVO); }
    private MenuResponse changeStatus(Integer codm, short estado) { Menu menu = findMenu(codm); menu.setEstado(estado); return menuMapper.toResponse(menuRepository.save(menu)); }
    private Menu findMenu(Integer codm) { return menuRepository.findById(codm).orElseThrow(() -> new ResourceNotFoundException("Menú no encontrado.")); }
    private ConflictException duplicateNombre() { return new ConflictException("El nombre del Menú ya está registrado."); }
}
