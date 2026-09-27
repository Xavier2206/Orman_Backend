package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.payment.dto.request.QrCobroEstadoRequest;
import com.orman.backend.payment.dto.request.QrCobroRequest;
import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.entity.QrCobroEntity;
import com.orman.backend.payment.entity.QrCobroEstado;
import com.orman.backend.payment.mapper.QrCobroMapper;
import com.orman.backend.payment.repository.QrCobroRepository;
import com.orman.backend.payment.service.PaymentImageContent;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.payment.service.QrCobroService;
import com.orman.backend.payment.service.StoredPaymentImage;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QrCobroServiceImpl implements QrCobroService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/La_Paz");

    private final QrCobroRepository qrCobroRepository;
    private final PersonaRepository personaRepository;
    private final QrCobroMapper qrCobroMapper;
    private final PaymentImageStorageService imageStorageService;
    private final PaymentOwnershipService paymentOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final Clock clock;

    @Override
    @Transactional
    public QrCobroResponse create(QrCobroRequest request, MultipartFile imagen, Authentication authentication) {
        validateDateRange(request.fechaInicio(), request.fechaFin());
        Persona owner = propertyOwnershipService.currentPropietaria(authentication);
        lockOwner(owner.getCodper());
        validateNoOverlap(owner.getCodper(), request.fechaInicio(), request.fechaFin(), null);

        StoredPaymentImage stored = imageStorageService.storeQr(imagen, owner.getCodper());
        QrCobroEntity qr = new QrCobroEntity();
        qr.setPropietaria(owner);
        qr.setRutaArchivo(stored.rutaArchivo());
        qr.setNombreArchivo(stored.nombreArchivo());
        qr.setTipoContenido(stored.tipoContenido());
        qr.setFechaInicio(request.fechaInicio());
        qr.setFechaFin(request.fechaFin());
        qr.setEstado(QrCobroEstado.ACTIVO);
        qr.setFechaRegistro(nowUtc());
        try {
            return qrCobroMapper.toResponse(qrCobroRepository.saveAndFlush(qr));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("No se pudo registrar el QR de cobro con esa vigencia.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QrCobroResponse> list(Authentication authentication) {
        Integer ownerId = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return qrCobroRepository.findAllByPropietariaCodperOrderByFechaInicioDescCodqrDesc(ownerId).stream()
                .map(qrCobroMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QrCobroResponse get(Integer codqr, Authentication authentication) {
        Integer ownerId = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return qrCobroMapper.toResponse(findOwned(codqr, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public QrCobroResponse current(Authentication authentication) {
        Integer ownerId = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return qrCobroMapper.toResponse(findCurrent(ownerId, today()));
    }

    @Override
    @Transactional(readOnly = true)
    public QrCobroResponse currentForQuota(Integer codcuo, Authentication authentication) {
        var cuota = paymentOwnershipService.findAccessibleCuota(codcuo, authentication);
        Integer ownerId = ownerId(cuota);
        return qrCobroMapper.toResponse(findCurrent(ownerId, today()));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentImageContent image(Integer codqr, Authentication authentication) {
        Integer ownerId = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        QrCobroEntity qr = findOwned(codqr, ownerId);
        return imageStorageService.loadQr(qr.getRutaArchivo(), ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentImageContent imageForQuota(Integer codcuo, Authentication authentication) {
        var cuota = paymentOwnershipService.findAccessibleCuota(codcuo, authentication);
        Integer ownerId = ownerId(cuota);
        QrCobroEntity qr = findCurrent(ownerId, today());
        return imageStorageService.loadQr(qr.getRutaArchivo(), ownerId);
    }

    @Override
    @Transactional
    public QrCobroResponse changeState(Integer codqr, QrCobroEstadoRequest request,
                                       Authentication authentication) {
        Integer ownerId = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        lockOwner(ownerId);
        QrCobroEntity qr = findOwned(codqr, ownerId);
        if (request.estado() == QrCobroEstado.ACTIVO) {
            validateNoOverlap(ownerId, qr.getFechaInicio(), qr.getFechaFin(), codqr);
        }
        qr.setEstado(request.estado());
        return qrCobroMapper.toResponse(qrCobroRepository.saveAndFlush(qr));
    }

    @Override
    @Transactional(readOnly = true)
    public QrCobroEntity resolveForPayment(Integer codperPropietaria, LocalDate today, LocalDate fechaPago) {
        QrCobroEntity current;
        try {
            current = findCurrent(codperPropietaria, today);
        } catch (ResourceNotFoundException exception) {
            throw new BusinessRuleException("No existe un QR de cobro vigente para esta propietaria.");
        }
        if (fechaPago == null || fechaPago.isBefore(current.getFechaInicio()) || fechaPago.isAfter(current.getFechaFin())) {
            throw new BusinessRuleException("La fechaPago debe estar dentro de la vigencia del QR de cobro.");
        }
        return current;
    }

    private QrCobroEntity findCurrent(Integer ownerId, LocalDate date) {
        List<QrCobroEntity> current = qrCobroRepository.findActiveForDate(ownerId, date);
        if (current.isEmpty()) {
            throw new ResourceNotFoundException("No existe un QR de cobro vigente para esta propietaria.");
        }
        if (current.size() > 1) {
            throw new BusinessRuleException("Existe más de un QR de cobro vigente para esta propietaria.");
        }
        return current.getFirst();
    }

    private QrCobroEntity findOwned(Integer codqr, Integer ownerId) {
        return qrCobroRepository.findByCodqrAndPropietariaCodper(codqr, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("El QR de cobro solicitado no existe."));
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new BusinessRuleException("La fechaInicio del QR debe ser anterior o igual a fechaFin.");
        }
    }

    private void validateNoOverlap(Integer ownerId, LocalDate start, LocalDate end, Integer excludeCodqr) {
        if (qrCobroRepository.existsActiveOverlap(ownerId, start, end, excludeCodqr)) {
            throw new BusinessRuleException("La vigencia del QR se superpone con otro QR activo.");
        }
    }

    private void lockOwner(Integer ownerId) {
        personaRepository.findByCodperForUpdate(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("La propietaria no existe."));
    }

    private Integer ownerId(com.orman.backend.contract.entity.CuotaEntity cuota) {
        return cuota.getContrato().getUnidad().getPropiedad().getPropietaria().getCodper();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
