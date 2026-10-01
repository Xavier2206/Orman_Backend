package com.orman.backend.contract.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.dto.response.InquilinoContratoResponse;
import com.orman.backend.contract.dto.response.InquilinoCuotaResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.mapper.InquilinoPortalMapper;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.repository.InquilinoCuotaProjection;
import com.orman.backend.contract.service.InquilinoPortalService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquilinoPortalServiceImpl implements InquilinoPortalService {

    private static final int UPCOMING_DAYS = 7;

    private final ContratoRepository contratoRepository;
    private final CuotaRepository cuotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaymentOwnershipService paymentOwnershipService;
    private final InquilinoPortalMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InquilinoContratoResponse> listContracts(Pageable pageable,
                                                                  Authentication authentication) {
        Usuario tenant = authenticatedTenant(authentication);
        Page<InquilinoContratoResponse> page = contratoRepository.findAllForTenant(
                        tenant.getPersona().getCodper(), pageable)
                .map(mapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public InquilinoContratoResponse getContract(Integer codcon, Authentication authentication) {
        Usuario tenant = authenticatedTenant(authentication);
        ContratoEntity contrato = contratoRepository.findByCodconAndInquilinoCodper(
                        codcon, tenant.getPersona().getCodper())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado."));
        return mapper.toResponse(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InquilinoCuotaResponse> listQuotas(Integer codcon, Authentication authentication) {
        Usuario tenant = authenticatedTenant(authentication);
        Integer codper = tenant.getPersona().getCodper();
        if (contratoRepository.findByCodconAndInquilinoCodper(codcon, codper).isEmpty()) {
            throw new ResourceNotFoundException("Contrato no encontrado.");
        }
        LocalDate today = today();
        return cuotaRepository.findTenantQuotaSummaries(codper, codcon, null).stream()
                .map(row -> toQuotaResponse(row, today))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InquilinoCuotaResponse getQuota(Integer codcuo, Authentication authentication) {
        Usuario tenant = authenticatedTenant(authentication);
        paymentOwnershipService.findAccessibleCuota(codcuo, authentication);
        InquilinoCuotaProjection row = cuotaRepository.findTenantQuotaSummaries(
                        tenant.getPersona().getCodper(), null, codcuo)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada."));
        return toQuotaResponse(row, today());
    }

    private Usuario authenticatedTenant(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> "ROLE_INQUILINO".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("No tiene autorizaciÃ³n para consultar recursos de inquilino.");
        }
        Usuario tenant = usuarioRepository.findByLoginWithPersona(authenticatedUser.login())
                .orElseThrow(() -> new AccessDeniedException("El Usuario autenticado no existe."));
        if (tenant.getPersona() == null || !Character.valueOf('I').equals(tenant.getPersona().getTipoPersona())) {
            throw new AccessDeniedException("El Usuario autenticado no corresponde a un Inquilino.");
        }
        return tenant;
    }

    private InquilinoCuotaResponse toQuotaResponse(InquilinoCuotaProjection row, LocalDate today) {
        String situation;
        if (row.getSaldo().signum() <= 0) {
            situation = "SIN_SALDO";
        } else if (row.getFechaVencimiento().isBefore(today)) {
            situation = "VENCIDA";
        } else if (row.getFechaVencimiento().isEqual(today)) {
            situation = "HOY";
        } else if (!row.getFechaVencimiento().isAfter(today.plusDays(UPCOMING_DAYS))) {
            situation = "PROXIMA";
        } else {
            situation = "AL_DIA";
        }
        return new InquilinoCuotaResponse(row.getCodcuo(), row.getCodcon(), row.getPeriodo(),
                row.getFechaVencimiento(), row.getMonto(), row.getMontoConfirmado(),
                row.getMontoPendienteRevision(), row.getSaldo(), row.getEstado(), situation);
    }

    private LocalDate today() {
        return OrmanTimeConfig.today(clock);
    }
}
