package com.orman.backend.payment.service;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.file.FileStorageService;
import com.orman.backend.common.file.StorageException;
import com.orman.backend.common.file.StorageInputStreamResource;
import com.orman.backend.common.file.StorageObjectNotFoundException;
import com.orman.backend.common.file.StoredObject;
import com.orman.backend.payment.config.PaymentImageStorageProperties;
import com.orman.backend.payment.exception.InvalidPaymentImageException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PaymentImageStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentImageStorageService.class);
    private static final long MAX_DECODED_PIXELS = 16_000_000L;
    private static final int DECODE_SIDE_LIMIT = 3840;

    private final PaymentImageStorageProperties properties;
    private final FileStorageService fileStorageService;

    public StoredPaymentImage storeQr(MultipartFile file, Integer codperPropietaria) {
        return store(file, "qr-cobro", codperPropietaria, true);
    }

    public StoredPaymentImage storeProof(MultipartFile file, Integer codpag) {
        return store(file, "comprobantes", codpag, false);
    }

    public PaymentImageContent loadQr(String reference, Integer codperPropietaria) {
        return load(reference, "qr-cobro", codperPropietaria);
    }

    public PaymentImageContent loadProof(String reference, Integer codpag) {
        return load(reference, "comprobantes", codpag);
    }

    public void deleteQrAfterCommit(String reference, Integer codperPropietaria) {
        deleteAfterCommit(resolveStoredKey(reference, "qr-cobro", codperPropietaria));
    }

    public void deleteProofAfterCommit(String reference, Integer codpag) {
        deleteAfterCommit(resolveStoredKey(reference, "comprobantes", codpag));
    }

    private StoredPaymentImage store(MultipartFile file, String directoryName, Integer parentId, boolean qrImage) {
        if (parentId == null || parentId <= 0) {
            throw new InvalidPaymentImageException("No fue posible asociar la imagen al recurso.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidPaymentImageException("La imagen es obligatoria.");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidPaymentImageException("La imagen supera el máximo de 5 MB.");
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String suppliedType = normalizeContentType(file.getContentType());
        validateExtensionAndDeclaredType(originalName, suppliedType);

        Path root = storageRoot();
        Path upload = null;
        Path processed = null;
        String storedKey = null;
        try {
            Files.createDirectories(root);
            upload = Files.createTempFile(root, ".payment-image-upload-", ".tmp");
            processed = Files.createTempFile(root, ".payment-image-processed-", ".tmp");
            copyUpload(file, upload);
            ImageDetails image = normalize(upload, processed, originalName, suppliedType, qrImage);

            String physicalName = UUID.randomUUID() + "." + image.extension();
            storedKey = directoryName + "/" + parentId + "/" + physicalName;
            try (InputStream input = Files.newInputStream(processed)) {
                fileStorageService.put(storedKey, input, Files.size(processed), image.contentType());
            }
            registerRollbackCleanup(storedKey);
            return new StoredPaymentImage(storedKey,
                    originalName, image.contentType());
        } catch (InvalidPaymentImageException exception) {
            throw exception;
        } catch (StorageException exception) {
            deleteQuietly(storedKey);
            throw exception;
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(storedKey);
            LOGGER.error("No se pudo procesar una imagen de pago: {}", exception.getClass().getSimpleName());
            throw new InvalidPaymentImageException("No fue posible validar o almacenar la imagen.");
        } finally {
            deleteTemporaryQuietly(upload);
            deleteTemporaryQuietly(processed);
        }
    }

    private ImageDetails normalize(Path source, Path target, String originalName, String suppliedType,
                                   boolean qrImage) throws IOException {
        String extension = extension(originalName);
        try (ImageInputStream input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) {
                throw new InvalidPaymentImageException("El archivo no contiene una imagen válida.");
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidPaymentImageException("El archivo no contiene una imagen válida.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String actualType;
                String outputFormat;
                String outputExtension;
                if (format.equals("png")) {
                    actualType = MediaType.IMAGE_PNG_VALUE;
                    outputFormat = "png";
                    outputExtension = "png";
                } else if (format.equals("jpeg") || format.equals("jpg")) {
                    actualType = MediaType.IMAGE_JPEG_VALUE;
                    outputFormat = "jpeg";
                    outputExtension = "jpg";
                } else {
                    throw new InvalidPaymentImageException("Solo se permiten imágenes PNG o JPEG.");
                }
                if (!actualType.equals(suppliedType) || !extensionMatches(extension, outputExtension)) {
                    throw new InvalidPaymentImageException("El tipo o extensión no coincide con el contenido de la imagen.");
                }

                int sourceWidth = reader.getWidth(0);
                int sourceHeight = reader.getHeight(0);
                if (sourceWidth <= 0 || sourceHeight <= 0) {
                    throw new InvalidPaymentImageException("La imagen tiene dimensiones no válidas.");
                }
                int subsampling = sourceSubsampling(sourceWidth, sourceHeight);
                var readParam = reader.getDefaultReadParam();
                if (subsampling > 1) {
                    readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }
                BufferedImage decoded = reader.read(0, readParam);
                if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
                    throw new InvalidPaymentImageException("El archivo no contiene una imagen válida.");
                }
                BufferedImage normalized = resize(decoded, outputFormat.equals("png"), qrImage);
                write(normalized, target, outputFormat);
                return new ImageDetails(outputExtension, actualType);
            } finally {
                reader.dispose();
            }
        } catch (InvalidPaymentImageException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new InvalidPaymentImageException("El archivo no contiene una imagen válida o está dañado.");
        }
    }

    private int sourceSubsampling(int width, int height) {
        double pixels = (double) width * height;
        int bySide = Math.max(1, (int) Math.ceil(Math.max(width, height) / (double) DECODE_SIDE_LIMIT));
        int byPixels = Math.max(1, (int) Math.ceil(Math.sqrt(pixels / MAX_DECODED_PIXELS)));
        return Math.max(bySide, byPixels);
    }

    private BufferedImage resize(BufferedImage source, boolean png, boolean qrImage) {
        double scale = Math.min(1d, Math.min((double) properties.maxDimension() / source.getWidth(),
                (double) properties.maxDimension() / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int type = png && source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(width, height, type);
        Graphics2D graphics = target.createGraphics();
        try {
            if (!png || type == BufferedImage.TYPE_INT_RGB) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, qrImage
                    ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                    : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private void write(BufferedImage image, Path target, String format) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IOException("Image writer not available");
        }
        ImageWriter writer = writers.next();
        try (OutputStream fileOutput = Files.newOutputStream(target);
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(fileOutput)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (format.equals("jpeg") && parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.95f);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private void validateExtensionAndDeclaredType(String originalName, String contentType) {
        String extension = extension(originalName);
        if ((!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg"))
                || (!MediaType.IMAGE_PNG_VALUE.equals(contentType) && !MediaType.IMAGE_JPEG_VALUE.equals(contentType))) {
            throw new InvalidPaymentImageException("Solo se permiten imágenes PNG o JPEG.");
        }
        if ((extension.equals("png") && !MediaType.IMAGE_PNG_VALUE.equals(contentType))
                || ((extension.equals("jpg") || extension.equals("jpeg"))
                && !MediaType.IMAGE_JPEG_VALUE.equals(contentType))) {
            throw new InvalidPaymentImageException("El tipo o extensión no coincide con el contenido de la imagen.");
        }
    }

    private String sanitizeOriginalName(String filename) {
        if (filename == null) {
            throw new InvalidPaymentImageException("El nombre original de la imagen es obligatorio.");
        }
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (basename.isBlank() || basename.length() > 255) {
            throw new InvalidPaymentImageException("El nombre de la imagen no es válido.");
        }
        return basename;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String value = separator < 0 ? contentType : contentType.substring(0, separator);
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean extensionMatches(String extension, String actualExtension) {
        return actualExtension.equals("jpg") ? extension.equals("jpg") || extension.equals("jpeg")
                : actualExtension.equals(extension);
    }

    private void copyUpload(MultipartFile file, Path destination) throws IOException {
        long written = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = file.getInputStream(); OutputStream output = Files.newOutputStream(destination)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                written += count;
                if (written > properties.maxFileSizeBytes()) {
                    throw new InvalidPaymentImageException("La imagen supera el máximo de 5 MB.");
                }
                output.write(buffer, 0, count);
            }
        }
        if (written == 0) {
            throw new InvalidPaymentImageException("La imagen no puede estar vacía.");
        }
    }

    private PaymentImageContent load(String reference, String directoryName, Integer parentId) {
        String key = resolveStoredKey(reference, directoryName, parentId);
        if (key == null) {
            throw new ResourceNotFoundException("La imagen solicitada no está disponible.");
        }
        try {
            int slash = key.lastIndexOf('/');
            String name = key.substring(slash + 1);
            String extension = extension(name);
            String contentType = extension.equals("png") ? MediaType.IMAGE_PNG_VALUE
                    : extension.equals("jpg") ? MediaType.IMAGE_JPEG_VALUE : null;
            if (contentType == null) {
                throw new ResourceNotFoundException("La imagen solicitada no está disponible.");
            }
            StoredObject object = fileStorageService.get(key);
            return new PaymentImageContent(new StorageInputStreamResource(object), name, contentType,
                    object.contentLength());
        } catch (StorageObjectNotFoundException exception) {
            throw new ResourceNotFoundException("La imagen solicitada no está disponible.");
        }
    }

    private String resolveStoredKey(String reference, String directoryName, Integer parentId) {
        if (reference == null || parentId == null || parentId <= 0
                || !reference.matches(directoryName + "/" + parentId
                + "/[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            return null;
        }
        return reference;
    }

    private Path storageRoot() {
        return Path.of(properties.root()).toAbsolutePath().normalize();
    }

    private void registerRollbackCleanup(String storedKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(storedKey);
                }
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (key == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(key);
            }
        });
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            fileStorageService.delete(key);
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo limpiar una imagen privada del módulo de pagos.");
        }
    }

    private void deleteTemporaryQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("No se pudo limpiar un archivo temporal del módulo de pagos.");
        }
    }

    private record ImageDetails(String extension, String contentType) {
    }
}
