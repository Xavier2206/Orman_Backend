package com.orman.backend.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.mail")
public record MailProperties(@NotBlank @Email String from) {

    @Override public String toString() { return "MailProperties[from=<redacted>]"; }
}
