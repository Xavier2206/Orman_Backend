package com.orman.backend.property.service;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface UnidadFotoService {

    UnidadFotoResponse create(Integer coduni, UnidadFotoRequest request, Authentication authentication);

    UnidadFotoResponse createInternal(Integer coduni, MultipartFile foto, UnidadFotoMetadataRequest request,
                                      Authentication authentication);

    List<UnidadFotoResponse> listByUnidad(Integer coduni, Authentication authentication);

    UnidadFotoResponse update(Integer coduni, Integer id, UnidadFotoRequest request, Authentication authentication);

    UnidadFotoResponse updateMetadata(Integer coduni, Integer id, UnidadFotoMetadataRequest request,
                                      Authentication authentication);

    UnidadFotoResponse replaceArchivo(Integer coduni, Integer id, MultipartFile foto, Authentication authentication);

    UnidadFotoResource getArchivo(Integer coduni, Integer id, Authentication authentication);

    UnidadFotoResponse setPortada(Integer coduni, Integer id, Authentication authentication);

    void delete(Integer coduni, Integer id, Authentication authentication);

    record UnidadFotoResource(Resource resource, MediaType mediaType) {
    }
}
