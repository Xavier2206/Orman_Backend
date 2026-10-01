package com.orman.backend.payment.service;

import com.orman.backend.payment.config.PaymentImageStorageProperties;
import com.orman.backend.payment.exception.InvalidPaymentImageException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import javax.imageio.ImageIO;
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
}
