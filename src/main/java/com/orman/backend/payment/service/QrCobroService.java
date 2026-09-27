package com.orman.backend.payment.service;

import com.orman.backend.payment.dto.request.QrCobroEstadoRequest;
import com.orman.backend.payment.dto.request.QrCobroRequest;
import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.entity.QrCobroEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface QrCobroService {

    QrCobroResponse create(QrCobroRequest request, MultipartFile imagen, Authentication authentication);

    List<QrCobroResponse> list(Authentication authentication);

    QrCobroResponse get(Integer codqr, Authentication authentication);

    QrCobroResponse current(Authentication authentication);

    QrCobroResponse currentForQuota(Integer codcuo, Authentication authentication);

    PaymentImageContent image(Integer codqr, Authentication authentication);

    PaymentImageContent imageForQuota(Integer codcuo, Authentication authentication);

    QrCobroResponse changeState(Integer codqr, QrCobroEstadoRequest request, Authentication authentication);

    QrCobroEntity resolveForPayment(Integer codperPropietaria, LocalDate today, LocalDate fechaPago);
}
