package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.config.PropiedadPortadaProperties;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.exception.InvalidPropiedadPortadaException;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.PropiedadPortadaService;
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
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropiedadPortadaServiceImplTest {

    @TempDir Path storage;

    private final PropiedadRepository propiedadRepository = mock(PropiedadRepository.class);
    private final PropertyOwnershipService ownershipService = mock(PropertyOwnershipService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final PropiedadEntity propiedad = propiedad(7, null);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void acceptsJpegAndPngStoresOptimizedJpegAndExposesResource() throws Exception {
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        when(propiedadRepository.saveAndFlush(propiedad)).thenReturn(propiedad);
        PropiedadPortadaServiceImpl service = service(5 * 1024 * 1024, 1600);

        service.upload(7, image("portada.png", "image/png", "png", 2000, 1000), authentication);

        assertThat(propiedad.getPortadaRef()).matches("propiedades/7/[0-9a-f-]+\\.jpg");
        Path stored = storage.resolve(propiedad.getPortadaRef());
        assertThat(stored).exists();
        BufferedImage result = ImageIO.read(stored.toFile());
        assertThat(result.getWidth()).isEqualTo(1600);
        assertThat(result.getHeight()).isEqualTo(800);
        PropiedadPortadaService.PropiedadPortadaResource resource = service.get(7, authentication);
        assertThat(resource.mediaType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(resource.resource()).isInstanceOf(Resource.class);
    }

    @Test
    void doesNotEnlargeSmallImagesAndReplacesOnlyAfterCommit() throws Exception {
        Path previous = storage.resolve("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, imageBytes("jpg", 80, 40));
        propiedad.setPortadaRef("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        when(propiedadRepository.saveAndFlush(propiedad)).thenReturn(propiedad);
        TransactionSynchronizationManager.initSynchronization();

        service(5 * 1024 * 1024, 1600).upload(7,
                image("portada.jpg", "image/jpeg", "jpg", 80, 40), authentication);

        Path current = storage.resolve(propiedad.getPortadaRef());
        assertThat(ImageIO.read(current.toFile()).getWidth()).isEqualTo(80);
        assertThat(previous).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertThat(previous).doesNotExist();
        assertThat(current).exists();
    }

    @Test
    void rollsBackReplacementFileWithoutDeletingPreviousReference() throws Exception {
        Path previous = storage.resolve("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, imageBytes("jpg", 80, 40));
        propiedad.setPortadaRef("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        when(propiedadRepository.saveAndFlush(propiedad)).thenReturn(propiedad);
        TransactionSynchronizationManager.initSynchronization();

        service(5 * 1024 * 1024, 1600).upload(7,
                image("portada.jpg", "image/jpeg", "jpg", 80, 40), authentication);
        Path replacement = storage.resolve(propiedad.getPortadaRef());
        assertThat(replacement).exists();

        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(replacement).doesNotExist();
        assertThat(previous).exists();
    }

    @Test
    void deletesReferenceAndFileAfterCommit() throws Exception {
        Path file = storage.resolve("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(file.getParent());
        Files.write(file, imageBytes("jpg", 20, 20));
        propiedad.setPortadaRef("propiedades/7/00000000-0000-0000-0000-000000000001.jpg");
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        when(propiedadRepository.saveAndFlush(propiedad)).thenReturn(propiedad);
        TransactionSynchronizationManager.initSynchronization();

        service(5 * 1024 * 1024, 1600).delete(7, authentication);

        assertThat(propiedad.getPortadaRef()).isNull();
        assertThat(file).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        assertThat(file).doesNotExist();
        verify(propiedadRepository).saveAndFlush(propiedad);
    }

    @Test
    void rejectsEmptyWrongMimeFakeOversizedAndTraversalFiles() throws Exception {
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        PropiedadPortadaServiceImpl service = service(10, 1600);

        assertThatThrownBy(() -> service.upload(7,
                new MockMultipartFile("foto", "empty.jpg", "image/jpeg", new byte[0]), authentication))
                .isInstanceOf(InvalidPropiedadPortadaException.class);
        assertThatThrownBy(() -> service.upload(7,
                new MockMultipartFile("foto", "text.txt", "text/plain", "not-image".getBytes()), authentication))
                .isInstanceOf(InvalidPropiedadPortadaException.class);
        assertThatThrownBy(() -> service.upload(7,
                new MockMultipartFile("foto", "fake.jpg", "image/jpeg", "not-image".getBytes()), authentication))
                .isInstanceOf(InvalidPropiedadPortadaException.class);
        assertThatThrownBy(() -> service.upload(7,
                image("large.jpg", "image/jpeg", "jpg", 20, 20), authentication))
                .isInstanceOf(InvalidPropiedadPortadaException.class);

        propiedad.setPortadaRef("propiedades/7/../../outside.jpg");
        assertThatThrownBy(() -> service.get(7, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void preservesDatabaseReferenceWhenStorageFails() throws Exception {
        Path rootFile = storage.resolve("not-a-directory");
        Files.writeString(rootFile, "file");
        PropiedadPortadaServiceImpl service = new PropiedadPortadaServiceImpl(propiedadRepository,
                new PropiedadPortadaProperties(rootFile.toString(), 5 * 1024 * 1024, 1600), ownershipService);
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));

        assertThatThrownBy(() -> service.upload(7,
                image("portada.jpg", "image/jpeg", "jpg", 20, 20), authentication))
                .isInstanceOf(InvalidPropiedadPortadaException.class);
        assertThat(propiedad.getPortadaRef()).isNull();
    }

    @Test
    void everyOperationChecksPropertyOwnership() throws Exception {
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));
        doThrow(new AccessDeniedException("denied")).when(ownershipService)
                .assertCurrentPropietaria(authentication, propiedad.getPropietaria());
        PropiedadPortadaServiceImpl service = service(5 * 1024 * 1024, 1600);

        assertThatThrownBy(() -> service.upload(7, image("portada.jpg", "image/jpeg", "jpg", 20, 20), authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.get(7, authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.delete(7, authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingCoverReturnsNotFound() {
        when(propiedadRepository.findById(7)).thenReturn(Optional.of(propiedad));

        assertThatThrownBy(() -> service(5 * 1024 * 1024, 1600).get(7, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service(5 * 1024 * 1024, 1600).delete(7, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PropiedadPortadaServiceImpl service(long maxFileSize, int maxDimension) {
        return new PropiedadPortadaServiceImpl(propiedadRepository,
                new PropiedadPortadaProperties(storage.toString(), maxFileSize, maxDimension), ownershipService);
    }

    private PropiedadEntity propiedad(int codprop, String portadaRef) {
        PropiedadEntity entity = new PropiedadEntity();
        ReflectionTestUtils.setField(entity, "codprop", codprop);
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", 7);
        entity.setPropietaria(persona);
        entity.setPortadaRef(portadaRef);
        return entity;
    }

    private MockMultipartFile image(String name, String contentType, String format, int width, int height)
            throws Exception {
        return new MockMultipartFile("foto", name, contentType, imageBytes(format, width, height));
    }

    private byte[] imageBytes(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
