package com.orman.backend.auth.dto.response;

import java.util.List;

public record AuthContextMenuResponse(Integer codm, String nombre, String icono,
        List<AuthContextProcesoResponse> procesos) {
}
