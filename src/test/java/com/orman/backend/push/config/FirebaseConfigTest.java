package com.orman.backend.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class FirebaseConfigTest {

    private static final String CREDENTIALS_ENVIRONMENT_PROPERTY = "ORMAN_FIREBASE_SERVICE_ACCOUNT_JSON";
    private static final String SAFE_ERROR_MESSAGE =
            "Firebase credentials could not be initialized from environment configuration.";

    @TempDir
    Path temporaryDirectory;

    private FirebaseApp createdApp;
    private String originalJavaTempDirectory;

    @AfterEach
    void cleanUp() {
        if (createdApp != null) {
            createdApp.delete();
            createdApp = null;
        }
        if (originalJavaTempDirectory != null) {
            System.setProperty("java.io.tmpdir", originalJavaTempDirectory);
            originalJavaTempDirectory = null;
        }
    }

    @Test
    void disabledFirebaseDoesNotRegisterOrInitializeFirebaseBeans() {
        int existingApps = FirebaseApp.getApps().size();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("orman.firebase.enabled", "false")));
            context.register(FirebaseConfig.class);
            context.refresh();

            assertThat(context.getBeansOfType(FirebaseApp.class)).isEmpty();
            assertThat(context.getBeansOfType(FirebaseMessaging.class)).isEmpty();
            assertThat(FirebaseApp.getApps()).hasSize(existingApps);
        }
    }

    @Test
    void validEnvironmentJsonInitializesFirebaseWithoutFallbackOrTemporaryFiles() throws Exception {
        String serviceAccountJson = simulatedServiceAccountJson();
        FirebaseProperties properties = enabledProperties(serviceAccountJson);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(CREDENTIALS_ENVIRONMENT_PROPERTY, serviceAccountJson);
        FirebaseConfig config = new FirebaseConfig();
        originalJavaTempDirectory = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", temporaryDirectory.toString());
        GoogleCredentials parsedCredentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
        AtomicBoolean receivedByteArrayInputStream = new AtomicBoolean();
        AtomicReference<byte[]> receivedBytes = new AtomicReference<>();

        try (MockedStatic<GoogleCredentials> credentials = mockStatic(GoogleCredentials.class)) {
            credentials.when(() -> GoogleCredentials.fromStream(org.mockito.ArgumentMatchers.any(InputStream.class)))
                    .thenAnswer(invocation -> {
                        InputStream stream = invocation.getArgument(0);
                        receivedByteArrayInputStream.set(stream instanceof ByteArrayInputStream);
                        receivedBytes.set(stream.readAllBytes());
                        return parsedCredentials;
                    });
            credentials.when(GoogleCredentials::getApplicationDefault)
                    .thenThrow(new IOException("fallback must not be used"));

            createdApp = config.firebaseApp(properties, environment);
            FirebaseMessaging messaging = config.firebaseMessaging(createdApp);

            assertThat(createdApp.getOptions().getProjectId()).isEqualTo("firebase-config-test");
            assertThat(messaging).isSameAs(FirebaseMessaging.getInstance(createdApp));
            assertThat(receivedByteArrayInputStream).isTrue();
            assertThat(java.util.Arrays.equals(serviceAccountJson.getBytes(StandardCharsets.UTF_8),
                    receivedBytes.get())).isTrue();
            credentials.verify(GoogleCredentials::getApplicationDefault, never());
            credentials.verify(() -> GoogleCredentials.fromStream(org.mockito.ArgumentMatchers.any()), times(1));
        }

        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void absentJsonUsesApplicationDefaultCredentials() throws Exception {
        FirebaseProperties properties = enabledProperties(null);
        MockEnvironment environment = new MockEnvironment();
        FirebaseConfig config = new FirebaseConfig();
        GoogleCredentials applicationDefaultCredentials = mock(GoogleCredentials.class);

        try (MockedStatic<GoogleCredentials> credentials = mockStatic(GoogleCredentials.class)) {
            credentials.when(GoogleCredentials::getApplicationDefault).thenReturn(applicationDefaultCredentials);

            createdApp = config.firebaseApp(properties, environment);

            assertThat(createdApp.getOptions().getProjectId()).isEqualTo("firebase-config-test");
            credentials.verify(GoogleCredentials::getApplicationDefault, times(1));
            credentials.verify(() -> GoogleCredentials.fromStream(org.mockito.ArgumentMatchers.any()), never());
        }
    }

    @Test
    void emptyConfiguredJsonFailsWithoutTryingApplicationDefaultCredentials() {
        FirebaseProperties properties = enabledProperties("");
        MockEnvironment environment = new MockEnvironment()
                .withProperty(CREDENTIALS_ENVIRONMENT_PROPERTY, "");

        try (MockedStatic<GoogleCredentials> credentials = mockStatic(GoogleCredentials.class)) {
            assertThatThrownBy(() -> new FirebaseConfig().firebaseApp(properties, environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAFE_ERROR_MESSAGE)
                    .hasNoCause();

            credentials.verify(GoogleCredentials::getApplicationDefault, never());
            credentials.verify(() -> GoogleCredentials.fromStream(org.mockito.ArgumentMatchers.any()), never());
        }
    }

    @Test
    void invalidJsonFailsWithSanitizedMessageAndNoCause() {
        String invalidJson = "INVALID_FAKE_CREDENTIAL_MARKER";
        FirebaseProperties properties = enabledProperties(invalidJson);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(CREDENTIALS_ENVIRONMENT_PROPERTY, invalidJson);

        assertThatThrownBy(() -> new FirebaseConfig().firebaseApp(properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(SAFE_ERROR_MESSAGE)
                .hasMessageNotContaining(invalidJson)
                .hasNoCause();
    }

    private FirebaseProperties enabledProperties(String serviceAccountJson) {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(true);
        properties.setProjectId("firebase-config-test");
        properties.setServiceAccountJson(serviceAccountJson);
        return properties;
    }

    private String simulatedServiceAccountJson() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        byte[] encodedPrivateKey = keyPairGenerator.generateKeyPair().getPrivate().getEncoded();
        String pemPrivateKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encodedPrivateKey)
                + "\n-----END PRIVATE KEY-----\n";
        String escapedPrivateKey = pemPrivateKey.replace("\n", "\\n");

        return """
                {
                  "type": "service_account",
                  "project_id": "firebase-config-test",
                  "private_key_id": "generated-test-key-id",
                  "private_key": "%s",
                  "client_email": "firebase-test@firebase-config-test.iam.gserviceaccount.com",
                  "client_id": "1234567890",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-test"
                }
                """.formatted(escapedPrivateKey);
    }
}
