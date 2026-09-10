package com.orman.backend.contract.repository;

import com.orman.backend.contract.entity.ContratoArchivoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoArchivoRepository extends JpaRepository<ContratoArchivoEntity, Integer> {

    boolean existsByContratoCodconAndOrden(Integer codcon, Integer orden);

    List<ContratoArchivoEntity> findAllByContratoCodconOrderByOrdenAscIdAsc(Integer codcon);
}
