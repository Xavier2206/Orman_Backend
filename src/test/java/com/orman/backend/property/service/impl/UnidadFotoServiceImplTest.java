package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.config.UnidadFotoProperties;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.entity.UnidadFotoEntity;
import com.orman.backend.property.exception.InvalidUnidadFotoException;
import com.orman.backend.property.mapper.UnidadFotoMapper;
import com.orman.backend.property.repository.UnidadFotoRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnidadFotoServiceImplTest {

    @TempDir Path storage;

    private final UnidadRepository unidadRepository = mock(UnidadRepository.class);
    private final UnidadFotoRepository unidadFotoRepository = mock(UnidadFotoRepository.class);
    private final PropertyOwnershipService ownershipService = mock(PropertyOwnershipService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final UnidadEntity unidad = unidad(7);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createsPngAsBoundedJpegWithPrivateUuidReference() throws Exception {
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        when(unidadFotoRepository.saveAndFlush(any(UnidadFotoEntity.class))).thenAnswer(invocation -> {
            UnidadFotoEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 19);
            return saved;
        });
        TransactionSynchronizationManager.initSynchronization();

        UnidadFotoResponse response = service(5 * 1024 * 1024, 1600).createInternal(7,
                image("transparente.png", "image/png", "png", 2000, 1000, true),
                new UnidadFotoMetadataRequest(" Sala ", " Baño ", 0), authentication);

        ArgumentCaptor<UnidadFotoEntity> captor = ArgumentCaptor.forClass(UnidadFotoEntity.class);
        verify(unidadFotoRepository).saveAndFlush(captor.capture());
        UnidadFotoEntity saved = captor.getValue();
        Path file = storage.resolve(saved.getFotoRef());
        BufferedImage result = ImageIO.read(file.toFile());
        byte[] jpeg = Files.readAllBytes(file);
        assertThat(response).satisfies(value -> {
            assertThat(value.id()).isEqualTo(19);
            assertThat(value.url()).isNull();
            assertThat(value.tieneArchivo()).isTrue();
            assertThat(value.titulo()).isEqualTo("Sala");
            assertThat(value.ambiente()).isEqualTo("Baño");
        });
        assertThat(saved.getFotoRef()).matches("unidades/7/[0-9a-f-]+\\.jpg");
        assertThat(jpeg[0]).isEqualTo((byte) 0xFF);
        assertThat(jpeg[1]).isEqualTo((byte) 0xD8);
        assertThat(result.getWidth()).isEqualTo(1600);
        assertThat(result.getHeight()).isEqualTo(800);
        Color background = new Color(result.getRGB(0, 0));
        assertThat(background.getRed()).isGreaterThan(240);
        assertThat(background.getGreen()).isGreaterThan(240);
        assertThat(background.getBlue()).isGreaterThan(240);
    }

    @Test
    void validatesEmptyWrongMimeCorruptedAndOversizedFiles() throws Exception {
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        UnidadFotoServiceImpl service = service(10, 1600);

        assertThatThrownBy(() -> service.createInternal(7,
                new MockMultipartFile("foto", "empty.jpg", "image/jpeg", new byte[0]), metadata(), authentication))
                .isInstanceOf(InvalidUnidadFotoException.class);
        assertThatThrownBy(() -> service.createInternal(7,
                new MockMultipartFile("foto", "text.txt", "text/plain", "not-image".getBytes()), metadata(), authentication))
                .isInstanceOf(InvalidUnidadFotoException.class);
        assertThatThrownBy(() -> service.createInternal(7,
                new MockMultipartFile("foto", "fake.jpg", "image/jpeg", "not-image".getBytes()), metadata(), authentication))
                .isInstanceOf(InvalidUnidadFotoException.class);
        assertThatThrownBy(() -> service.createInternal(7,
                image("png-con-mime-falso.jpg", "image/jpeg", "png", 20, 20, false), metadata(), authentication))
                .isInstanceOf(InvalidUnidadFotoException.class);
        assertThatThrownBy(() -> service.createInternal(7,
                new MockMultipartFile("foto", "large.jpg", "image/jpeg", new byte[11]), metadata(), authentication))
                .isInstanceOf(InvalidUnidadFotoException.class);
    }

    @Test
    void replacesOnlyAfterCommitAndRemovesReplacementOnRollback() throws Exception {
        Path previous = storage.resolve("unidades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, imageBytes("jpg", 80, 40, false));
        UnidadFotoEntity foto = internalPhoto(31, previous.getFileName().toString());
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        when(unidadFotoRepository.findByIdAndUnidadCoduni(31, 7)).thenReturn(Optional.of(foto));
        when(unidadFotoRepository.saveAndFlush(foto)).thenReturn(foto);
        TransactionSynchronizationManager.initSynchronization();

        UnidadFotoResponse response = service(5 * 1024 * 1024, 1600).replaceArchivo(7, 31,
                image("nueva.jpg", "image/jpeg", "jpg", 80, 40, false), authentication);
        Path replacement = storage.resolve(foto.getFotoRef());

        assertThat(response.tieneArchivo()).isTrue();
        assertThat(response.url()).isNull();
        assertThat(replacement).exists();
        assertThat(previous).exists();
        BufferedImage storedReplacement = ImageIO.read(replacement.toFile());
        assertThat(storedReplacement.getWidth()).isEqualTo(80);
        assertThat(storedReplacement.getHeight()).isEqualTo(40);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(replacement).doesNotExist();
        assertThat(previous).exists();
    }

    @Test
    void convertsInternalPhotoBackToHistoricalUrlAndDeletesItsFileAfterCommit() throws Exception {
        Path previous = storage.resolve("unidades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, imageBytes("jpg", 80, 40, false));
        UnidadFotoEntity foto = internalPhoto(33, previous.getFileName().toString());
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        when(unidadFotoRepository.findByIdAndUnidadCoduni(33, 7)).thenReturn(Optional.of(foto));
        when(unidadFotoRepository.saveAndFlush(foto)).thenReturn(foto);
        TransactionSynchronizationManager.initSynchronization();

        UnidadFotoResponse response = service(5 * 1024 * 1024, 1600).update(7, 33,
                new UnidadFotoRequest("https://example.test/historica.jpg", "Sala", "Sala", 0), authentication);

        assertThat(response.url()).isEqualTo("https://example.test/historica.jpg");
        assertThat(response.tieneArchivo()).isFalse();
        assertThat(foto.getFotoRef()).isNull();
        assertThat(previous).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCommit());
        assertThat(previous).doesNotExist();
    }

    @Test
    void updatesMetadataWithoutChangingInternalSourceAndDeletesAfterCommit() throws Exception {
        Path file = storage.resolve("unidades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(file.getParent());
        Files.write(file, imageBytes("jpg", 80, 40, false));
        UnidadFotoEntity foto = internalPhoto(35, file.getFileName().toString());
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        when(unidadFotoRepository.findByIdAndUnidadCoduni(35, 7)).thenReturn(Optional.of(foto));
        when(unidadFotoRepository.saveAndFlush(foto)).thenReturn(foto);
        TransactionSynchronizationManager.initSynchronization();

        UnidadFotoResponse response = service(5 * 1024 * 1024, 1600).updateMetadata(7, 35,
                new UnidadFotoMetadataRequest(" Cocina ", " Cocina ", 2), authentication);
        String reference = foto.getFotoRef();
        service(5 * 1024 * 1024, 1600).delete(7, 35, authentication);

        assertThat(response).satisfies(value -> {
            assertThat(value.titulo()).isEqualTo("Cocina");
            assertThat(value.ambiente()).isEqualTo("Cocina");
            assertThat(value.orden()).isEqualTo(2);
            assertThat(value.tieneArchivo()).isTrue();
        });
        assertThat(foto.getFotoRef()).isEqualTo(reference);
        assertThat(file).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCommit());
        assertThat(file).doesNotExist();
        verify(unidadFotoRepository).delete(foto);
    }

    @Test
    void servesOnlyValidInternalReferences() throws Exception {
        UnidadFotoEntity foto = internalPhoto(37, "00000000-0000-0000-0000-000000000001.jpg");
        Path file = storage.resolve(foto.getFotoRef());
        Files.createDirectories(file.getParent());
        Files.write(file, imageBytes("jpg", 20, 20, false));
        when(unidadRepository.findByCoduni(7)).thenReturn(Optional.of(unidad));
        when(unidadFotoRepository.findByIdAndUnidadCoduni(37, 7)).thenReturn(Optional.of(foto));

        assertThat(service(5 * 1024 * 1024, 1600).getArchivo(7, 37, authentication).mediaType().toString())
                .isEqualTo("image/jpeg");
        foto.setFotoRef("unidades/7/../../outside.jpg");
        assertThatThrownBy(() -> service(5 * 1024 * 1024, 1600).getArchivo(7, 37, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UnidadFotoServiceImpl service(long maxFileSize, int maxDimension) {
        return new UnidadFotoServiceImpl(unidadRepository, unidadFotoRepository, new UnidadFotoMapper(),
                ownershipService, new UnidadFotoProperties(storage.toString(), maxFileSize, maxDimension),
                new com.orman.backend.common.file.LocalFileStorageService(
                        java.util.Map.of("unidades", storage.toString())));
    }

    private UnidadFotoMetadataRequest metadata() {
        return new UnidadFotoMetadataRequest(null, "Sala", 0);
    }

    private UnidadEntity unidad(int coduni) {
        UnidadEntity entity = new UnidadEntity();
        ReflectionTestUtils.setField(entity, "coduni", coduni);
        PropiedadEntity propiedad = new PropiedadEntity();
        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 4);
        propiedad.setPropietaria(propietaria);
        entity.setPropiedad(propiedad);
        return entity;
    }

    private UnidadFotoEntity internalPhoto(int id, String filename) {
        UnidadFotoEntity foto = new UnidadFotoEntity();
        ReflectionTestUtils.setField(foto, "id", id);
        foto.setUnidad(unidad);
        foto.setFotoRef("unidades/7/" + filename);
        foto.setOrden(0);
        foto.setPortada(false);
        return foto;
    }

    private MockMultipartFile image(String name, String contentType, String format, int width, int height,
                                    boolean transparent) throws Exception {
        return new MockMultipartFile("foto", name, contentType, imageBytes(format, width, height, transparent));
    }

    private byte[] imageBytes(String format, int width, int height, boolean transparent) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        if (!transparent) {
            var graphics = image.createGraphics();
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
