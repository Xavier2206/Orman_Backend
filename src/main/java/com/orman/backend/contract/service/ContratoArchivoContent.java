package com.orman.backend.contract.service;

import org.springframework.core.io.Resource;

public record ContratoArchivoContent(Resource resource, String nombreArchivo, long tamano) {
}
