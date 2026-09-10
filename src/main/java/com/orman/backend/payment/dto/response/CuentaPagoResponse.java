package com.orman.backend.payment.dto.response;

public record CuentaPagoResponse(Integer codcta, Integer codperPropietaria, String banco, String numeroCuenta,
                                 String titular, String qrUrl, String instrucciones, Integer orden, Short estado) {
}
