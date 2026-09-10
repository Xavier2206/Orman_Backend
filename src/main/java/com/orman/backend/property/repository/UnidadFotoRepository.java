package com.orman.backend.property.repository;

import com.orman.backend.property.entity.UnidadFotoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadFotoRepository extends JpaRepository<UnidadFotoEntity, Integer> {

    boolean existsByUnidadCoduniAndOrden(Integer coduni, Integer orden);

    boolean existsByUnidadCoduniAndOrdenAndIdNot(Integer coduni, Integer orden, Integer id);

    Optional<UnidadFotoEntity> findByIdAndUnidadCoduni(Integer id, Integer coduni);

    Optional<UnidadFotoEntity> findByIdAndUnidadCoduniAndUnidadPropiedadPropietariaCodper(
            Integer id, Integer coduni, Integer codper);

    List<UnidadFotoEntity> findAllByUnidadCoduniAndUnidadPropiedadPropietariaCodperOrderByOrdenAscIdAsc(
            Integer coduni, Integer codper);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UnidadFotoEntity f set f.portada = false where f.unidad.coduni = :coduni and f.portada = true")
    void clearPortadaByUnidadCoduni(@Param("coduni") Integer coduni);
}
