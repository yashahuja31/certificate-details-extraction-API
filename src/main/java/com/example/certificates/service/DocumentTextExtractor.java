package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentTextExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTextExtractor.class);
    private static final int OCR_DPI = 220;
    private static final int MAX_OCR_PAGES = 5;
    private static final String DEFAULT_TESSDATA_PATH = "C:/Program Files/Tesseract-OCR/tessdata";

    public String extractFromImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                LOGGER.warn("Image decoder could not read the supplied image bytes");
                throw unreadable("The image is corrupted or cannot be decoded");
            }
            LOGGER.debug("Decoded image; width={}, height={}", image.getWidth(), image.getHeight());
            return runOcr(image);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("Image decoding failed", e);
            throw unreadable("The image is corrupted or cannot be decoded");
        }
    }

    public String extractFromPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                LOGGER.warn("A password-protected PDF was submitted; encrypted PDFs are unsupported");
                throw unreadable("Password-protected PDFs are not supported");
            }

            LOGGER.debug("Opened PDF; pageCount={}", document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            if (text != null && text.replaceAll("\\s", "").length() >= 20) {
                LOGGER.debug("Extracted embedded PDF text; characterCount={}", text.length());
                return text;
            }

            // Scanned/image-based PDFs often contain little or no embedded text. Render a bounded
            // number of pages and OCR them instead of sending every page to native OCR.
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder ocrText = new StringBuilder();
            int pagesToRead = Math.min(document.getNumberOfPages(), MAX_OCR_PAGES);
            LOGGER.debug("PDF contains insufficient embedded text; using OCR on {} page(s)", pagesToRead);
            for (int page = 0; page < pagesToRead; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.RGB);
                LOGGER.debug("Rendered PDF page {} for OCR; width={}, height={}",
                        page + 1, image.getWidth(), image.getHeight());
                ocrText.append(runOcr(image)).append('\n');
            }
            if (ocrText.toString().isBlank()) {
                LOGGER.warn("OCR did not return readable text from the PDF");
                throw unreadable("No readable text was found in the PDF");
            }
            LOGGER.debug("PDF OCR completed; characterCount={}", ocrText.length());
            return ocrText.toString();
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("PDF decoding or parsing failed", e);
            throw unreadable("The PDF is corrupted or cannot be decoded");
        }
    }

    private String runOcr(BufferedImage image) {
        try {
            ITesseract tesseract = new Tesseract();
            String dataPath = resolveTessdataPath();
            if (dataPath != null) {
                tesseract.setDatapath(dataPath);
                LOGGER.debug("Configured Tesseract data directory: {}", dataPath);
            } else {
                LOGGER.warn("Tesseract tessdata directory was not found; set TESSDATA_PREFIX or app.ocr.tessdata-path");
            }
            tesseract.setLanguage("eng");

            LOGGER.debug("Starting OCR; width={}, height={}, language=eng", image.getWidth(), image.getHeight());
            String text = tesseract.doOCR(image);
            LOGGER.debug("OCR completed; characterCount={}", text == null ? 0 : text.length());
            return text;
        } catch (TesseractException | RuntimeException | LinkageError e) {
            // Log the underlying exception for developers, but don't expose it to the API client.
            LOGGER.error("OCR failed for image dimensions {}x{}. Check the Tesseract runtime, native libraries and eng.traineddata.",
                    image.getWidth(), image.getHeight(), e);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_PROCESSING_FAILED",
                    "OCR could not read this certificate. Verify the OCR runtime is installed and try a clearer file.");
        }
    }

    private String resolveTessdataPath() {
        String configured = System.getProperty("app.ocr.tessdata-path");
        if (configured != null && !configured.isBlank()) return configured;

        configured = System.getenv("TESSDATA_PREFIX");
        if (configured != null && !configured.isBlank()) return configured;

        if (Files.isDirectory(Path.of(DEFAULT_TESSDATA_PATH))) return DEFAULT_TESSDATA_PATH;
        return null;
    }

    private ApiException unreadable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CERTIFICATE_UNREADABLE", message);
    }
}
