package com.orman.backend.person.service;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PersonaService {
    PersonaResponse create(CreatePersonaRequest request);
    PersonaResponse get(Integer codper);
    PageResponse<PersonaResponse> list(Pageable pageable);
    PersonaResponse update(Integer codper, UpdatePersonaRequest request);
    PersonaResponse deactivate(Integer codper);
    PersonaResponse activate(Integer codper);
    void delete(Integer codper);
}
