package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.property.config.UnidadFotoProperties;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.entity.UnidadFotoEntity;
import com.orman.backend.property.exception.InvalidUnidadFotoException;
import com.orman.backend.property.mapper.UnidadFotoMapper;
import com.orman.backend.property.repository.UnidadFotoRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.UnidadFotoService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.dao.DataIntegrityViolationException;
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
public class UnidadFotoServiceImpl implements UnidadFotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnidadFotoServiceImpl.class);
    private static final Map<String, String> EXTENSIONS = Map.of("jpeg", "jpg", "png", "png");

    private final UnidadRepository unidadRepository;
    private final UnidadFotoRepository unidadFotoRepository;
    private final UnidadFotoMapper unidadFotoMapper;
    private final PropertyOwnershipService propertyOwnershipService;
    private final UnidadFotoProperties properties;

    @Override
    @Transactional
    public UnidadFotoResponse create(Integer coduni, UnidadFotoRequest request, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidad(coduni, authentication);
        assertAvailableOrden(coduni, request.orden(), null);
        try {
            UnidadFotoEntity saved = unidadFotoRepository.saveAndFlush(unidadFotoMapper.toEntity(request, unidad));
            return unidadFotoMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional
    public UnidadFotoResponse createInternal(Integer coduni, MultipartFile foto, UnidadFotoMetadataRequest request,
                                             Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidad(coduni, authentication);
        assertAvailableOrden(coduni, request.orden(), null);
        BufferedImage image = validateAndRead(foto);
        Path target = destination(coduni);
        try {
            Files.createDirectories(target.getParent());
            writeJpeg(scaleForPhoto(image), target);
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new InvalidUnidadFotoException("No fue posible almacenar la fotografía de la Unidad.");
        }

        try {
            UnidadFotoEntity saved = unidadFotoRepository.saveAndFlush(
                    unidadFotoMapper.toInternalEntity(request, unidad, reference(coduni, target)));
            registerReplacementLifecycle(target, null);
            return unidadFotoMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            deleteQuietly(target);
            throw duplicateOrden();
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadFotoResponse> listByUnidad(Integer coduni, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return unidadFotoRepository
                .findAllByUnidadCoduniAndUnidadPropiedadPropietariaCodperOrderByOrdenAscIdAsc(coduni, codper)
                .stream().map(unidadFotoMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UnidadFotoResponse update(Integer coduni, Integer id, UnidadFotoRequest request, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        assertAvailableOrden(coduni, request.orden(), id);
        Path previous = resolveStoredFile(foto.getFotoRef(), coduni);
        try {
            unidadFotoMapper.update(foto, request);
            UnidadFotoResponse response = unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
            deleteAfterCommit(previous);
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional
    public UnidadFotoResponse updateMetadata(Integer coduni, Integer id, UnidadFotoMetadataRequest request,
                                             Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        assertAvailableOrden(coduni, request.orden(), id);
        try {
            unidadFotoMapper.updateMetadata(foto, request);
            return unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional
    public UnidadFotoResponse replaceArchivo(Integer coduni, Integer id, MultipartFile archivo,
                                             Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        BufferedImage image = validateAndRead(archivo);
        Path target = destination(coduni);
        try {
            Files.createDirectories(target.getParent());
            writeJpeg(scaleForPhoto(image), target);
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new InvalidUnidadFotoException("No fue posible almacenar la fotografía de la Unidad.");
        }

        Path previous = resolveStoredFile(foto.getFotoRef(), coduni);
        foto.setUrl(null);
        foto.setFotoRef(reference(coduni, target));
        try {
            UnidadFotoResponse response = unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
            registerReplacementLifecycle(target, previous);
            return response;
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadFotoResource getArchivo(Integer coduni, Integer id, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        Path file = resolveStoredFile(foto.getFotoRef(), coduni);
        if (file == null || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("El archivo interno de la fotografía de la Unidad no existe.");
        }
        Resource resource = new FileSystemResource(file);
        return new UnidadFotoResource(resource, MediaType.IMAGE_JPEG);
    }

    @Override
    @Transactional
    public UnidadFotoResponse setPortada(Integer coduni, Integer id, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduniForUpdate(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        UnidadFotoEntity foto = findFoto(coduni, id);
        unidadFotoRepository.clearPortadaByUnidadCoduni(coduni);
        foto.setPortada(true);
        return unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
    }

    @Override
    @Transactional
    public void delete(Integer coduni, Integer id, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        Path file = resolveStoredFile(foto.getFotoRef(), coduni);
        unidadFotoRepository.delete(foto);
        unidadFotoRepository.flush();
        deleteAfterCommit(file);
    }

    private UnidadEntity findOwnedUnidad(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduni(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    private UnidadFotoEntity findFoto(Integer coduni, Integer id) {
        return unidadFotoRepository.findByIdAndUnidadCoduni(id, coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Fotografía de Unidad no encontrada."));
    }

    private void assertAvailableOrden(Integer coduni, Integer orden, Integer id) {
        boolean exists = id == null
                ? unidadFotoRepository.existsByUnidadCoduniAndOrden(coduni, orden)
                : unidadFotoRepository.existsByUnidadCoduniAndOrdenAndIdNot(coduni, orden, id);
        if (exists) {
            throw duplicateOrden();
        }
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden de la fotografía ya existe en esta Unidad.");
    }

    private BufferedImage validateAndRead(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new InvalidUnidadFotoException("La fotografía es obligatoria.");
        }
        if (foto.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidUnidadFotoException("La fotografía supera el tamaño máximo permitido.");
        }
        String contentType = foto.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            throw new InvalidUnidadFotoException("Solo se permiten imágenes JPEG o PNG.");
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
                if (!EXTENSIONS.containsKey(format) || !declaredContentTypeFor(format).equals(contentType) || image == null
                        || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw invalidImage();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (InvalidUnidadFotoException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private InvalidUnidadFotoException invalidImage() {
        return new InvalidUnidadFotoException("El archivo no contiene una imagen válida JPEG o PNG.");
    }

    private String declaredContentTypeFor(String format) {
        return "jpeg".equals(format) ? MediaType.IMAGE_JPEG_VALUE : MediaType.IMAGE_PNG_VALUE;
    }

    private Path destination(Integer coduni) {
        Path root = storageRoot();
        Path directory = root.resolve("unidades").resolve(String.valueOf(coduni)).normalize();
        if (!directory.startsWith(root)) {
            throw new InvalidUnidadFotoException("La ruta de fotografía no es válida.");
        }
        return directory.resolve(UUID.randomUUID() + ".jpg").normalize();
    }

    private String reference(Integer coduni, Path target) {
        return "unidades/" + coduni + "/" + target.getFileName();
    }

    private Path resolveStoredFile(String reference, Integer coduni) {
        if (reference == null || !reference.matches("unidades/" + coduni + "/[0-9a-fA-F-]+\\.jpg")) {
            return null;
        }
        Path candidate = storageRoot().resolve(reference).normalize();
        return candidate.startsWith(storageRoot()) ? candidate : null;
    }

    private Path storageRoot() {
        return Path.of(properties.root()).toAbsolutePath().normalize();
    }

    private BufferedImage scaleForPhoto(BufferedImage source) {
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
            LOGGER.warn("No se pudo eliminar una fotografía de Unidad almacenada.");
        }
    }
}
