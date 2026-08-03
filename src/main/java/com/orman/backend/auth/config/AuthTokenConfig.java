package com.orman.backend.auth.config;

import java.security.SecureRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, RefreshCookieProperties.class})
public class AuthTokenConfig {

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
