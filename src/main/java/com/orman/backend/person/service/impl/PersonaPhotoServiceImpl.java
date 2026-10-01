package com.orman.backend.person.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.file.FileStorageService;
import com.orman.backend.common.file.StorageInputStreamResource;
import com.orman.backend.common.file.StorageObjectNotFoundException;
import com.orman.backend.common.file.StoredObject;
import com.orman.backend.person.config.PersonaPhotoProperties;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.exception.InvalidPersonaPhotoException;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaPhotoService;
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
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PersonaPhotoServiceImpl implements PersonaPhotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonaPhotoServiceImpl.class);
    private static final Map<String, String> EXTENSIONS = Map.of("jpeg", "jpg", "png", "png");
    private static final Map<String, MediaType> MEDIA_TYPES = Map.of("jpg", MediaType.IMAGE_JPEG, "png", MediaType.IMAGE_PNG);

    private final PersonaRepository personaRepository;
    private final PersonaPhotoProperties properties;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void upload(Integer codper, MultipartFile foto) {
        Persona persona = find(codper);
        BufferedImage image = validateAndRead(foto);
        String target = destination(codper);
        try {
            byte[] content = writeJpeg(scaleForProfile(image));
            fileStorageService.put(target, content, MediaType.IMAGE_JPEG_VALUE);
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(target);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new InvalidPersonaPhotoException("No fue posible almacenar la fotografía.");
        }

        String previousReference = persona.getFoto();
        persona.setFoto(target);
        try {
            personaRepository.saveAndFlush(persona);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
        registerReplacementLifecycle(target, resolveStoredKey(previousReference, codper));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaPhotoResource get(Integer codper) {
        String key = resolveStoredKey(find(codper).getFoto(), codper);
        if (key == null) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        MediaType mediaType = MEDIA_TYPES.get(extension(key));
        if (mediaType == null) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        try {
            StoredObject object = fileStorageService.get(key);
            Resource resource = new StorageInputStreamResource(object);
            return new PersonaPhotoResource(resource, mediaType);
        } catch (StorageObjectNotFoundException exception) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer codper) {
        Persona persona = find(codper);
        String key = resolveStoredKey(persona.getFoto(), codper);
        boolean externalPhoto = persona.getFoto() != null
                && persona.getFoto().matches("(?i)^https?://[^\\s]+$");
        if (!externalPhoto && (key == null || !fileStorageService.exists(key))) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        persona.setFoto(null);
        personaRepository.saveAndFlush(persona);
        deleteAfterCommit(key);
    }

    @Override
    public void deleteAfterPersonaRemoval(Integer codper, String reference) {
        deleteAfterCommit(resolveStoredKey(reference, codper));
    }

    private BufferedImage validateAndRead(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new InvalidPersonaPhotoException("La fotografía es obligatoria.");
        }
        if (foto.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidPersonaPhotoException("La fotografía supera el tamaño máximo permitido.");
        }
        String contentType = foto.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            throw new InvalidPersonaPhotoException("Solo se permiten imágenes JPEG o PNG.");
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(foto.getInputStream())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new InvalidPersonaPhotoException("El archivo no contiene una imagen válida.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                BufferedImage image = reader.read(0);
                if (!EXTENSIONS.containsKey(format) || image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new InvalidPersonaPhotoException("Solo se permiten imágenes JPEG o PNG válidas.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (InvalidPersonaPhotoException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidPersonaPhotoException("El archivo no contiene una imagen válida.");
        }
    }

    private Persona find(Integer codper) {
        return personaRepository.findById(codper)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada."));
    }

    private String destination(Integer codper) {
        return "personas/" + codper + "/" + UUID.randomUUID() + ".jpg";
    }

    private String resolveStoredKey(String reference, Integer codper) {
        if (reference == null || !reference.matches("personas/" + codper + "/[0-9a-fA-F-]+\\.(jpg|png)")) {
            return null;
        }
        return reference;
    }

    private BufferedImage scaleForProfile(BufferedImage source) {
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
            LOGGER.warn("No se pudo eliminar una fotografía de Persona almacenada.");
        }
    }
}
