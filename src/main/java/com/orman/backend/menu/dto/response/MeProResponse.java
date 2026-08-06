package com.orman.backend.menu.dto.response;

public record MeProResponse(
        Integer codm, String nombreMenu, Short estadoMenu,
        Integer codp, String nombreProceso, String enlaceProceso, Short estadoProceso) {
}
