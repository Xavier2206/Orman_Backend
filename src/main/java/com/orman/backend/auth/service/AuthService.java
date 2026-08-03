package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.model.ClientType;

public interface AuthService {

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken, ClientType sourceClientType);
}
