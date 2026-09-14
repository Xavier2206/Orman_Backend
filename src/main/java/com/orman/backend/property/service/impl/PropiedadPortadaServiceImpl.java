package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

    @Override
    @Transactional
    public void upload(Integer codprop, MultipartFile foto, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        BufferedImage image = validateAndRead(foto);
        Path target = destination(codprop);
        try {
            Files.createDirectories(target.getParent());
            writeJpeg(scaleForCover(image), target);
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new InvalidPropiedadPortadaException("No fue posible almacenar la portada de la Propiedad.");
        }

        Path previous = resolveStoredFile(propiedad.getPortadaRef(), codprop);
        propiedad.setPortadaRef(reference(codprop, target));
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
        Path file = resolveStoredFile(propiedad.getPortadaRef(), codprop);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        MediaType mediaType = MEDIA_TYPES.get(extension(file));
        if (mediaType == null) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        Resource resource = new FileSystemResource(file);
        return new PropiedadPortadaResource(resource, mediaType);
    }

    @Override
    @Transactional
    public void delete(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        Path file = resolveStoredFile(propiedad.getPortadaRef(), codprop);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("La portada de la Propiedad no existe.");
        }
        propiedad.setPortadaRef(null);
        propiedadRepository.saveAndFlush(propiedad);
        deleteAfterCommit(file);
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

    private Path destination(Integer codprop) {
        Path root = storageRoot();
        Path directory = root.resolve("propiedades").resolve(String.valueOf(codprop)).normalize();
        if (!directory.startsWith(root)) {
            throw new InvalidPropiedadPortadaException("La ruta de portada no es válida.");
        }
        return directory.resolve(UUID.randomUUID() + ".jpg").normalize();
    }

    private String reference(Integer codprop, Path target) {
        return "propiedades/" + codprop + "/" + target.getFileName();
    }

    private Path resolveStoredFile(String reference, Integer codprop) {
        if (reference == null || !reference.matches("propiedades/" + codprop + "/[0-9a-fA-F-]+\\.jpg")) {
            return null;
        }
        Path candidate = storageRoot().resolve(reference).normalize();
        return candidate.startsWith(storageRoot()) ? candidate : null;
    }

    private Path storageRoot() {
        return Path.of(properties.root()).toAbsolutePath().normalize();
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

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(0.85f);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void registerReplacementLifecycle(Path target, Path previous) {
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

    private void deleteAfterCommit(Path path) {
        if (path == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(path);
            }
        });
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("No se pudo eliminar una portada de Propiedad almacenada.");
        }
    }
}
