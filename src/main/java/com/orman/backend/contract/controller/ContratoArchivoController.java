package com.orman.backend.contract.controller;

import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.service.ContratoArchivoContent;
import com.orman.backend.contract.service.ContratoArchivoService;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/contratos/{codcon}/archivos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PROPIETARIO')")
public class ContratoArchivoController {

    private final ContratoArchivoService contratoArchivoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContratoArchivoResponse> create(@PathVariable Integer codcon,
                                                           @RequestPart("archivo") MultipartFile archivo,
                                                           @RequestParam("orden") @Min(0) Integer orden,
                                                           Authentication authentication) {
        ContratoArchivoResponse response = contratoArchivoService.create(codcon, archivo, orden, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codarc}/download")
                .buildAndExpand(response.codarc()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ContratoArchivoResponse> listByContrato(@PathVariable Integer codcon, Authentication authentication) {
        return contratoArchivoService.listByContrato(codcon, authentication);
    }

    @GetMapping("/{codarc}/download")
    public ResponseEntity<Resource> download(@PathVariable Integer codcon, @PathVariable Integer codarc,
                                              Authentication authentication) {
        ContratoArchivoContent content = contratoArchivoService.download(codcon, codarc, authentication);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.nombreArchivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.tamano())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }

    @DeleteMapping("/{codarc}")
    public ResponseEntity<Void> delete(@PathVariable Integer codcon, @PathVariable Integer codarc,
                                       Authentication authentication) {
        contratoArchivoService.delete(codcon, codarc, authentication);
        return ResponseEntity.noContent().build();
    }
}
