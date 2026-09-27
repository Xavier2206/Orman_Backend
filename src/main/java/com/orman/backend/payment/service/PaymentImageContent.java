package com.orman.backend.payment.service;

import org.springframework.core.io.Resource;

public record PaymentImageContent(Resource resource, String nombreArchivo, String tipoContenido, long tamano) {
}
