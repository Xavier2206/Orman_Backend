package com.orman.backend.user.mapper;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(CreateUsuarioRequest request, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(request.login().trim());
        usuario.setEstado(request.estado());
        usuario.setPersona(persona);
        return usuario;
    }

    public void update(Usuario usuario, UpdateUsuarioRequest request) {
        usuario.setEstado(request.estado());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getLogin(), usuario.getEstado(), usuario.getPersona().getCodper(),
                usuario.getFechaCreacion(), usuario.getUltimoAcceso());
    }
}
