package com.orman.backend.contract.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.mapper.ContratoArchivoMapper;
import com.orman.backend.contract.repository.ContratoArchivoRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.ContratoArchivoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoArchivoServiceImpl implements ContratoArchivoService {

    private final ContratoArchivoRepository contratoArchivoRepository;
    private final ContratoArchivoMapper contratoArchivoMapper;
    private final ContractOwnershipService contractOwnershipService;

    @Override
    @Transactional
    public ContratoArchivoResponse create(Integer codcon, ContratoArchivoRequest request, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContrato(codcon, authentication);
        if (contratoArchivoRepository.existsByContratoCodconAndOrden(codcon, request.orden())) {
            throw duplicateOrden();
        }
        try {
            return contratoArchivoMapper.toResponse(contratoArchivoRepository.saveAndFlush(
                    contratoArchivoMapper.toEntity(request, contrato)));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoArchivoResponse> listByContrato(Integer codcon, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        return contratoArchivoRepository.findAllByContratoCodconOrderByOrdenAscIdAsc(codcon).stream()
                .map(contratoArchivoMapper::toResponse).toList();
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden del archivo ya existe en este Contrato.");
    }
}
