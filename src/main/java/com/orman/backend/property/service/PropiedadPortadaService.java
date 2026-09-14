package com.orman.backend.property.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface PropiedadPortadaService {

    void upload(Integer codprop, MultipartFile foto, Authentication authentication);

    PropiedadPortadaResource get(Integer codprop, Authentication authentication);

    void delete(Integer codprop, Authentication authentication);

    record PropiedadPortadaResource(Resource resource, MediaType mediaType) {
    }
}
