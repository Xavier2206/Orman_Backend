package com.orman.backend.contract.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.dto.response.ContratoResumenResponse;
import com.orman.backend.contract.dto.response.ContratoInquilinoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.mapper.ContratoMapper;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.ContratoResumenProjection;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.contract.dto.response.ContratoCuotasResumenResponse;
import com.orman.backend.contract.repository.ContratoCuotasResumenProjection;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private static final char TIPO_PERSONA_INQUILINO = 'I';
    private static final String MONEDA_BOB = "BOB";
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
                estado, OrmanTimeConfig.businessNow(clock)));
        assertCurrency(saved.getMoneda());
        cuotaService.generatePending(saved);
        return contratoMapper.toResponse(saved, getCuotasResumen(saved.getCodcon()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> listByUnidad(Integer coduni, Pageable pageable, Authentication authentication) {
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidad(coduni, authentication);
        Page<ContratoEntity> page = contratoRepository.findAllByUnidadOwned(coduni,
                unidad.getPropiedad().getPropietaria().getCodper(), defaultSort(pageable));
        Map<Integer, ContratoCuotasResumenResponse> cuotasMap = summarizeCuotas(page.getContent());
        Page<ContratoResponse> responsePage = page.map(c -> contratoMapper.toResponse(c, cuotasMap.get(c.getCodcon())));
        return pageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> list(String q, Integer codprop, Integer coduni, ContratoEstado estado,
                                                Pageable pageable, Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<ContratoEntity> page = contratoRepository.searchOwned(codper, codprop, coduni, estado,
                        normalizeQuery(q), defaultSort(pageable));
        Map<Integer, ContratoCuotasResumenResponse> cuotasMap = summarizeCuotas(page.getContent());
        Page<ContratoResponse> responsePage = page.map(c -> contratoMapper.toResponse(c, cuotasMap.get(c.getCodcon())));
        return pageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResumenResponse resumen(Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        ContratoResumenProjection resumen = contratoRepository.summarizeOwned(codper);
        return new ContratoResumenResponse(
                resumen.getVigentes(),
                resumen.getProgramados(),
                resumen.getFinalizados(),
                resumen.getRescindidos());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoInquilinoResponse> inquilinos(Authentication authentication) {
        Integer codperPropietaria = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return contratoRepository.findDistinctInquilinosOwned(codperPropietaria).stream()
                .map(contratoMapper::toInquilinoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse get(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContrato(codcon, authentication);
        return contratoMapper.toResponse(contrato, getCuotasResumen(codcon));
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
        ContratoEntity saved = contratoRepository.saveAndFlush(contrato);
        return contratoMapper.toResponse(saved, getCuotasResumen(codcon));
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
        ContratoEntity saved = contratoRepository.saveAndFlush(contrato);
        return contratoMapper.toResponse(saved, getCuotasResumen(codcon));
    }

    private Persona findActiveInquilino(Integer codperInquilino) {
        Persona inquilino = personaRepository.findById(codperInquilino)
                .orElseThrow(() -> new BusinessRuleException("La Persona inquilina no existe."));
        if (!Short.valueOf(ACTIVO).equals(inquilino.getEstado())) {
            throw new BusinessRuleException("La Persona inquilina debe estar activa.");
        }
        if (!Character.valueOf(TIPO_PERSONA_INQUILINO).equals(inquilino.getTipoPersona())) {
            throw new BusinessRuleException("La Persona seleccionada debe ser de tipo INQUILINO.");
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
        return OrmanTimeConfig.today(clock);
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

    private Map<Integer, ContratoCuotasResumenResponse> summarizeCuotas(List<ContratoEntity> contratos) {
        if (contratos == null || contratos.isEmpty()) {
            return Map.of();
        }
        List<Integer> codcons = contratos.stream().map(ContratoEntity::getCodcon).filter(Objects::nonNull).toList();
        if (codcons.isEmpty()) {
            return Map.of();
        }
        Map<Integer, ContratoCuotasResumenProjection> projectionMap = cuotaRepository.summarizeByContratoCodcons(codcons)
                .stream()
                .collect(Collectors.toMap(ContratoCuotasResumenProjection::getCodcon, p -> p));

        Map<Integer, ContratoCuotasResumenResponse> result = new HashMap<>();
        for (Integer codcon : codcons) {
            ContratoCuotasResumenProjection p = projectionMap.get(codcon);
            if (p != null) {
                result.put(codcon, new ContratoCuotasResumenResponse(
                        p.getTotalCuotas(),
                        p.getCuotasPagadas(),
                        p.getCuotasPendientes(),
                        p.getSaldoPendiente() != null
                                ? p.getSaldoPendiente().setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO.setScale(2)
                ));
            } else {
                result.put(codcon, new ContratoCuotasResumenResponse(0, 0, 0, BigDecimal.ZERO.setScale(2)));
            }
        }
        return result;
    }

    private ContratoCuotasResumenResponse getCuotasResumen(Integer codcon) {
        if (codcon == null) {
            return new ContratoCuotasResumenResponse(0, 0, 0, BigDecimal.ZERO.setScale(2));
        }
        List<ContratoCuotasResumenProjection> summaries = cuotaRepository.summarizeByContratoCodcons(List.of(codcon));
        if (summaries.isEmpty()) {
            return new ContratoCuotasResumenResponse(0, 0, 0, BigDecimal.ZERO.setScale(2));
        }
        ContratoCuotasResumenProjection p = summaries.getFirst();
        return new ContratoCuotasResumenResponse(
                p.getTotalCuotas(),
                p.getCuotasPagadas(),
                p.getCuotasPendientes(),
                p.getSaldoPendiente() != null
                        ? p.getSaldoPendiente().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2)
        );
    }
}
