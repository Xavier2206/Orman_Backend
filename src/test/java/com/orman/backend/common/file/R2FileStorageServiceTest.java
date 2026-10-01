package com.orman.backend.common.file;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class R2FileStorageServiceTest {

    private final S3Client client = mock(S3Client.class);
    private final R2FileStorageService storage = new R2FileStorageService(client, "private-bucket");

    @Test
    void r2ProviderRequiresExplicitConfiguration() {
        assertThatThrownBy(() -> new StorageConfiguration().r2S3Client(new R2StorageProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ORMAN_STORAGE_PROVIDER=r2 requiere ORMAN_R2_ENDPOINT.");
    }

    @Test
    void storesReadsAndDeletesThroughS3Client() throws Exception {
        byte[] bytes = "private-object".getBytes(StandardCharsets.UTF_8);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) bytes.length).build(),
                new ByteArrayInputStream(bytes)));

        storage.put("personas/14/photo.jpg", new ByteArrayInputStream(bytes), bytes.length, "image/jpeg");
        StoredObject object = storage.get("personas/14/photo.jpg");
        assertThat(object.contentLength()).isEqualTo(bytes.length);
        assertThat(object.content().readAllBytes()).containsExactly(bytes);
        storage.delete("personas/14/photo.jpg");

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void wrapsClientFailuresWithoutExposingProviderMessages() {
        when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder()
                .statusCode(500).message("private endpoint response").build());

        assertThatThrownBy(() -> storage.delete("personas/14/photo.jpg"))
                .isInstanceOf(StorageException.class)
                .hasMessage("No fue posible acceder al objeto almacenado.")
                .hasMessageNotContaining("private endpoint response");
    }
}
