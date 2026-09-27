package com.orman.backend.contract.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.response.InquilinoContratoResponse;
import com.orman.backend.contract.dto.response.InquilinoCuotaResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface InquilinoPortalService {

    PageResponse<InquilinoContratoResponse> listContracts(Pageable pageable, Authentication authentication);

    InquilinoContratoResponse getContract(Integer codcon, Authentication authentication);

    List<InquilinoCuotaResponse> listQuotas(Integer codcon, Authentication authentication);

    InquilinoCuotaResponse getQuota(Integer codcuo, Authentication authentication);
}
