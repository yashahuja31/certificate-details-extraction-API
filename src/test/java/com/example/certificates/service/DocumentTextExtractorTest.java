package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTest {
    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void unreadableImageIsReportedAsUnprocessable() {
        ApiException ex = assertThrows(ApiException.class, () -> extractor.extractFromImage(
                "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals("CERTIFICATE_UNREADABLE", ex.getCode());
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void corruptedPdfIsReportedAsUnprocessable() {
        byte[] corrupt = "%PDF-1.4\nThis is not a real PDF file\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        ApiException ex = assertThrows(ApiException.class, () -> extractor.extractFromPdf(corrupt));
        assertEquals("CERTIFICATE_UNREADABLE", ex.getCode());
        assertEquals(422, ex.getStatus().value());
    }
}
