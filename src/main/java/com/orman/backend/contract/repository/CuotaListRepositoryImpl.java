package com.orman.backend.contract.repository;

import com.orman.backend.contract.dto.request.CuotaListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CuotaListRepositoryImpl implements CuotaListRepository {

    private static final String CTE = """
            WITH cuotas_base AS (
                SELECT q.codcuo,
                       q.codcon,
                       q.periodo,
                       q.fecha_vencimiento,
                       i.codper AS codper_inquilino,
                       concat_ws(' ', nullif(trim(i.nombre), ''), nullif(trim(i.ap), ''),
                                     nullif(trim(i.am), '')) AS nombre_completo,
                       i.ci,
                       pr.codprop,
                       pr.nombre AS nombre_propiedad,
                       u.coduni,
                       u.nombre AS nombre_unidad,
                       q.monto,
                       q.estado
                  FROM cuotas q
                  JOIN contratos c ON c.codcon = q.codcon
                  JOIN personas i ON i.codper = c.codper_inquilino
                  JOIN unidades u ON u.coduni = c.coduni
                  JOIN propiedades pr ON pr.codprop = u.codprop
                 WHERE pr.codper_propietaria = :codperPropietaria
                   AND (:hasCodperInquilino = false OR c.codper_inquilino = :codperInquilino)
                   AND (:hasPeriodo = false OR q.periodo = :periodo)
                   AND (:estado = '' OR q.estado = :estado)
                   AND (:hasCodprop = false OR pr.codprop = :codprop)
                   AND (:hasCoduni = false OR u.coduni = :coduni)
            ), pagos_agregados AS (
                SELECT p.codcuo,
                       coalesce(sum(p.monto) FILTER (WHERE p.estado = 'CONFIRMADO'), 0) AS monto_confirmado,
                       coalesce(sum(p.monto) FILTER (WHERE p.estado = 'PENDIENTE_REVISION'), 0)
                           AS monto_pendiente_revision
                  FROM pagos p
                  JOIN cuotas_base cb ON cb.codcuo = p.codcuo
                 WHERE p.estado IN ('CONFIRMADO', 'PENDIENTE_REVISION')
                 GROUP BY p.codcuo
            ), cuotas_enriquecidas AS (
                SELECT cb.*,
                       coalesce(pa.monto_confirmado, 0) AS monto_confirmado,
                       coalesce(pa.monto_pendiente_revision, 0) AS monto_pendiente_revision,
                       cb.monto - coalesce(pa.monto_confirmado, 0) AS saldo
                  FROM cuotas_base cb
                  LEFT JOIN pagos_agregados pa ON pa.codcuo = cb.codcuo
            )
            """;

    private static final String FILTERS = """
            WHERE (:conPagoPendienteRevision = false OR q.monto_pendiente_revision > 0)
              AND (:vencimiento = ''
                   OR (:vencimiento = 'VENCIDAS'
                       AND q.periodo < :hoy AND q.saldo > 0)
                   OR (:vencimiento = 'HOY'
                       AND q.periodo = :hoy AND q.saldo > 0)
                   OR (:vencimiento = 'PROXIMAS'
                       AND q.periodo > :hoy AND q.periodo <= :fechaLimite AND q.saldo > 0)
                   OR (:vencimiento = 'AL_DIA'
                       AND q.periodo > :fechaLimite AND q.saldo > 0))
            """;

    private final EntityManager entityManager;

    @Override
    public CuotaListPage searchOwned(Integer codperPropietaria, CuotaListCriteria criteria, LocalDate hoy,
                                     LocalDate fechaLimite) {
        Query dataQuery = entityManager.createNativeQuery(CTE + """
                SELECT q.codcuo, q.codcon, q.periodo, q.fecha_vencimiento, q.codper_inquilino,
                       q.nombre_completo, q.ci, q.codprop, q.nombre_propiedad, q.coduni, q.nombre_unidad,
                       q.monto, q.monto_confirmado, q.monto_pendiente_revision, q.saldo, q.estado
                  FROM cuotas_enriquecidas q
                """ + FILTERS + """
                ORDER BY CASE
                           WHEN q.periodo < :hoy AND q.saldo > 0 THEN 0
                           WHEN q.periodo = :hoy AND q.saldo > 0 THEN 1
                           WHEN q.periodo > :hoy AND q.periodo <= :fechaLimite
                                AND q.saldo > 0 THEN 2
                           ELSE 3
                         END,
                         q.periodo ASC,
                         q.codcuo ASC
                LIMIT :limit OFFSET :offset
                """);
        bindFilters(dataQuery, codperPropietaria, criteria, hoy, fechaLimite);
        dataQuery.setParameter("limit", criteria.size());
        dataQuery.setParameter("offset", (long) criteria.page() * criteria.size());

        List<CuotaListProjection> content = toRows(dataQuery.getResultList());

        Query countQuery = entityManager.createNativeQuery(CTE + "SELECT count(*) FROM cuotas_enriquecidas q " + FILTERS);
        bindFilters(countQuery, codperPropietaria, criteria, hoy, fechaLimite);
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();
        return new CuotaListPage(content, totalElements);
    }

    @SuppressWarnings("unchecked")
    private static List<CuotaListProjection> toRows(List<?> resultList) {
        return ((List<Object[]>) resultList).stream()
                .map(CuotaListRepositoryImpl::toProjection)
                .toList();
    }

    private static void bindFilters(Query query, Integer codperPropietaria, CuotaListCriteria criteria,
                                    LocalDate hoy, LocalDate fechaLimite) {
        query.setParameter("codperPropietaria", codperPropietaria);
        query.setParameter("hasCodperInquilino", criteria.codperInquilino() != null);
        query.setParameter("codperInquilino", criteria.codperInquilino() == null ? -1 : criteria.codperInquilino());
        query.setParameter("hasPeriodo", criteria.periodo() != null);
        query.setParameter("periodo", criteria.periodo() == null ? LocalDate.of(1, 1, 1) : criteria.periodo());
        query.setParameter("estado", criteria.estado() == null ? "" : criteria.estado().name());
        query.setParameter("hasCodprop", criteria.codprop() != null);
        query.setParameter("codprop", criteria.codprop() == null ? -1 : criteria.codprop());
        query.setParameter("hasCoduni", criteria.coduni() != null);
        query.setParameter("coduni", criteria.coduni() == null ? -1 : criteria.coduni());
        query.setParameter("conPagoPendienteRevision", criteria.conPagoPendienteRevision());
        query.setParameter("vencimiento", criteria.vencimiento() == null ? "" : criteria.vencimiento().name());
        query.setParameter("hoy", hoy);
        query.setParameter("fechaLimite", fechaLimite);
    }

    private static CuotaListProjection toProjection(Object[] row) {
        return new CuotaListProjection(
                integer(row[0]), integer(row[1]), date(row[2]), date(row[3]), integer(row[4]),
                (String) row[5], (String) row[6], integer(row[7]), (String) row[8], integer(row[9]),
                (String) row[10], decimal(row[11]), decimal(row[12]), decimal(row[13]), decimal(row[14]),
                (String) row[15]);
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("La consulta de cuotas devolvió una fecha inesperada.");
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        throw new IllegalStateException("La consulta de cuotas devolvió un monto inesperado.");
    }
}
