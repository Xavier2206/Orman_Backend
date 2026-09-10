package com.orman.backend.notification.mapper;

import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {

    public NotificacionEntity toEntity(Usuario destinatario, NotificacionTipo tipo, String titulo, String mensaje,
                                       ReferenciaTipo referenciaTipo, Integer referenciaId) {
        NotificacionEntity notificacion = new NotificacionEntity();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setReferenciaTipo(referenciaTipo);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));
        return notificacion;
    }

    public NotificacionResponse toResponse(NotificacionEntity notificacion) {
        return new NotificacionResponse(notificacion.getCodnot(), notificacion.getTipo().name(),
                notificacion.getTitulo(), notificacion.getMensaje(), notificacion.getReferenciaTipo().name(),
                notificacion.getReferenciaId(), notificacion.getFechaCreacion(),
                notificacion.getFechaLectura() != null, notificacion.getFechaLectura());
    }
}
