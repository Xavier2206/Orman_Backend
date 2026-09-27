package com.orman.backend.contract.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.CuotaListCriteria;
import com.orman.backend.contract.dto.response.CuotaListItemResponse;
import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.mapper.CuotaMapper;
import com.orman.backend.contract.repository.CuotaListPage;
import com.orman.backend.contract.repository.CuotaListProjection;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuotaServiceImpl implements CuotaService {

    private final CuotaRepository cuotaRepository;
    private final CuotaMapper cuotaMapper;
    private final ContractOwnershipService contractOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final Clock clock;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/La_Paz");

    @Override
    @Transactional
    public void generatePending(ContratoEntity contrato) {
        List<LocalDate> periodos = periods(contrato);
        for (LocalDate periodo : periodos) {
            if (cuotaRepository.existsByContratoCodconAndPeriodo(contrato.getCodcon(), periodo)) {
                throw new ConflictException("Ya existen cuotas generadas para este Contrato.");
            }
        }
        try {
            cuotaRepository.saveAllAndFlush(periodos.stream()
                    .map(periodo -> cuotaMapper.toPendingEntity(contrato, periodo)).toList());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Ya existen cuotas generadas para este Contrato.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuotaResponse> listByContrato(Integer codcon, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        return cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(codcon).stream()
                .map(cuota -> cuotaMapper.toResponse(cuota,
                        cuotaRepository.sumPaymentAmountByState(cuota.getCodcuo(), "CONFIRMADO"),
                        cuotaRepository.sumPaymentAmountByState(cuota.getCodcuo(), "PENDIENTE_REVISION")))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CuotaListItemResponse> listGlobal(CuotaListCriteria criteria,
                                                           Authentication authentication) {
        Integer codperPropietaria = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        LocalDate hoy = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate fechaLimite = hoy.plusDays(7);
        CuotaListPage result = cuotaRepository.searchOwned(codperPropietaria, criteria, hoy, fechaLimite);
        List<CuotaListItemResponse> content = result.content().stream()
                .map(row -> toListItem(row, hoy, fechaLimite))
                .toList();
        int totalPages = totalPages(result.totalElements(), criteria.size());
        boolean first = criteria.page() == 0;
        boolean last = totalPages == 0 || criteria.page() >= totalPages - 1;
        return new PageResponse<>(content, criteria.page(), criteria.size(), result.totalElements(), totalPages,
                first, last);
    }

    private CuotaListItemResponse toListItem(CuotaListProjection row, LocalDate hoy, LocalDate fechaLimite) {
        String situacion;
        if (row.saldo().signum() <= 0) {
            situacion = "SIN_SALDO";
        } else if (row.fechaVencimiento().isBefore(hoy)) {
            situacion = "VENCIDA";
        } else if (row.fechaVencimiento().isEqual(hoy)) {
            situacion = "HOY";
        } else if (!row.fechaVencimiento().isAfter(fechaLimite)) {
            situacion = "PROXIMA";
        } else {
            situacion = "AL_DIA";
        }
        return new CuotaListItemResponse(row.codcuo(), row.codcon(), row.periodo(), row.fechaVencimiento(),
                row.codperInquilino(), row.nombreCompleto(), row.ci(), row.codprop(), row.nombrePropiedad(),
                row.coduni(), row.nombreUnidad(), row.monto(), row.montoConfirmado(),
                row.montoPendienteRevision(), row.saldo(), row.estado(), situacion);
    }

    private int totalPages(long totalElements, int size) {
        long pages = totalElements / size + (totalElements % size == 0 ? 0 : 1);
        return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
    }

    private List<LocalDate> periods(ContratoEntity contrato) {
        List<LocalDate> periodos = new ArrayList<>();
        LocalDate period = contrato.getFechaInicio();
        while (period.isBefore(contrato.getFechaFin())) {
            periodos.add(period);
            period = period.plusMonths(1);
        }
        return periodos;
    }
}
