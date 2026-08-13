package com.orman.backend.auth.dto.response;

import java.util.List;

public record AuthContextRolResponse(Integer codr, String nombre, List<AuthContextMenuResponse> menus) {
}
