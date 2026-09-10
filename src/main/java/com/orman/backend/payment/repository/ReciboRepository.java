package com.orman.backend.payment.repository;

import com.orman.backend.payment.entity.ReciboEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReciboRepository extends JpaRepository<ReciboEntity, Integer> {

    boolean existsByPagoCodpag(Integer codpag);

    @EntityGraph(attributePaths = {"pago", "pago.cuota", "pago.cuota.contrato", "pago.cuota.contrato.unidad",
            "pago.cuota.contrato.unidad.propiedad", "pago.cuota.contrato.unidad.propiedad.propietaria"})
    Optional<ReciboEntity> findByPagoCodpag(Integer codpag);

    @EntityGraph(attributePaths = {"pago", "pago.cuota", "pago.cuota.contrato", "pago.cuota.contrato.unidad",
            "pago.cuota.contrato.unidad.propiedad", "pago.cuota.contrato.unidad.propiedad.propietaria"})
    Optional<ReciboEntity> findByCodrec(Integer codrec);
}
