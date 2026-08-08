package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.request.OtpVerifyRequest;
import com.orman.backend.auth.dto.request.OtpResendRequest;
import com.orman.backend.auth.model.ClientType;

public interface AuthService {

    AuthResult login(LoginRequest request);

    AuthResult verifyOtp(OtpVerifyRequest request);

    void resendOtp(OtpResendRequest request);

    AuthResult refresh(String refreshToken, ClientType sourceClientType);
}
