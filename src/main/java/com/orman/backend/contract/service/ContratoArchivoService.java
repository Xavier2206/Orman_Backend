package com.orman.backend.contract.service;

import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface ContratoArchivoService {

    ContratoArchivoResponse create(Integer codcon, ContratoArchivoRequest request, Authentication authentication);

    List<ContratoArchivoResponse> listByContrato(Integer codcon, Authentication authentication);
}
