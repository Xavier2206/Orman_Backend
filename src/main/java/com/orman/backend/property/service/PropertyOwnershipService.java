package com.orman.backend.property.service;

import com.orman.backend.person.entity.Persona;
import org.springframework.security.core.Authentication;

public interface PropertyOwnershipService {

    Persona currentPropietaria(Authentication authentication);

    void assertCurrentPropietaria(Authentication authentication, Persona propietaria);

    void assertCurrentPropietaria(Authentication authentication, Integer codperPropietaria);
}
