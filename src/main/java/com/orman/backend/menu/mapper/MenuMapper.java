package com.orman.backend.menu.mapper;

import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.request.UpdateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResponse;
import com.orman.backend.menu.entity.Menu;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MenuMapper {

    public Menu toEntity(CreateMenuRequest request) {
        return new Menu(normalizeNombre(request.nombre()), normalizeOptional(request.icono()), request.estado());
    }

    public void update(Menu menu, UpdateMenuRequest request) {
        menu.setNombre(normalizeNombre(request.nombre()));
        menu.setIcono(normalizeOptional(request.icono()));
    }

    public MenuResponse toResponse(Menu menu) {
        return new MenuResponse(menu.getCodm(), menu.getNombre(), menu.getIcono(), menu.getEstado());
    }

    public String normalizeNombre(String nombre) {
        return nombre == null ? null : nombre.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
