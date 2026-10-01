package com.orman.backend.payment.service;

import com.orman.backend.common.file.FileStorageService;
import com.orman.backend.common.file.StoredObject;
import com.orman.backend.payment.config.PaymentImageStorageProperties;
import com.orman.backend.payment.exception.InvalidPaymentImageException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentImageStorageServiceTest {

    @TempDir Path tempDir;
    private PaymentImageStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new PaymentImageStorageService(new PaymentImageStorageProperties(tempDir.toString(),
                5 * 1024 * 1024, 1920),
                new com.orman.backend.common.file.LocalFileStorageService(java.util.Map.of(
                        "qr-cobro", tempDir.toString(), "comprobantes", tempDir.toString())));
    }

    @Test
    void storesAndLoadsPngAndJpegAsPrivateServerReferences() throws Exception {
        StoredPaymentImage png = storage.storeQr(image("qr.png", "image/png", "png", 64, 64), 12);
        StoredPaymentImage qrJpeg = storage.storeQr(image("qr.jpeg", "image/jpeg", "jpeg", 80, 60), 13);
        StoredPaymentImage proofJpeg = storage.storeProof(image("proof.jpeg", "image/jpeg", "jpeg", 80, 60), 44);

        assertThat(png.rutaArchivo()).matches("qr-cobro/12/[0-9a-f-]{36}\\.png");
        assertThat(qrJpeg.rutaArchivo()).matches("qr-cobro/13/[0-9a-f-]{36}\\.jpg");
        assertThat(proofJpeg.rutaArchivo()).matches("comprobantes/44/[0-9a-f-]{36}\\.jpg");
        assertThat(storage.loadQr(png.rutaArchivo(), 12).tipoContenido()).isEqualTo("image/png");
        assertThat(storage.loadQr(qrJpeg.rutaArchivo(), 13).tipoContenido()).isEqualTo("image/jpeg");
        assertThat(storage.loadProof(proofJpeg.rutaArchivo(), 44).tipoContenido()).isEqualTo("image/jpeg");
        assertThat(Files.exists(tempDir.resolve(png.rutaArchivo()))).isTrue();
        assertThat(storage.loadQr(png.rutaArchivo(), 12).resource().getInputStream().readAllBytes()).isNotEmpty();
    }

    @Test
    void sendsNormalizedQrAndProofBytesWithCorrectKeysTypesAndSizes() throws Exception {
        RecordingFileStorageService fileStorage = new RecordingFileStorageService();
        PaymentImageStorageService byteStorage = new PaymentImageStorageService(
                new PaymentImageStorageProperties(tempDir.toString(), 5 * 1024 * 1024, 1920), fileStorage);
        MockMultipartFile qrPngFile = image("qr.png", "image/png", "png", 64, 64);
        MockMultipartFile qrJpegFile = image("qr.jpeg", "image/jpeg", "jpeg", 80, 60);
        MockMultipartFile proofJpegFile = image("proof.jpeg", "image/jpeg", "jpeg", 80, 60);

        StoredPaymentImage qrPng = byteStorage.storeQr(qrPngFile, 12);
        StoredPaymentImage qrJpeg = byteStorage.storeQr(qrJpegFile, 13);
        StoredPaymentImage proofJpeg = byteStorage.storeProof(proofJpegFile, 44);

        assertStoredUpload(fileStorage.uploads.get(0), qrPng, qrPngFile, "qr-cobro/12/", "png", "image/png", true);
        assertStoredUpload(fileStorage.uploads.get(1), qrJpeg, qrJpegFile, "qr-cobro/13/", "jpg", "image/jpeg", true);
        assertStoredUpload(fileStorage.uploads.get(2), proofJpeg, proofJpegFile, "comprobantes/44/", "jpg",
                "image/jpeg", false);
        assertThat(fileStorage.uploads).hasSize(3);
        assertThat(fileStorage.inputStreamPutCalls).isZero();
    }

    @Test
    void rejectsCorruptImagesFalseMimeAndUploadsAboveFiveMegabytes() {
        assertThatThrownBy(() -> storage.storeProof(new MockMultipartFile("comprobante", "proof.png",
                "image/png", "not an image".getBytes()), 1)).isInstanceOf(InvalidPaymentImageException.class);
        try (var files = Files.list(tempDir)) {
            assertThat(files).isEmpty();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        assertThatThrownBy(() -> storage.storeProof(image("proof.png", "image/jpeg", "png", 40, 40), 1))
                .isInstanceOf(InvalidPaymentImageException.class);
        assertThatThrownBy(() -> storage.storeProof(new MockMultipartFile("comprobante", "proof.png",
                "image/png", new byte[5 * 1024 * 1024 + 1]), 1))
                .isInstanceOf(InvalidPaymentImageException.class).hasMessageContaining("5 MB");
    }

    @Test
    void normalizesOversizedImagesWithoutExceedingConfiguredDimension() throws Exception {
        StoredPaymentImage stored = storage.storeProof(image("proof.png", "image/png", "png", 2300, 100), 19);
        BufferedImage normalized = ImageIO.read(storage.loadProof(stored.rutaArchivo(), 19)
                .resource().getInputStream());
        assertThat(normalized.getWidth()).isLessThanOrEqualTo(1920);
        assertThat(normalized.getHeight()).isLessThanOrEqualTo(1920);
        assertThat(normalized.getWidth()).isEqualTo(1920);
    }

    @Test
    void rejectsPathTraversalWhenLoadingPrivateImage() {
        assertThatThrownBy(() -> storage.loadProof("comprobantes/1/../../secret.png", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MockMultipartFile image(String filename, String contentType, String format, int width, int height)
            throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);
            for (int y = 0; y < height; y += 10) {
                graphics.fillRect(0, y, width / 2, 5);
            }
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return new MockMultipartFile("imagen", filename, contentType, output.toByteArray());
    }

    private void assertStoredUpload(StoredUpload upload, StoredPaymentImage stored, MockMultipartFile source,
                                    String keyPrefix, String extension, String contentType, boolean qrImage)
            throws IOException {
        assertThat(stored.rutaArchivo()).matches(keyPrefix.replace("/", "\\/")
                + "[0-9a-f-]{36}\\." + extension);
        assertThat(upload.key).isEqualTo(stored.rutaArchivo());
        assertThat(upload.contentType).isEqualTo(contentType);
        byte[] expected = normalizedBytes(source, extension.equals("png") ? "png" : "jpeg", qrImage);
        assertThat(upload.content).containsExactly(expected);
        assertThat(upload.content).hasSize(expected.length).isNotEmpty().hasSizeLessThan(5 * 1024 * 1024);
    }

    private byte[] normalizedBytes(MockMultipartFile source, String format, boolean qrImage) throws IOException {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(source.getBytes()));
        BufferedImage normalized = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, qrImage
                    ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                    : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(decoded, 0, 0, normalized.getWidth(), normalized.getHeight(), null);
        } finally {
            graphics.dispose();
        }

        var writers = ImageIO.getImageWritersByFormatName(format);
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (format.equals("jpeg") && parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.95f);
            }
            writer.write(null, new IIOImage(normalized, null, null), parameters);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private record StoredUpload(String key, byte[] content, String contentType) {
    }

    private static final class RecordingFileStorageService implements FileStorageService {
        private final List<StoredUpload> uploads = new ArrayList<>();
        private final Map<String, byte[]> contentByKey = new HashMap<>();
        private int inputStreamPutCalls;

        @Override
        public void put(String key, InputStream content, long contentLength, String contentType) {
            inputStreamPutCalls++;
            try {
                put(key, content.readAllBytes(), contentType);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public void put(String key, byte[] content, String contentType) {
            byte[] copy = content.clone();
            uploads.add(new StoredUpload(key, copy, contentType));
            contentByKey.put(key, copy);
        }

        @Override
        public StoredObject get(String key) {
            byte[] content = contentByKey.get(key);
            return new StoredObject(new ByteArrayInputStream(content), content.length);
        }

        @Override
        public void delete(String key) {
            contentByKey.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return contentByKey.containsKey(key);
        }
    }
}
