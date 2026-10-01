package com.orman.backend.contract.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.config.ContratoArchivoProperties;
import com.orman.backend.contract.entity.ContratoArchivoEntity;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.exception.InvalidContratoArchivoException;
import com.orman.backend.contract.mapper.ContratoArchivoMapper;
import com.orman.backend.contract.repository.ContratoArchivoRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoArchivoServiceImplTest {

    private static final long MB = 1024L * 1024L;

    @TempDir Path storage;
    @Mock ContratoArchivoRepository repository;
    @Mock ContractOwnershipService ownershipService;

    private final List<ContratoArchivoEntity> records = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private ContratoEntity contrato;
    private Authentication authentication;
    private ContratoArchivoProperties properties;
    private ContratoPdfProcessor processor;
    private ContratoArchivoServiceImpl service;

    @BeforeEach
    void setUp() {
        contrato = new ContratoEntity();
        ReflectionTestUtils.setField(contrato, "codcon", 42);
        authentication = new TestingAuthenticationToken(
                new AuthenticatedUser("owner.test", UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
        properties = properties(20 * MB);
        processor = new ContratoPdfProcessor(properties);
        service = new ContratoArchivoServiceImpl(repository, new ContratoArchivoMapper(), ownershipService,
                properties, processor, fixedClock(), new com.orman.backend.common.file.LocalFileStorageService(
                java.util.Map.of("contratos", storage.toString())));

        lenient().when(ownershipService.findOwnedContrato(42, authentication)).thenReturn(contrato);
        lenient().when(repository.existsByContratoCodconAndOrden(any(), any())).thenAnswer(invocation -> records.stream()
                .anyMatch(row -> row.getOrden().equals(invocation.getArgument(1))));
        lenient().when(repository.saveAndFlush(any(ContratoArchivoEntity.class))).thenAnswer(invocation -> {
            ContratoArchivoEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", nextId.getAndIncrement());
            records.add(entity);
            return entity;
        });
        lenient().when(repository.findAllByContratoCodconOrderByOrdenAscIdAsc(42)).thenAnswer(invocation -> records.stream()
                .filter(row -> row.getContrato().getCodcon().equals(42))
                .sorted(java.util.Comparator.comparing(ContratoArchivoEntity::getOrden))
                .toList());
        lenient().when(repository.findByIdAndContratoCodcon(any(), any())).thenAnswer(invocation -> records.stream()
                .filter(row -> row.getId().equals(invocation.getArgument(0)))
                .filter(row -> row.getContrato().getCodcon().equals(invocation.getArgument(1)))
                .findFirst());
        lenient().doAnswer(invocation -> {
            records.remove(invocation.getArgument(0));
            return null;
        }).when(repository).delete(any(ContratoArchivoEntity.class));
    }

    @Test
    void storesValidPdfAndReturnsMetadataWithoutStoragePathOrPublicUrl() throws Exception {
        byte[] pdf = pdf("Contrato de alquiler");

        var response = service.create(42, upload("contrato-original.pdf", "application/pdf", pdf), 0,
                authentication);

        assertThat(response.codarc()).isEqualTo(1);
        assertThat(response.codcon()).isEqualTo(42);
        assertThat(response.nombreArchivo()).isEqualTo("contrato-original.pdf");
        assertThat(response.tipoContenido()).isEqualTo("application/pdf");
        assertThat(response.tamanoOriginal()).isEqualTo((long) pdf.length);
        assertThat(response.tamanoFinal()).isEqualTo((long) pdf.length);
        assertThat(response.fechaSubida()).isNotNull();
        assertThat(response.subidoPor()).isEqualTo("owner.test");
        assertThat(response.almacenadoInternamente()).isTrue();

        List<Path> stored = storedFiles();
        assertThat(stored).hasSize(1);
        assertThat(Files.readAllBytes(stored.getFirst())).containsExactly(pdf);
        assertThat(service.listByContrato(42, authentication)).hasSize(1);
        Resource download = service.download(42, response.codarc(), authentication).resource();
        try (var input = download.getInputStream()) {
            assertThat(input.readAllBytes()).containsExactly(pdf);
        }
    }

    @Test
    void deletesOnlySelectedPdfAndKeepsOtherDocumentsAvailable() throws Exception {
        byte[] pdf = pdf("Contrato firmado");
        var first = service.create(42, upload("primero.pdf", "application/pdf", pdf), 0, authentication);
        var second = service.create(42, upload("segundo.pdf", "application/pdf", pdf), 1, authentication);
        Path firstPath = storage.resolve(record(first.codarc()).getRutaRef());
        Path secondPath = storage.resolve(record(second.codarc()).getRutaRef());

        service.delete(42, first.codarc(), authentication);

        assertThat(firstPath).doesNotExist();
        assertThat(secondPath).exists();
        assertThat(service.listByContrato(42, authentication)).extracting(row -> row.codarc())
                .containsExactly(second.codarc());
        assertThat(service.download(42, second.codarc(), authentication).resource().exists()).isTrue();
        assertThatThrownBy(() -> service.download(42, first.codarc(), authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsEmptyWrongMimeWrongExtensionAndCorruptPdf() throws Exception {
        assertInvalid(upload("empty.pdf", "application/pdf", new byte[0]), "obligatorio");
        assertInvalid(upload("foto.pdf", "image/png", pdf("not an image")), "application/pdf");
        assertInvalid(upload("contrato.docx", "application/pdf", pdf("not a Word file")), "extensión");
        assertInvalid(upload("corrupto.pdf", "application/pdf", "%PDF-1.7\nno-es-un-pdf".getBytes()),
                "PDF válido");
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void rejectsFileLargerThanConfiguredContractLimit() throws Exception {
        byte[] pdf = pdf("Documento grande para el límite");
        ContratoArchivoProperties tinyLimit = properties(pdf.length - 1L);
        ContratoArchivoServiceImpl limited = new ContratoArchivoServiceImpl(repository, new ContratoArchivoMapper(),
                ownershipService, tinyLimit, new ContratoPdfProcessor(tinyLimit), fixedClock(),
                new com.orman.backend.common.file.LocalFileStorageService(
                        java.util.Map.of("contratos", storage.toString())));

        assertThatThrownBy(() -> limited.create(42, upload("limite.pdf", "application/pdf", pdf), 0,
                authentication)).isInstanceOf(InvalidContratoArchivoException.class)
                .hasMessageContaining("tamaño máximo");
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    void rejectsUploadDownloadAndDeleteWhenContractOwnershipFails() throws Exception {
        byte[] pdf = pdf("Privado");
        var saved = service.create(42, upload("privado.pdf", "application/pdf", pdf), 0, authentication);
        doThrow(new AccessDeniedException("denied")).when(ownershipService)
                .findOwnedContrato(42, authentication);

        assertThatThrownBy(() -> service.create(42, upload("otro.pdf", "application/pdf", pdf), 1,
                authentication)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.download(42, saved.codarc(), authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.delete(42, saved.codarc(), authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.listByContrato(42, authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(storedFiles()).hasSize(1);
        verify(ownershipService, org.mockito.Mockito.atLeast(5)).findOwnedContrato(42, authentication);
    }

    @Test
    void losslesslyRewritesLargeUnsignedPdfAndPreservesItsText() throws Exception {
        byte[] base = pdf("Texto que debe conservarse");
        byte[] padded = java.util.Arrays.copyOf(base, (int) (10 * MB + 1024));
        java.util.Arrays.fill(padded, base.length, padded.length, (byte) ' ');
        Path source = storage.resolve("large-source.pdf");
        Path optimized = storage.resolve("large-optimized.pdf");
        Files.write(source, padded);

        processor.process(source, optimized);

        assertThat(Files.size(optimized)).isLessThan(Files.size(source));
        try (PDDocument saved = org.apache.pdfbox.Loader.loadPDF(optimized.toFile())) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(saved))
                    .contains("Texto que debe conservarse");
        }
    }

    private void assertInvalid(MockMultipartFile file, String expectedMessage) {
        assertThatThrownBy(() -> service.create(42, file, 0, authentication))
                .isInstanceOf(InvalidContratoArchivoException.class).hasMessageContaining(expectedMessage);
    }

    private ContratoArchivoProperties properties(long maxBytes) {
        return new ContratoArchivoProperties(storage.toString(), maxBytes, 10 * MB, 16_000_000);
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), OrmanTimeConfig.ORMAN_ZONE);
    }

    private MockMultipartFile upload(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("archivo", filename, contentType, bytes);
    }

    private byte[] pdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private List<Path> storedFiles() throws IOException {
        Path contractDirectory = storage.resolve("contratos/42");
        if (!Files.isDirectory(contractDirectory)) {
            return List.of();
        }
        try (var files = Files.list(contractDirectory)) {
            return files.toList();
        }
    }

    private ContratoArchivoEntity record(Integer codarc) {
        return records.stream().filter(row -> row.getId().equals(codarc)).findFirst().orElseThrow();
    }
}
