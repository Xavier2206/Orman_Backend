package com.orman.backend.auth.mapper;

import com.orman.backend.auth.dto.response.AuthContextMenuResponse;
import com.orman.backend.auth.dto.response.AuthContextPersonaResponse;
import com.orman.backend.auth.dto.response.AuthContextProcesoResponse;
import com.orman.backend.auth.dto.response.AuthContextResponse;
import com.orman.backend.auth.dto.response.AuthContextRolResponse;
import com.orman.backend.auth.dto.response.AuthContextUsuarioResponse;
import com.orman.backend.auth.repository.AuthContextNavigationRow;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthContextMapper {

    public AuthContextResponse toResponse(Usuario usuario, List<AuthContextNavigationRow> rows) {
        Persona persona = usuario.getPersona();
        Map<Integer, RolAccumulator> roles = new LinkedHashMap<>();

        for (AuthContextNavigationRow row : rows) {
            RolAccumulator rol = roles.computeIfAbsent(row.codr(),
                    ignored -> new RolAccumulator(row.codr(), row.rolNombre()));
            if (row.codm() == null) {
                continue;
            }
            MenuAccumulator menu = rol.menus().computeIfAbsent(row.codm(),
                    ignored -> new MenuAccumulator(row.codm(), row.menuNombre(), row.menuIcono()));
            if (row.codp() != null) {
                menu.procesos().putIfAbsent(row.codp(),
                        new AuthContextProcesoResponse(row.codp(), row.procesoNombre(), row.procesoEnlace()));
            }
        }

        List<AuthContextRolResponse> roleResponses = roles.values().stream()
                .map(rol -> new AuthContextRolResponse(rol.codr(), rol.nombre(), rol.menus().values().stream()
                        .map(menu -> new AuthContextMenuResponse(menu.codm(), menu.nombre(), menu.icono(),
                                List.copyOf(menu.procesos().values())))
                        .toList()))
                .toList();

        return new AuthContextResponse(new AuthContextUsuarioResponse(usuario.getLogin(), persona.getCodper()),
                new AuthContextPersonaResponse(persona.getNombre(), persona.getAp(), persona.getAm(), persona.getFoto()),
                roleResponses);
    }

    private record RolAccumulator(Integer codr, String nombre, Map<Integer, MenuAccumulator> menus) {
        private RolAccumulator(Integer codr, String nombre) {
            this(codr, nombre, new LinkedHashMap<>());
        }
    }

    private record MenuAccumulator(Integer codm, String nombre, String icono,
            Map<Integer, AuthContextProcesoResponse> procesos) {
        private MenuAccumulator(Integer codm, String nombre, String icono) {
            this(codm, nombre, icono, new LinkedHashMap<>());
        }
    }
}
