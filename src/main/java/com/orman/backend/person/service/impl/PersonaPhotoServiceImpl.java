package com.orman.backend.person.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.config.PersonaPhotoProperties;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.exception.InvalidPersonaPhotoException;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaPhotoService;
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

    @Override
    @Transactional
    public void upload(Integer codper, MultipartFile foto) {
        Persona persona = find(codper);
        BufferedImage image = validateAndRead(foto);
        Path target = destination(codper);
        try {
            Files.createDirectories(target.getParent());
            writeJpeg(scaleForProfile(image), target);
        } catch (IOException exception) {
            throw new InvalidPersonaPhotoException("No fue posible almacenar la fotografía.");
        }

        String previousReference = persona.getFoto();
        persona.setFoto("personas/" + codper + "/" + target.getFileName());
        try {
            personaRepository.saveAndFlush(persona);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
        deleteAfterCommit(previousReference, codper);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaPhotoResource get(Integer codper) {
        Path file = resolveStoredFile(find(codper).getFoto(), codper);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        MediaType mediaType = MEDIA_TYPES.get(extension(file));
        if (mediaType == null) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        Resource resource = new FileSystemResource(file);
        return new PersonaPhotoResource(resource, mediaType);
    }

    @Override
    @Transactional
    public void delete(Integer codper) {
        Persona persona = find(codper);
        Path file = resolveStoredFile(persona.getFoto(), codper);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("La fotografía de la Persona no existe.");
        }
        persona.setFoto(null);
        personaRepository.saveAndFlush(persona);
        deleteAfterCommit(file);
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

    private Path destination(Integer codper) {
        Path root = storageRoot();
        Path directory = root.resolve("personas").resolve(String.valueOf(codper)).normalize();
        if (!directory.startsWith(root)) {
            throw new InvalidPersonaPhotoException("La ruta de fotografía no es válida.");
        }
        return directory.resolve(UUID.randomUUID() + ".jpg").normalize();
    }

    private Path resolveStoredFile(String reference, Integer codper) {
        if (reference == null || !reference.matches("personas/" + codper + "/[0-9a-fA-F-]+\\.(jpg|png)")) {
            return null;
        }
        Path candidate = storageRoot().resolve(reference).normalize();
        return candidate.startsWith(storageRoot()) ? candidate : null;
    }

    private Path storageRoot() {
        return Path.of(properties.root()).toAbsolutePath().normalize();
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

    private void deleteAfterCommit(String reference, Integer codper) {
        Path previous = resolveStoredFile(reference, codper);
        if (previous != null) {
            deleteAfterCommit(previous);
        }
    }

    private void deleteAfterCommit(Path path) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(path);
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("No se pudo eliminar una fotografía de Persona almacenada.");
        }
    }
}
