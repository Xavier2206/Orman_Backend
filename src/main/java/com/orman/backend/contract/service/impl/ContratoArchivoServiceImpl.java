package com.orman.backend.contract.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.config.ContratoArchivoProperties;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.entity.ContratoArchivoEntity;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.exception.InvalidContratoArchivoException;
import com.orman.backend.contract.mapper.ContratoArchivoMapper;
import com.orman.backend.contract.repository.ContratoArchivoRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.ContratoArchivoContent;
import com.orman.backend.contract.service.ContratoArchivoService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContratoArchivoServiceImpl implements ContratoArchivoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContratoArchivoServiceImpl.class);
    private static final String PDF_CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");

    private final ContratoArchivoRepository contratoArchivoRepository;
    private final ContratoArchivoMapper contratoArchivoMapper;
    private final ContractOwnershipService contractOwnershipService;
    private final ContratoArchivoProperties properties;
    private final ContratoPdfProcessor pdfProcessor;

    @Override
    @Transactional
    public ContratoArchivoResponse create(Integer codcon, MultipartFile archivo, Integer orden,
                                          Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContrato(codcon, authentication);
        if (archivo == null || archivo.isEmpty()) {
            throw new InvalidContratoArchivoException("El archivo PDF es obligatorio y no puede estar vacío.");
        }
        if (orden == null || orden < 0) {
            throw new InvalidContratoArchivoException("El orden debe ser un número mayor o igual a cero.");
        }
        if (contratoArchivoRepository.existsByContratoCodconAndOrden(codcon, orden)) {
            throw duplicateOrden();
        }

        String nombreOriginal = validateMetadata(archivo);
        Path root = storageRoot();
        Path original = null;
        Path processed = null;
        Path stored = null;
        try {
            Files.createDirectories(root);
            original = Files.createTempFile(root, ".contrato-upload-", ".tmp");
            processed = Files.createTempFile(root, ".contrato-processed-", ".tmp");
            copyUpload(archivo, original);
            validatePdfHeader(original);
            long tamanoOriginal = Files.size(original);
            pdfProcessor.process(original, processed);
            long tamanoFinal = Files.size(processed);

            String nombreAlmacenado = UUID.randomUUID() + ".pdf";
            Path contractDirectory = root.resolve("contratos").resolve(codcon.toString()).normalize();
            if (!contractDirectory.startsWith(root)) {
                throw new IOException("Invalid contract storage path");
            }
            Files.createDirectories(contractDirectory);
            stored = contractDirectory.resolve(nombreAlmacenado);
            move(processed, stored);
            String rutaRef = "contratos/" + codcon + "/" + nombreAlmacenado;
            ContratoArchivoEntity entity = contratoArchivoMapper.toEntity(contrato, nombreOriginal,
                    nombreAlmacenado, rutaRef, tamanoOriginal, tamanoFinal,
                    LocalDateTime.now(ZONA_NEGOCIO), authenticatedLogin(authentication), orden);
            try {
                ContratoArchivoEntity saved = contratoArchivoRepository.saveAndFlush(entity);
                deleteStoredFileOnRollback(stored);
                return contratoArchivoMapper.toResponse(saved);
            } catch (DataIntegrityViolationException exception) {
                deleteQuietly(stored);
                throw duplicateOrden();
            } catch (RuntimeException exception) {
                deleteQuietly(stored);
                throw exception;
            }
        } catch (InvalidContratoArchivoException | ConflictException exception) {
            throw exception;
        } catch (IOException exception) {
            LOGGER.error("No se pudo almacenar el documento de Contrato codcon={}", codcon,
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("No fue posible almacenar el documento PDF.");
        } finally {
            deleteQuietly(original);
            deleteQuietly(processed);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoArchivoResponse> listByContrato(Integer codcon, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        return contratoArchivoRepository.findAllByContratoCodconOrderByOrdenAscIdAsc(codcon).stream()
                .map(contratoArchivoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoArchivoContent download(Integer codcon, Integer codarc, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        ContratoArchivoEntity archivo = findArchivo(codcon, codarc);
        Path file = resolveStoredFile(archivo.getRutaRef());
        if (file == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResourceNotFoundException("El documento del Contrato no está disponible.");
        }
        try {
            return new ContratoArchivoContent(new FileSystemResource(file), archivo.getNombreArchivo(),
                    Files.size(file));
        } catch (IOException exception) {
            throw new ResourceNotFoundException("El documento del Contrato no está disponible.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer codcon, Integer codarc, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        ContratoArchivoEntity archivo = findArchivo(codcon, codarc);
        Path file = resolveStoredFile(archivo.getRutaRef());
        contratoArchivoRepository.delete(archivo);
        contratoArchivoRepository.flush();
        deleteStoredFileAfterCommit(file);
    }

    private String validateMetadata(MultipartFile archivo) {
        if (archivo.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidContratoArchivoException("El archivo supera el tamaño máximo permitido.");
        }
        String contentType = archivo.getContentType();
        try {
            if (contentType == null || !MediaType.parseMediaType(contentType)
                    .isCompatibleWith(MediaType.APPLICATION_PDF)) {
                throw new InvalidContratoArchivoException("Solo se permiten archivos con tipo application/pdf.");
            }
        } catch (org.springframework.http.InvalidMediaTypeException exception) {
            throw new InvalidContratoArchivoException("Solo se permiten archivos con tipo application/pdf.");
        }
        String originalFilename = archivo.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidContratoArchivoException("El nombre original del archivo es obligatorio.");
        }
        String basename = originalFilename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (basename.isBlank() || basename.length() > 200
                || !basename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidContratoArchivoException("El nombre del archivo debe tener extensión .pdf.");
        }
        return basename;
    }

    private void copyUpload(MultipartFile archivo, Path destination) throws IOException {
        long written = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = archivo.getInputStream(); OutputStream output = Files.newOutputStream(destination)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                written += count;
                if (written > properties.maxFileSizeBytes()) {
                    throw new InvalidContratoArchivoException("El archivo supera el tamaño máximo permitido.");
                }
                output.write(buffer, 0, count);
            }
        }
        if (written == 0) {
            throw new InvalidContratoArchivoException("El archivo PDF está vacío.");
        }
    }

    private void validatePdfHeader(Path file) throws IOException {
        byte[] header = new byte[5];
        try (InputStream input = Files.newInputStream(file)) {
            if (input.read(header) != header.length || header[0] != '%'
                    || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw new InvalidContratoArchivoException("El contenido no corresponde a un archivo PDF.");
            }
        }
    }

    private String authenticatedLogin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("No se pudo identificar a la propietaria autenticada.");
        }
        return user.login();
    }

    private ContratoArchivoEntity findArchivo(Integer codcon, Integer codarc) {
        return contratoArchivoRepository.findByIdAndContratoCodcon(codarc, codcon)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de Contrato no encontrado."));
    }

    private Path storageRoot() {
        return Path.of(properties.root()).toAbsolutePath().normalize();
    }

    private Path resolveStoredFile(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        Path root = storageRoot();
        Path candidate = root.resolve(reference).normalize();
        if (!candidate.startsWith(root)) {
            return null;
        }
        try {
            Path realRoot = root.toRealPath();
            Path realCandidate = candidate.toRealPath();
            return realCandidate.startsWith(realRoot) ? realCandidate : null;
        } catch (IOException exception) {
            return null;
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void deleteStoredFileOnRollback(Path path) {
        if (path == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(path);
                }
            }
        });
    }

    private void deleteStoredFileAfterCommit(Path path) {
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
            LOGGER.warn("No se pudo limpiar un archivo privado de Contrato.");
        }
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden del archivo ya existe en este Contrato.");
    }
}
