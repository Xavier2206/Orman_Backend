package com.orman.backend.dashboard.repository;

import com.orman.backend.property.entity.PropiedadEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DashboardRepository extends Repository<PropiedadEntity, Integer> {

    @Query(value = """
            SELECT p.codprop AS codprop,
                   p.nombre AS nombre,
                   p.inversion_inicial AS "inversionInicial"
            FROM propiedades p
            WHERE p.codper_propietaria = :codper
            ORDER BY p.nombre ASC, p.codprop ASC
            """, nativeQuery = true)
    List<DashboardPropiedadProjection> findPropiedadesByPropietaria(@Param("codper") Integer codper);

    @Query(value = """
            SELECT pr.codprop AS codprop,
                   COALESCE(SUM(pg.monto), 0) AS total
            FROM pagos pg
            JOIN cuotas q ON q.codcuo = pg.codcuo
            JOIN contratos c ON c.codcon = q.codcon
            JOIN unidades u ON u.coduni = c.coduni
            JOIN propiedades pr ON pr.codprop = u.codprop
            WHERE pr.codper_propietaria = :codper
              AND pg.estado = 'CONFIRMADO'
            GROUP BY pr.codprop
            """, nativeQuery = true)
    List<DashboardIngresoPropiedadProjection> sumarIngresosConfirmadosPorPropiedad(
            @Param("codper") Integer codper);

    @Query(value = """
            SELECT pr.codprop AS codprop,
                   EXTRACT(YEAR FROM pg.fecha_revision)::INTEGER AS anio,
                   EXTRACT(MONTH FROM pg.fecha_revision)::INTEGER AS mes,
                   COALESCE(SUM(pg.monto), 0) AS total
            FROM pagos pg
            JOIN cuotas q ON q.codcuo = pg.codcuo
            JOIN contratos c ON c.codcon = q.codcon
            JOIN unidades u ON u.coduni = c.coduni
            JOIN propiedades pr ON pr.codprop = u.codprop
            WHERE pr.codper_propietaria = :codper
              AND pg.estado = 'CONFIRMADO'
              AND pg.fecha_revision >= :desde
              AND pg.fecha_revision < :hasta
            GROUP BY pr.codprop, EXTRACT(YEAR FROM pg.fecha_revision), EXTRACT(MONTH FROM pg.fecha_revision)
            ORDER BY pr.codprop ASC, anio ASC, mes ASC
            """, nativeQuery = true)
    List<DashboardIngresoMensualProjection> sumarIngresosMensualesConfirmados(
            @Param("codper") Integer codper,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
