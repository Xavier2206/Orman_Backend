package com.orman.backend.payment.repository;

import com.orman.backend.payment.entity.CuentaPagoEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaPagoRepository extends JpaRepository<CuentaPagoEntity, Integer> {

    @EntityGraph(attributePaths = "propietaria")
    Optional<CuentaPagoEntity> findByCodcta(Integer codcta);

    @EntityGraph(attributePaths = "propietaria")
    @Query(value = """
            select c from CuentaPagoEntity c
            where c.propietaria.codper = :codper
              and (:estado is null or c.estado = :estado)
            """, countQuery = """
            select count(c) from CuentaPagoEntity c
            where c.propietaria.codper = :codper
              and (:estado is null or c.estado = :estado)
            """)
    Page<CuentaPagoEntity> searchOwned(@Param("codper") Integer codper, @Param("estado") Short estado,
                                       Pageable pageable);

    @Query("""
            select (count(c) > 0) from CuentaPagoEntity c
            where c.propietaria.codper = :codper and c.banco = :banco and c.numeroCuenta = :numeroCuenta
              and (:excludeCodcta is null or c.codcta <> :excludeCodcta)
            """)
    boolean existsDuplicate(@Param("codper") Integer codper, @Param("banco") String banco,
                            @Param("numeroCuenta") String numeroCuenta, @Param("excludeCodcta") Integer excludeCodcta);
}
