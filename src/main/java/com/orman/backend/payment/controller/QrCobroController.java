package com.orman.backend.payment.controller;

import com.orman.backend.payment.dto.request.QrCobroEstadoRequest;
import com.orman.backend.payment.dto.request.QrCobroRequest;
import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.service.PaymentImageContent;
import com.orman.backend.payment.service.QrCobroService;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/qr-cobro")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROPIETARIO')")
public class QrCobroController {

    private final QrCobroService qrCobroService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QrCobroResponse> create(@Valid @RequestPart("qr") QrCobroRequest request,
                                                   @RequestPart("imagen") MultipartFile imagen,
                                                   Authentication authentication) {
        QrCobroResponse response = qrCobroService.create(request, imagen, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{codqr}")
                .buildAndExpand(response.codqr()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<QrCobroResponse> list(Authentication authentication) {
        return qrCobroService.list(authentication);
    }

    @GetMapping("/vigente")
    public QrCobroResponse current(Authentication authentication) {
        return qrCobroService.current(authentication);
    }

    @GetMapping("/{codqr}")
    public QrCobroResponse get(@PathVariable Integer codqr, Authentication authentication) {
        return qrCobroService.get(codqr, authentication);
    }

    @GetMapping("/{codqr}/imagen")
    public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable Integer codqr,
                                                                       Authentication authentication) {
        return imageResponse(qrCobroService.image(codqr, authentication));
    }

    @PatchMapping("/{codqr}/estado")
    public QrCobroResponse changeState(@PathVariable Integer codqr,
                                       @Valid @RequestBody QrCobroEstadoRequest request,
                                       Authentication authentication) {
        return qrCobroService.changeState(codqr, request, authentication);
    }

    static ResponseEntity<org.springframework.core.io.Resource> imageResponse(PaymentImageContent content) {
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.nombreArchivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.tipoContenido()))
                .contentLength(content.tamano())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
