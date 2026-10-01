package com.orman.backend.common.file;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Private Cloudflare R2 access through its S3-compatible API. */
public final class R2FileStorageService implements FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(R2FileStorageService.class);
    private final S3Client client;
    private final String bucket;

    public R2FileStorageService(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream content, long contentLength, String contentType) {
        String safeKey = StorageKeys.validate(key);
        try (InputStream input = content) {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(safeKey)
                            .contentType(contentType).contentLength(contentLength).build(),
                    RequestBody.fromInputStream(input, contentLength));
        } catch (RuntimeException exception) {
            LOGGER.error("Falló almacenamiento R2, operación=put, key={}, error={}", safeKey,
                    exception.getClass().getSimpleName());
            throw new StorageException("No fue posible almacenar el objeto.", exception);
        } catch (java.io.IOException exception) {
            LOGGER.error("Falló almacenamiento R2, operación=put, key={}, error={}", safeKey,
                    exception.getClass().getSimpleName());
            throw new StorageException("No fue posible almacenar el objeto.", exception);
        }
    }

    @Override
    public StoredObject get(String key) {
        String safeKey = StorageKeys.validate(key);
        try {
            ResponseInputStream<GetObjectResponse> stream = client.getObject(GetObjectRequest.builder()
                    .bucket(bucket).key(safeKey).build());
            long contentLength = stream.response().contentLength() == null ? 0L : stream.response().contentLength();
            return new StoredObject(stream, contentLength);
        } catch (NoSuchKeyException exception) {
            throw new StorageObjectNotFoundException();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404 || "NoSuchKey".equals(exception.awsErrorDetails() == null
                    ? null : exception.awsErrorDetails().errorCode())) {
                throw new StorageObjectNotFoundException();
            }
            throw failed("get", safeKey, exception);
        } catch (RuntimeException exception) {
            throw failed("get", safeKey, exception);
        }
    }

    @Override
    public void delete(String key) {
        String safeKey = StorageKeys.validate(key);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(safeKey).build());
        } catch (RuntimeException exception) {
            throw failed("delete", safeKey, exception);
        }
    }

    @Override
    public boolean exists(String key) {
        String safeKey = StorageKeys.validate(key);
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(safeKey).build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404 || "NotFound".equals(exception.awsErrorDetails() == null
                    ? null : exception.awsErrorDetails().errorCode())) {
                return false;
            }
            throw failed("head", safeKey, exception);
        } catch (RuntimeException exception) {
            throw failed("head", safeKey, exception);
        }
    }

    private StorageException failed(String operation, String key, RuntimeException exception) {
        LOGGER.error("Falló almacenamiento R2, operación={}, key={}, error={}", operation, key,
                exception.getClass().getSimpleName());
        return new StorageException("No fue posible acceder al objeto almacenado.", exception);
    }
}
