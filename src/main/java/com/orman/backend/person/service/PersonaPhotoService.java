package com.orman.backend.person.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface PersonaPhotoService {

    void upload(Integer codper, MultipartFile foto);

    PersonaPhotoResource get(Integer codper);

    void delete(Integer codper);

    record PersonaPhotoResource(Resource resource, MediaType mediaType) {
    }
}
