package com.orman.backend.contract.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.mapper.ContratoMapper;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoService {

    private static final short ACTIVO = 1;
    private static final String MONEDA_BOB = "BOB";
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");

    private final ContratoRepository contratoRepository;
    private final PersonaRepository personaRepository;
    private final ContratoMapper contratoMapper;
    private final ContractOwnershipService contractOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final CuotaService cuotaService;
    private final CuotaRepository cuotaRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ContratoResponse create(Integer coduni, ContratoRequest request, Authentication authentication) {
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidadForUpdate(coduni, authentication);
        assertUnidadOperativa(unidad);
        assertPropiedadHabilitada(unidad);
        validateDates(request.fechaInicio(), request.fechaFin());
        validateMoney(request.montoMensual(), request.garantia());
        Persona inquilino = findActiveInquilino(request.codperInquilino());
        if (contratoRepository.existsActiveOverlap(coduni, request.fechaInicio(), request.fechaFin())) {
            throw new ConflictException("El período del Contrato se solapa con otro Contrato PROGRAMADO o VIGENTE.");
        }
        LocalDate fechaActual = currentDate();
        ContratoEstado estado = request.fechaInicio().isAfter(fechaActual)
                ? ContratoEstado.PROGRAMADO : ContratoEstado.VIGENTE;
        ContratoEntity saved = contratoRepository.saveAndFlush(contratoMapper.toEntity(request, unidad, inquilino,
                estado, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)));
        assertCurrency(saved.getMoneda());
        cuotaService.generatePending(saved);
        return contratoMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> listByUnidad(Integer coduni, Pageable pageable, Authentication authentication) {
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidad(coduni, authentication);
        Page<ContratoResponse> page = contratoRepository.findAllByUnidadOwned(coduni,
                unidad.getPropiedad().getPropietaria().getCodper(), defaultSort(pageable))
                .map(contratoMapper::toResponse);
        return pageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> list(String q, Integer codprop, Integer coduni, ContratoEstado estado,
                                                Pageable pageable, Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<ContratoResponse> page = contratoRepository.searchOwned(codper, codprop, coduni, estado,
                        normalizeQuery(q), defaultSort(pageable))
                .map(contratoMapper::toResponse);
        return pageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse get(Integer codcon, Authentication authentication) {
        return contratoMapper.toResponse(contractOwnershipService.findOwnedContrato(codcon, authentication));
    }

    @Override
    @Transactional
    public ContratoResponse finish(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.VIGENTE, "Solo se puede finalizar un Contrato vigente.");
        if (currentDate().isBefore(contrato.getFechaFin())) {
            throw new BusinessRuleException("El Contrato no puede finalizar antes de su fecha de fin.");
        }
        if (cuotaRepository.existsPendingReviewPaymentByContrato(codcon)) {
            throw new BusinessRuleException("El Contrato tiene Pagos pendientes de revisión.");
        }
        if (cuotaRepository.existsByContratoAndEstados(codcon,
                java.util.List.of(com.orman.backend.contract.entity.CuotaEstado.PENDIENTE,
                        com.orman.backend.contract.entity.CuotaEstado.PARCIAL))) {
            throw new BusinessRuleException("Todas las Cuotas exigibles deben estar pagadas para finalizar.");
        }
        contrato.setEstado(ContratoEstado.FINALIZADO);
        return contratoMapper.toResponse(contratoRepository.saveAndFlush(contrato));
    }

    @Override
    @Transactional
    public ContratoResponse rescind(Integer codcon, RescisionContratoRequest request, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.VIGENTE, "Solo se puede rescindir un Contrato vigente.");
        if (request.fechaRescision().isBefore(contrato.getFechaInicio())
                || !request.fechaRescision().isBefore(contrato.getFechaFin())) {
            throw new BusinessRuleException("La fecha de rescisión debe pertenecer al período contractual.");
        }
        if (request.fechaRescision().getDayOfMonth() != 1) {
            throw new BusinessRuleException("La fecha de rescisión debe ser el primer día del mes.");
        }
        if (request.fechaRescision().isAfter(currentDate().withDayOfMonth(1))) {
            throw new BusinessRuleException("La fecha de rescisión no puede corresponder a un período futuro.");
        }
        if (cuotaRepository.existsPendingReviewPaymentByContrato(codcon)) {
            throw new BusinessRuleException("El Contrato tiene Pagos pendientes de revisión que deben resolverse.");
        }
        if (cuotaRepository.existsUnpaidThroughPeriod(codcon, request.fechaRescision())) {
            throw new BusinessRuleException("Todas las Cuotas hasta el período de rescisión inclusive deben estar pagadas.");
        }
        cuotaRepository.annulAfterPeriod(codcon, request.fechaRescision());
        contrato.setEstado(ContratoEstado.RESCINDIDO);
        contrato.setFechaRescision(request.fechaRescision());
        contrato.setMotivoRescision(request.motivoRescision().trim());
        return contratoMapper.toResponse(contratoRepository.saveAndFlush(contrato));
    }

    private Persona findActiveInquilino(Integer codperInquilino) {
        Persona inquilino = personaRepository.findById(codperInquilino)
                .orElseThrow(() -> new BusinessRuleException("La Persona inquilina no existe."));
        if (!Short.valueOf(ACTIVO).equals(inquilino.getEstado())) {
            throw new BusinessRuleException("La Persona inquilina debe estar activa.");
        }
        return inquilino;
    }

    private void assertUnidadOperativa(UnidadEntity unidad) {
        if (!Short.valueOf(ACTIVO).equals(unidad.getEstadoOperativo())) {
            throw new BusinessRuleException("La Unidad debe estar operativa para gestionar el Contrato.");
        }
    }

    private void assertPropiedadHabilitada(UnidadEntity unidad) {
        if (!Short.valueOf(ACTIVO).equals(unidad.getPropiedad().getEstado())) {
            throw new BusinessRuleException("La Propiedad debe estar habilitada para registrar nuevos Contratos.");
        }
    }

    private void validateDates(LocalDate fechaInicio, LocalDate fechaFin) {
        if (!fechaInicio.isBefore(fechaFin)) {
            throw new BusinessRuleException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        if (fechaInicio.getDayOfMonth() != 1 || fechaFin.getDayOfMonth() != 1) {
            throw new BusinessRuleException("Las fechas del Contrato deben ser el primer día del mes.");
        }
    }

    private void validateMoney(BigDecimal montoMensual, BigDecimal garantia) {
        if (montoMensual == null || montoMensual.compareTo(BigDecimal.ZERO) <= 0
                || montoMensual.scale() > 2) {
            throw new BusinessRuleException("El monto mensual debe ser mayor a cero y tener máximo dos decimales.");
        }
        if (garantia == null || garantia.compareTo(BigDecimal.ZERO) < 0 || garantia.scale() > 2) {
            throw new BusinessRuleException("La garantía no puede ser negativa y debe tener máximo dos decimales.");
        }
    }

    private void assertCurrency(String moneda) {
        if (!MONEDA_BOB.equals(moneda)) {
            throw new BusinessRuleException("ORMAN admite únicamente moneda BOB.");
        }
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock.withZone(ZONA_NEGOCIO));
    }

    private void assertState(ContratoEntity contrato, ContratoEstado expected, String message) {
        if (contrato.getEstado() != expected) {
            throw new BusinessRuleException(message);
        }
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "fechaInicio"));
    }

    private String normalizeQuery(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String singleSpaced = value.trim().replaceAll("\\s+", " ");
        return Normalizer.normalize(singleSpaced, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();
    }

    private PageResponse<ContratoResponse> pageResponse(Page<ContratoResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
