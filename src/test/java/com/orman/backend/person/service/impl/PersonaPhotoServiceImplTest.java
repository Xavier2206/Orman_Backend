package com.orman.backend.person.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.config.PersonaPhotoProperties;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.exception.InvalidPersonaPhotoException;
import com.orman.backend.person.repository.PersonaRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonaPhotoServiceImplTest {

    @TempDir Path storage;
    private final PersonaRepository repository = mock(PersonaRepository.class);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void acceptsJpegAndPngNormalizesToBoundedJpegAndKeepsAspectRatio() throws Exception {
        Persona persona = persona(7, null);
        when(repository.findById(7)).thenReturn(Optional.of(persona));
        when(repository.saveAndFlush(persona)).thenReturn(persona);
        PersonaPhotoServiceImpl service = service(2 * 1024 * 1024);
        TransactionSynchronizationManager.initSynchronization();

        service.upload(7, image("foto.png", "image/png", "png", 1200, 600));

        assertThat(persona.getFoto()).matches("personas/7/[0-9a-f-]+\\.jpg");
        Path stored = storage.resolve(persona.getFoto());
        BufferedImage result = ImageIO.read(stored.toFile());
        assertThat(result.getWidth()).isEqualTo(320);
        assertThat(result.getHeight()).isEqualTo(160);
        assertThat(service.get(7).mediaType().toString()).isEqualTo("image/jpeg");
        assertThat(stored).exists();
    }

    @Test
    void doesNotEnlargeSmallJpegAndDeletesPreviousOnlyAfterSuccessfulReplacement() throws Exception {
        Path previous = storage.resolve("personas/8/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, jpeg(80, 40));
        Persona persona = persona(8, "personas/8/00000000-0000-0000-0000-000000000001.jpg");
        when(repository.findById(8)).thenReturn(Optional.of(persona));
        when(repository.saveAndFlush(persona)).thenReturn(persona);
        PersonaPhotoServiceImpl service = service(2 * 1024 * 1024);
        TransactionSynchronizationManager.initSynchronization();

        service.upload(8, image("foto.jpg", "image/jpeg", "jpg", 80, 40));

        Path current = storage.resolve(persona.getFoto());
        assertThat(ImageIO.read(current.toFile()).getWidth()).isEqualTo(80);
        assertThat(previous).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        assertThat(previous).doesNotExist();
        assertThat(current).exists();
    }

    @Test
    void removesNewPhotoOnLaterRollbackAndPreservesPreviousPhoto() throws Exception {
        Path previous = storage.resolve("personas/8/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(previous.getParent());
        Files.write(previous, jpeg(20, 20));
        Persona persona = persona(8, "personas/8/00000000-0000-0000-0000-000000000001.jpg");
        when(repository.findById(8)).thenReturn(Optional.of(persona));
        when(repository.saveAndFlush(persona)).thenReturn(persona);
        TransactionSynchronizationManager.initSynchronization();

        service(2 * 1024 * 1024).upload(8, image("replacement.jpg", "image/jpeg", "jpg", 30, 30));
        Path replacement = storage.resolve(persona.getFoto());
        assertThat(replacement).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(replacement).doesNotExist();
        assertThat(previous).exists();
    }

    @Test
    void rejectsInvalidOversizedMissingAndTraversalReferencesWithoutLosingExistingPhoto() throws Exception {
        Persona persona = persona(9, "../../outside.jpg");
        when(repository.findById(9)).thenReturn(Optional.of(persona));
        PersonaPhotoServiceImpl service = service(10);

        assertThatThrownBy(() -> service.upload(9, image("large.jpg", "image/jpeg", "jpg", 20, 20)))
                .isInstanceOf(InvalidPersonaPhotoException.class);
        assertThatThrownBy(() -> service.upload(9, new MockMultipartFile("foto", "fake.jpg", "image/jpeg", "not-image".getBytes())))
                .isInstanceOf(InvalidPersonaPhotoException.class);
        assertThatThrownBy(() -> service.get(9)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(persona.getFoto()).isEqualTo("../../outside.jpg");
        when(repository.findById(10)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(10)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesExistingPhotoAndClearsOnlyItsRelativeReference() throws Exception {
        Path file = storage.resolve("personas/11/00000000-0000-0000-0000-000000000001.jpg");
        Files.createDirectories(file.getParent());
        Files.write(file, jpeg(20, 20));
        Persona persona = persona(11, "personas/11/00000000-0000-0000-0000-000000000001.jpg");
        when(repository.findById(11)).thenReturn(Optional.of(persona));
        when(repository.saveAndFlush(persona)).thenReturn(persona);
        PersonaPhotoServiceImpl service = service(2 * 1024 * 1024);
        TransactionSynchronizationManager.initSynchronization();

        service.delete(11);
        assertThat(persona.getFoto()).isNull();
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        assertThat(file).doesNotExist();
        verify(repository).saveAndFlush(persona);
    }

    @Test
    void removesPhotoOfDeletedPersonaOnlyAfterCommit() throws Exception {
        String reference = "personas/12/00000000-0000-0000-0000-000000000001.jpg";
        Path file = storage.resolve(reference);
        Files.createDirectories(file.getParent());
        Files.write(file, jpeg(20, 20));
        TransactionSynchronizationManager.initSynchronization();

        service(2 * 1024 * 1024).deleteAfterPersonaRemoval(12, reference);
        assertThat(file).exists();
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        assertThat(file).doesNotExist();
    }

    @Test
    void keepsPhotoOfDeletedPersonaWhenTransactionRollsBack() throws Exception {
        String reference = "personas/12/00000000-0000-0000-0000-000000000001.jpg";
        Path file = storage.resolve(reference);
        Files.createDirectories(file.getParent());
        Files.write(file, jpeg(20, 20));
        TransactionSynchronizationManager.initSynchronization();

        service(2 * 1024 * 1024).deleteAfterPersonaRemoval(12, reference);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(file).exists();
    }

    @Test
    void clearsExternalPhotoReferenceWithoutLookingForLocalFile() {
        Persona persona = persona(13, "https://example.test/photo.jpg");
        when(repository.findById(13)).thenReturn(Optional.of(persona));
        when(repository.saveAndFlush(persona)).thenReturn(persona);

        service(2 * 1024 * 1024).delete(13);

        assertThat(persona.getFoto()).isNull();
        verify(repository).saveAndFlush(persona);
    }

    private PersonaPhotoServiceImpl service(long maxFileSize) {
        return new PersonaPhotoServiceImpl(repository, new PersonaPhotoProperties(storage.toString(), maxFileSize, 320));
    }

    private Persona persona(int codper, String foto) {
        Persona persona = new Persona();
        org.springframework.test.util.ReflectionTestUtils.setField(persona, "codper", codper);
        persona.setFoto(foto);
        return persona;
    }

    private MockMultipartFile image(String name, String contentType, String format, int width, int height) throws Exception {
        return new MockMultipartFile("foto", name, contentType, imageBytes(format, width, height));
    }

    private byte[] jpeg(int width, int height) throws Exception {
        return imageBytes("jpg", width, height);
    }

    private byte[] imageBytes(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics().getColor();
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
