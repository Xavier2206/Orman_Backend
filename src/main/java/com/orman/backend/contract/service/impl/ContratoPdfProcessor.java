package com.orman.backend.contract.service.impl;

import com.orman.backend.contract.config.ContratoArchivoProperties;
import com.orman.backend.contract.exception.InvalidContratoArchivoException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class ContratoPdfProcessor {

    private static final int MAX_FORM_DEPTH = 24;

    private final ContratoArchivoProperties properties;

    public ContratoPdfProcessor(ContratoArchivoProperties properties) {
        this.properties = properties;
    }

    public void process(Path source, Path target) {
        PDDocument document;
        try {
            document = Loader.loadPDF(source.toFile());
        } catch (IOException | RuntimeException exception) {
            throw new InvalidContratoArchivoException("El archivo no contiene un PDF válido o está dañado.", exception);
        }

        try (document) {
            if (document.getNumberOfPages() < 1) {
                throw new InvalidContratoArchivoException("El PDF debe contener al menos una página.");
            }

            if (!document.getSignatureDictionaries().isEmpty()
                    || Files.size(source) <= properties.targetOptimizedSizeBytes()) {
                copyOriginal(source, target);
                return;
            }

            String originalText;
            try {
                originalText = new PDFTextStripper().getText(document);
            } catch (IOException | RuntimeException invalidContent) {
                throw new InvalidContratoArchivoException("El PDF está dañado o no se puede leer.", invalidContent);
            }
            try {
                Set<COSDictionary> visitedResources = Collections.newSetFromMap(new IdentityHashMap<>());
                for (var page : document.getPages()) {
                    optimizeResources(document, page.getResources(), visitedResources, 0);
                }
                document.save(target.toFile());
            } catch (IOException | RuntimeException optimizationFailure) {
                copyOriginal(source, target);
                return;
            }

            if (!preservesExtractedText(target, originalText) || Files.size(target) >= Files.size(source)) {
                copyOriginal(source, target);
            }
        } catch (InvalidContratoArchivoException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidContratoArchivoException("No fue posible procesar el PDF.", exception);
        }
    }

    private void optimizeResources(PDDocument document, PDResources resources, Set<COSDictionary> visited,
                                   int depth) throws IOException {
        if (resources == null || depth > MAX_FORM_DEPTH || !visited.add(resources.getCOSObject())) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject object = resources.getXObject(name);
            if (object instanceof PDImageXObject image) {
                optimizeImage(document, resources, name, image);
            } else if (object instanceof PDFormXObject form) {
                optimizeResources(document, form.getResources(), visited, depth + 1);
            }
        }
    }

    private void optimizeImage(PDDocument document, PDResources resources, COSName name,
                               PDImageXObject image) {
        long pixels = (long) image.getWidth() * image.getHeight();
        if (image.isStencil() || image.getCOSObject().containsKey(COSName.SMASK)
                || image.getCOSObject().containsKey(COSName.MASK)
                || pixels <= 0 || pixels > properties.maxImagePixelsToOptimize()) {
            return;
        }
        try {
            BufferedImage decoded = image.getImage();
            if (decoded == null) {
                return;
            }
            PDImageXObject lossless = LosslessFactory.createFromImage(document, decoded);
            if (encodedLength(lossless) < encodedLength(image)) {
                resources.put(name, lossless);
            }
        } catch (IOException | RuntimeException ignored) {
            // An unsupported image is kept unchanged; the PDF itself remains usable.
        }
    }

    private long encodedLength(PDImageXObject image) throws IOException {
        try (InputStream input = image.getCOSObject().createRawInputStream()) {
            return input.transferTo(OutputStream.nullOutputStream());
        }
    }

    private boolean preservesExtractedText(Path pdf, String expectedText) {
        try (PDDocument saved = Loader.loadPDF(pdf.toFile())) {
            return expectedText.equals(new PDFTextStripper().getText(saved));
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private void copyOriginal(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
