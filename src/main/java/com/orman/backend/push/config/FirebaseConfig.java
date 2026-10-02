package com.orman.backend.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final String DEFAULT_APP_NAME = "[DEFAULT]";

    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "orman.firebase", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(FirebaseProperties properties, Environment environment) {
        String projectId = properties.getProjectId() == null ? "" : properties.getProjectId().trim();
        if (projectId.isEmpty()) {
            throw new IllegalStateException("Firebase project ID is not configured");
        }

        synchronized (FirebaseConfig.class) {
            return FirebaseApp.getApps().stream()
                    .filter(app -> DEFAULT_APP_NAME.equals(app.getName()))
                    .findFirst()
                    .orElseGet(() -> initializeApp(projectId, properties, environment));
        }
    }

    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "orman.firebase", name = "enabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private FirebaseApp initializeApp(String projectId, FirebaseProperties properties, Environment environment) {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(resolveCredentials(properties, environment))
                .setProjectId(projectId)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials resolveCredentials(FirebaseProperties properties, Environment environment) {
        String serviceAccountJson = properties.getServiceAccountJson();
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            if (environment.containsProperty("ORMAN_FIREBASE_SERVICE_ACCOUNT_JSON")) {
                throw new IllegalStateException(
                        "Firebase credentials could not be initialized from environment configuration.");
            }
            try {
                return GoogleCredentials.getApplicationDefault();
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Firebase Application Default Credentials are unavailable.");
            }
        }

        try {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Firebase credentials could not be initialized from environment configuration.");
        }
    }
}
