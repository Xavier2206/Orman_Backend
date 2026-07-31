package com.orman.backend.person.repository;

import com.orman.backend.person.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {

    boolean existsByCi(String ci);

    boolean existsByCiAndCodperNot(String ci, Integer codper);
}
