package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.file.FileStorageService;
import com.orman.backend.common.file.StorageInputStreamResource;
import com.orman.backend.common.file.StorageObjectNotFoundException;
import com.orman.backend.common.file.StoredObject;
import com.orman.backend.property.config.PropiedadPortadaProperties;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.exception.InvalidPropiedadPortadaException;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.PropiedadPortadaService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PropiedadPortadaServiceImpl implements PropiedadPortadaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropiedadPortadaServiceImpl.class);
    private static final Map<String, String> EXTENSIONS = Map.of("jpeg", "jpg", "png", "png");
    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG);

    private final PropiedadRepository propiedadRepository;
    private final PropiedadPortadaProperties properties;
    private final PropertyOwnershipService propertyOwnershipService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void upload(Integer codprop, MultipartFile foto, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        BufferedImage image = validateAndRead(foto);
        String target = destination(codprop);
        try {
            byte[] content = writeJpeg(scaleForCover(image));
            fileStorageService.put(target, content, MediaType.IMAGE_JPEG_VALUE);
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(target);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new InvalidPropiedadPortadaException("No fue posible almacenar la portada de la Propiedad.");
        }

        String previous = resolveStoredKey(propiedad.getPortadaRef(), codprop);
        propiedad.setPortadaRef(target);
        try {
            propiedadRepository.saveAndFlush(propiedad);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
        registerReplacementLifecycle(target, previous);
    }

    @Override
    @Transactional(readOnly = true)
    public PropiedadPortadaResource get(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        String key = resolveStoredKey(propiedad.getPortadaRef(), codprop);
        if (key == null) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        MediaType mediaType = MEDIA_TYPES.get(extension(key));
        if (mediaType == null) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        try {
            StoredObject object = fileStorageService.get(key);
            return new PropiedadPortadaResource(new StorageInputStreamResource(object), mediaType);
        } catch (StorageObjectNotFoundException exception) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        String key = resolveStoredKey(propiedad.getPortadaRef(), codprop);
        if (key == null || !fileStorageService.exists(key)) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        propiedad.setPortadaRef(null);
        propiedadRepository.saveAndFlush(propiedad);
        deleteAfterCommit(key);
    }

    private PropiedadEntity findOwned(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = propiedadRepository.findById(codprop)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, propiedad.getPropietaria());
        return propiedad;
    }

    private BufferedImage validateAndRead(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new InvalidPropiedadPortadaException("La portada de la Propiedad es obligatoria.");
        }
        if (foto.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidPropiedadPortadaException("La portada supera el tamaño máximo permitido.");
        }
        String contentType = foto.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            throw new InvalidPropiedadPortadaException("Solo se permiten imágenes JPEG o PNG.");
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(foto.getInputStream())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                BufferedImage image = reader.read(0);
                if (!EXTENSIONS.containsKey(format) || image == null
                        || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw invalidImage();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (InvalidPropiedadPortadaException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private InvalidPropiedadPortadaException invalidImage() {
        return new InvalidPropiedadPortadaException("El archivo no contiene una imagen válida JPEG o PNG.");
    }

    private String destination(Integer codprop) {
        return "propiedades/" + codprop + "/" + UUID.randomUUID() + ".jpg";
    }

    private String resolveStoredKey(String reference, Integer codprop) {
        if (reference == null || !reference.matches("propiedades/" + codprop + "/[0-9a-fA-F-]+\\.jpg")) {
            return null;
        }
        return reference;
    }

    private BufferedImage scaleForCover(BufferedImage source) {
        int maxDimension = properties.maxDimension();
        double scale = Math.min(1d, Math.min((double) maxDimension / source.getWidth(),
                (double) maxDimension / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.85f);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), parameters);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String extension(String key) {
        int slash = key.lastIndexOf('/');
        String name = slash < 0 ? key : key.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void registerReplacementLifecycle(String target, String previous) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(previous);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(previous);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteQuietly(target);
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
            LOGGER.warn("No se pudo eliminar una portada de Propiedad almacenada.");
        }
    }
}
