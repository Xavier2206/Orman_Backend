package com.orman.backend.auth.mapper;

import com.orman.backend.auth.dto.response.SessionResponse;
import com.orman.backend.auth.entity.SesionUsuario;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(SesionUsuario session, UUID currentSid) {
        return new SessionResponse(session.getSid(), session.getDeviceId(), session.getDeviceName(),
                session.getClientType(), session.getFechaCreacion(), session.getFechaExpiracion(),
                session.getUltimoUso(), session.getSid().equals(currentSid));
    }
}
