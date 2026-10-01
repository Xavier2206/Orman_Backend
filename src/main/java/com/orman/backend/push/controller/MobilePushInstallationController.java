package com.orman.backend.push.controller;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.push.dto.PushInstallationRequest;
import com.orman.backend.push.service.PushInstallationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/push-installation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MobilePushInstallationController {

    private final PushInstallationService pushInstallationService;

    @PutMapping
    public ResponseEntity<Void> register(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PushInstallationRequest request) {
        pushInstallationService.register(user, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser user) {
        pushInstallationService.deactivate(user);
        return ResponseEntity.noContent().build();
    }
}
