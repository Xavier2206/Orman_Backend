package com.orman.backend.common.file;

import com.orman.backend.contract.config.ContratoArchivoProperties;
import com.orman.backend.payment.config.PaymentImageStorageProperties;
import com.orman.backend.person.config.PersonaPhotoProperties;
import com.orman.backend.property.config.PropiedadPortadaProperties;
import com.orman.backend.property.config.UnidadFotoProperties;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class StorageConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "orman.storage", name = "provider", havingValue = "local", matchIfMissing = true)
    FileStorageService localFileStorageService(PersonaPhotoProperties personas,
                                               PropiedadPortadaProperties propiedades,
                                               UnidadFotoProperties unidades,
                                               PaymentImageStorageProperties pagos,
                                               ContratoArchivoProperties contratos) {
        LOGGER.info("Proveedor de almacenamiento seleccionado: local");
        return new LocalFileStorageService(Map.of(
                "personas", personas.root(),
                "propiedades", propiedades.root(),
                "unidades", unidades.root(),
                "comprobantes", pagos.root(),
                "qr-cobro", pagos.root(),
                "contratos", contratos.root()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "orman.storage", name = "provider", havingValue = "r2")
    S3Client r2S3Client(R2StorageProperties properties) {
        String endpoint = required(properties.getEndpoint(), "ORMAN_R2_ENDPOINT");
        String bucket = required(properties.getBucket(), "ORMAN_R2_BUCKET");
        String accessKeyId = required(properties.getAccessKeyId(), "ORMAN_R2_ACCESS_KEY_ID");
        String secretAccessKey = required(properties.getSecretAccessKey(), "ORMAN_R2_SECRET_ACCESS_KEY");
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null
                    || endpointUri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ORMAN_STORAGE_PROVIDER=r2 requiere ORMAN_R2_ENDPOINT HTTPS válido.");
        }
        if (bucket.isBlank()) {
            throw new IllegalStateException("ORMAN_STORAGE_PROVIDER=r2 requiere ORMAN_R2_BUCKET.");
        }
        return S3Client.builder()
                .endpointOverride(endpointUri)
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "orman.storage", name = "provider", havingValue = "r2")
    FileStorageService r2FileStorageService(S3Client r2S3Client, R2StorageProperties properties) {
        LOGGER.info("Proveedor de almacenamiento seleccionado: r2");
        return new R2FileStorageService(r2S3Client, properties.getBucket());
    }

    @Bean
    StorageProviderConfigurationCheck storageProviderConfigurationCheck(
            org.springframework.core.env.Environment environment) {
        String provider = environment.getProperty("orman.storage.provider", "local").toLowerCase(Locale.ROOT);
        if (!provider.equals("local") && !provider.equals("r2")) {
            throw new IllegalStateException("ORMAN_STORAGE_PROVIDER debe ser local o r2.");
        }
        return new StorageProviderConfigurationCheck();
    }

    private String required(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("ORMAN_STORAGE_PROVIDER=r2 requiere " + variable + ".");
        }
        return value;
    }

    static final class StorageProviderConfigurationCheck {
    }
}
