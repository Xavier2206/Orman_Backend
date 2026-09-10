package com.orman.backend.payment.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.response.CuentaPagoResponse;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.payment.mapper.CuentaPagoMapper;
import com.orman.backend.payment.repository.CuentaPagoRepository;
import com.orman.backend.payment.service.CuentaPagoService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuentaPagoServiceImpl implements CuentaPagoService {

    private final CuentaPagoRepository cuentaPagoRepository;
    private final CuentaPagoMapper cuentaPagoMapper;
    private final PaymentOwnershipService paymentOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional
    public CuentaPagoResponse create(CuentaPagoRequest request, Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        validateDuplicate(codper, request, null);
        try {
            return cuentaPagoMapper.toResponse(cuentaPagoRepository.saveAndFlush(
                    cuentaPagoMapper.toEntity(request, propertyOwnershipService.currentPropietaria(authentication))));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCuenta();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CuentaPagoResponse> list(Short estado, Pageable pageable, Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<CuentaPagoResponse> page = cuentaPagoRepository.searchOwned(codper, estado, pageable)
                .map(cuentaPagoMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPagoResponse get(Integer codcta, Authentication authentication) {
        return cuentaPagoMapper.toResponse(paymentOwnershipService.findOwnedCuentaPago(codcta, authentication));
    }

    @Override
    @Transactional
    public CuentaPagoResponse update(Integer codcta, CuentaPagoRequest request, Authentication authentication) {
        CuentaPagoEntity cuenta = paymentOwnershipService.findOwnedCuentaPago(codcta, authentication);
        validateDuplicate(cuenta.getPropietaria().getCodper(), request, codcta);
        cuentaPagoMapper.update(cuenta, request);
        try {
            return cuentaPagoMapper.toResponse(cuentaPagoRepository.saveAndFlush(cuenta));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCuenta();
        }
    }

    @Override
    @Transactional
    public CuentaPagoResponse activate(Integer codcta, Authentication authentication) {
        CuentaPagoEntity cuenta = paymentOwnershipService.findOwnedCuentaPago(codcta, authentication);
        if (!Objects.equals(cuenta.getEstado(), (short) 1)) {
            cuenta.setEstado((short) 1);
        }
        return cuentaPagoMapper.toResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaPagoResponse deactivate(Integer codcta, Authentication authentication) {
        CuentaPagoEntity cuenta = paymentOwnershipService.findOwnedCuentaPago(codcta, authentication);
        if (!Objects.equals(cuenta.getEstado(), (short) 0)) {
            cuenta.setEstado((short) 0);
        }
        return cuentaPagoMapper.toResponse(cuenta);
    }

    private void validateDuplicate(Integer codper, CuentaPagoRequest request, Integer excludeCodcta) {
        if (cuentaPagoRepository.existsDuplicate(codper, request.banco().trim(), request.numeroCuenta().trim(), excludeCodcta)) {
            throw duplicateCuenta();
        }
    }

    private ConflictException duplicateCuenta() {
        return new ConflictException("Ya existe una CuentaPago con ese banco y número para la Propietaria.");
    }
}
