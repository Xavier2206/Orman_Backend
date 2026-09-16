package com.orman.backend.contract.service;

import com.orman.backend.contract.service.ContratoArchivoContent;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface ContratoArchivoService {

    ContratoArchivoResponse create(Integer codcon, MultipartFile archivo, Integer orden,
                                   Authentication authentication);

    List<ContratoArchivoResponse> listByContrato(Integer codcon, Authentication authentication);

    ContratoArchivoContent download(Integer codcon, Integer codarc, Authentication authentication);

    void delete(Integer codcon, Integer codarc, Authentication authentication);
}
