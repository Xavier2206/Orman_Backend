package com.orman.backend.contract.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.file.FileStorageService;
import com.orman.backend.common.file.StorageException;
import com.orman.backend.common.file.StorageInputStreamResource;
import com.orman.backend.common.file.StorageObjectNotFoundException;
import com.orman.backend.common.file.StoredObject;
import com.orman.backend.config.OrmanTimeConfig;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ContratoArchivoRepository contratoArchivoRepository;
    private final ContratoArchivoMapper contratoArchivoMapper;
    private final ContractOwnershipService contractOwnershipService;
    private final ContratoArchivoProperties properties;
    private final ContratoPdfProcessor pdfProcessor;
    private final Clock clock;
    private final FileStorageService fileStorageService;

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
        String storedKey = null;
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
            String rutaRef = "contratos/" + codcon + "/" + nombreAlmacenado;
            storedKey = rutaRef;
            try (InputStream input = Files.newInputStream(processed)) {
                fileStorageService.put(storedKey, input, tamanoFinal, PDF_CONTENT_TYPE);
            }
            ContratoArchivoEntity entity = contratoArchivoMapper.toEntity(contrato, nombreOriginal,
                    nombreAlmacenado, rutaRef, tamanoOriginal, tamanoFinal,
                    OrmanTimeConfig.businessNow(clock), authenticatedLogin(authentication), orden);
            try {
                ContratoArchivoEntity saved = contratoArchivoRepository.saveAndFlush(entity);
                deleteStoredFileOnRollback(storedKey);
                return contratoArchivoMapper.toResponse(saved);
            } catch (DataIntegrityViolationException exception) {
                deleteQuietly(storedKey);
                throw duplicateOrden();
            } catch (RuntimeException exception) {
                deleteQuietly(storedKey);
                throw exception;
            }
        } catch (InvalidContratoArchivoException | ConflictException exception) {
            throw exception;
        } catch (StorageException exception) {
            deleteQuietly(storedKey);
            throw exception;
        } catch (IOException exception) {
            LOGGER.error("No se pudo almacenar el documento de Contrato codcon={}", codcon,
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("No fue posible almacenar el documento PDF.");
        } finally {
            deleteTemporaryQuietly(original);
            deleteTemporaryQuietly(processed);
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
        String key = resolveStoredKey(archivo.getRutaRef(), codcon);
        if (key == null) {
            throw new ResourceNotFoundException("El documento del Contrato no está disponible.");
        }
        try {
            StoredObject object = fileStorageService.get(key);
            return new ContratoArchivoContent(new StorageInputStreamResource(object), archivo.getNombreArchivo(),
                    object.contentLength());
        } catch (StorageObjectNotFoundException exception) {
            throw new ResourceNotFoundException("El documento del Contrato no está disponible.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer codcon, Integer codarc, Authentication authentication) {
        contractOwnershipService.findOwnedContrato(codcon, authentication);
        ContratoArchivoEntity archivo = findArchivo(codcon, codarc);
        String key = resolveStoredKey(archivo.getRutaRef(), codcon);
        contratoArchivoRepository.delete(archivo);
        contratoArchivoRepository.flush();
        deleteStoredFileAfterCommit(key);
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

    private String resolveStoredKey(String reference, Integer codcon) {
        if (reference == null || codcon == null
                || !reference.matches("contratos/" + codcon + "/[0-9a-fA-F-]{36}\\.pdf")) {
            return null;
        }
        return reference;
    }

    private void deleteStoredFileOnRollback(String key) {
        if (key == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(key);
                }
            }
        });
    }

    private void deleteStoredFileAfterCommit(String key) {
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
            LOGGER.warn("No se pudo limpiar un archivo privado de Contrato.");
        }
    }

    private void deleteTemporaryQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("No se pudo limpiar un archivo temporal de Contrato.");
        }
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden del archivo ya existe en este Contrato.");
    }
}
