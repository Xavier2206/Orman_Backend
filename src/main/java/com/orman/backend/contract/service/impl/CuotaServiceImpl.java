package com.orman.backend.contract.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.mapper.CuotaMapper;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.CuotaService;
import java.time.LocalDate;
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
                .map(cuotaMapper::toResponse).toList();
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
