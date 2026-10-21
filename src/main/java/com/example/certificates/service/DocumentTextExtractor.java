package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class DocumentTextExtractor {
    private static final int OCR_DPI = 220;
    private static final int MAX_OCR_PAGES = 5;

    public String extractFromImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw unreadable("The image is corrupted or cannot be decoded");
            }
            return runOcr(image);
        } catch (IOException e) {
            throw unreadable("The image is corrupted or cannot be decoded");
        }
    }

    public String extractFromPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw unreadable("Password-protected PDFs are not supported");
            }
            String text = new PDFTextStripper().getText(document);
            if (text != null && text.replaceAll("\\s", "").length() >= 20) {
                return text;
            }

            // Scanned/image-based PDFs often contain little or no embedded text. Render a bounded
            // number of pages and OCR them instead of sending every page to native OCR.
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder ocrText = new StringBuilder();
            int pagesToRead = Math.min(document.getNumberOfPages(), MAX_OCR_PAGES);
            for (int page = 0; page < pagesToRead; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.RGB);
                ocrText.append(runOcr(image)).append('\n');
            }
            if (ocrText.toString().isBlank()) {
                throw unreadable("No readable text was found in the PDF");
            }
            return ocrText.toString();
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw unreadable("The PDF is corrupted or cannot be decoded");
        }
    }

    private String runOcr(BufferedImage image) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(
                "C:/Program Files/Tesseract-OCR/tessdata"
            );
            tesseract.setLanguage("eng");
            return tesseract.doOCR(image);
        } catch (TesseractException | RuntimeException | LinkageError e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_PROCESSING_FAILED",
                    "OCR could not read this certificate. Verify the OCR runtime is installed and try a clearer file.");
        }
    }

    private ApiException unreadable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CERTIFICATE_UNREADABLE", message);
    }
}
