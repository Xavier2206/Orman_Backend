package com.orman.backend.contract.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.request.CuotaListCriteria;
import com.orman.backend.contract.dto.response.CuotaListItemResponse;
import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface CuotaService {

    void generatePending(ContratoEntity contrato);

    List<CuotaResponse> listByContrato(Integer codcon, Authentication authentication);

    PageResponse<CuotaListItemResponse> listGlobal(CuotaListCriteria criteria, Authentication authentication);
}
