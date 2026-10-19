package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import com.example.certificates.model.CertificateDetails;
import com.example.certificates.model.ExtractRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificateExtractionServiceTest {
    private DocumentTextExtractor textExtractor;
    private CertificateExtractionService service;

    @BeforeEach
    void setUp() {
        textExtractor = mock(DocumentTextExtractor.class);
        CertificateFieldParser parser = new CertificateFieldParser(
                Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC));
        service = new CertificateExtractionService(textExtractor, parser);
    }

    @Test
    void rejectsInvalidBase64() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.extract(new ExtractRequest("%%%not-base64%%%", "pdf")));
        assertEquals("INVALID_BASE64", ex.getCode());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void rejectsMissingCertificateString() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.extract(new ExtractRequest("  ", "pdf")));
        assertEquals("CERTIFICATE_REQUIRED", ex.getCode());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void acceptsBase64DataUriPrefix() {
        byte[] fakePdf = "%PDF-1.4\nsynthetic".getBytes(StandardCharsets.US_ASCII);
        String base64 = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(fakePdf);
        when(textExtractor.extractFromPdf(any(byte[].class))).thenReturn("Expiry Date: 2027-12-31");
        CertificateDetails result = service.extract(new ExtractRequest(base64, "pdf"));
        assertEquals("2027-12-31", result.expiryDate());
        assertEquals("valid", result.status());
    }

    @Test
    void rejectsUnsupportedType() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.extract(new ExtractRequest("YWJj", "docx")));
        assertEquals("UNSUPPORTED_FILE_TYPE", ex.getCode());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void rejectsMismatchBetweenPdfTypeAndBytes() {
        String base64 = Base64.getEncoder().encodeToString("not a PDF".getBytes(StandardCharsets.UTF_8));
        ApiException ex = assertThrows(ApiException.class,
                () -> service.extract(new ExtractRequest(base64, "pdf")));
        assertEquals("FILE_TYPE_MISMATCH", ex.getCode());
        verifyNoInteractions(textExtractor);
    }

    @Test
    void extractsFieldsFromValidPdfText() {
        byte[] pdfHeaderAndBody = "%PDF-1.4\nsynthetic".getBytes(StandardCharsets.US_ASCII);
        String base64 = Base64.getEncoder().encodeToString(pdfHeaderAndBody);
        when(textExtractor.extractFromPdf(any(byte[].class))).thenReturn("""
                Certificate Number: CERT-TEST-1
                Certificate Name: Backend Basics Certificate
                Presented to: Yash Sample
                Issued By: Demo Institute
                Issue Date: 2026-01-01
                Expiry Date: 2027-01-01
                """);
        CertificateDetails result = service.extract(new ExtractRequest(base64, "pdf"));
        assertEquals("CERT-TEST-1", result.certificateNumber());
        assertEquals("Yash Sample", result.holderName());
        assertEquals("valid", result.status());
    }
}
