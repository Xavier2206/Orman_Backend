package com.orman.backend.auth.service;

public interface OtpMailService {

    void sendOtp(String destination, String otp, long expirationSeconds);
}
