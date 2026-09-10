package com.orman.backend.property.service;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface UnidadFotoService {

    UnidadFotoResponse create(Integer coduni, UnidadFotoRequest request, Authentication authentication);

    List<UnidadFotoResponse> listByUnidad(Integer coduni, Authentication authentication);

    UnidadFotoResponse update(Integer coduni, Integer id, UnidadFotoRequest request, Authentication authentication);

    UnidadFotoResponse setPortada(Integer coduni, Integer id, Authentication authentication);

    void delete(Integer coduni, Integer id, Authentication authentication);
}
