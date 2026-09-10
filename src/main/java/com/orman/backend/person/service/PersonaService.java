package com.orman.backend.person.service;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.PersonaResumenResponse;
import com.orman.backend.person.dto.PersonaSearchCriteria;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface PersonaService {
    PersonaResponse create(CreatePersonaRequest request);
    PersonaResponse create(CreatePersonaRequest request, Authentication authentication);
    PersonaResponse get(Integer codper);
    PersonaResponse get(Integer codper, Authentication authentication);
    PersonaResumenResponse resumen();
    PageResponse<PersonaResponse> list(Pageable pageable);
    PageResponse<PersonaResponse> list(PersonaSearchCriteria criteria, Pageable pageable, Authentication authentication);
    PersonaResponse update(Integer codper, UpdatePersonaRequest request);
    PersonaResponse update(Integer codper, UpdatePersonaRequest request, Authentication authentication);
    PersonaResponse deactivate(Integer codper);
    PersonaResponse deactivate(Integer codper, Authentication authentication);
    PersonaResponse activate(Integer codper);
    PersonaResponse activate(Integer codper, Authentication authentication);
    void delete(Integer codper);
}
