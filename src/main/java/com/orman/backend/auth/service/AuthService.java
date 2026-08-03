package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
