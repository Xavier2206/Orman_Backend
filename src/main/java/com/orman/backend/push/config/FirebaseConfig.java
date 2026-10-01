package com.orman.backend.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final String DEFAULT_APP_NAME = "[DEFAULT]";

    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "orman.firebase", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        String projectId = properties.getProjectId() == null ? "" : properties.getProjectId().trim();
        if (projectId.isEmpty()) {
            throw new IllegalStateException("Firebase project ID is not configured");
        }

        synchronized (FirebaseConfig.class) {
            return FirebaseApp.getApps().stream()
                    .filter(app -> DEFAULT_APP_NAME.equals(app.getName()))
                    .findFirst()
                    .orElseGet(() -> initializeApp(projectId));
        }
    }

    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "orman.firebase", name = "enabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private FirebaseApp initializeApp(String projectId) {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Application Default Credentials are unavailable");
        }
    }
}
