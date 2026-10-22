package com.example.certificates.service;

import com.example.certificates.model.CertificateDetails;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class X509CertificateExtractorTest {
    private byte[] samplePem() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/domain-certificate.pem")) {
            assertNotNull(in, "synthetic X.509 test certificate should be available");
            return in.readAllBytes();
        }
    }

    @Test
    void extractsSslFieldsAndDomainNames() throws Exception {
        X509CertificateExtractor extractor = new X509CertificateExtractor(
                Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC));

        CertificateDetails details = extractor.extract(samplePem(), "pem");

        assertEquals("ssl_tls", details.certificateType());
        assertEquals("demo.example.test", details.certificateName());
        assertNull(details.holderName());
        assertNotNull(details.certificateNumber());
        assertNotNull(details.x509());
        assertTrue(details.x509().subject().contains("CN=demo.example.test"));
        assertTrue(details.x509().subjectAlternativeNames().contains("demo.example.test"));
        assertTrue(details.x509().subjectAlternativeNames().contains("*.demo.example.test"));
        assertNotNull(details.x509().signatureAlgorithm());
        assertEquals("valid", details.status());
    }

    @Test
    void marksExpiredCertificateExpired() throws Exception {
        X509CertificateExtractor extractor = new X509CertificateExtractor(
                Clock.fixed(Instant.parse("2040-01-01T00:00:00Z"), ZoneOffset.UTC));

        CertificateDetails details = extractor.extract(samplePem(), "pem");
        assertEquals("expired", details.status());
    }

    @Test
    void marksCertificateNotYetValidAsUnknown() throws Exception {
        // The included fixture is generated for the current test environment; compare well before its start date.
        X509CertificateExtractor extractor = new X509CertificateExtractor(
                Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));

        CertificateDetails details = extractor.extract(samplePem(), "pem");
        assertEquals("unknown", details.status());
    }
}
