package com.example.certificates.service;

import com.example.certificates.model.CertificateDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CertificateFieldParserTest {
    private CertificateFieldParser parser;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC);
        parser = new CertificateFieldParser(fixedClock);
    }

    @Test
    void extractsFieldsAndMarksFutureExpiryValid() {
        String text = """
                Certificate Number: CERT-2025-1024
                Certificate Name: Professional Training Certificate
                Presented to: Yash Sample
                Issued By: Example Technical Institute
                Issue Date: 2025-01-01
                Expiry Date: 2027-12-31
                """;
        CertificateDetails result = parser.parse(text);
        assertEquals("CERT-2025-1024", result.certificateNumber());
        assertEquals("Yash Sample", result.holderName());
        assertEquals("Professional Training Certificate", result.certificateName());
        assertEquals("Example Technical Institute", result.issuer());
        assertEquals("2025-01-01", result.issueDate());
        assertEquals("2027-12-31", result.expiryDate());
        assertEquals("valid", result.status());
    }

    @Test
    void parsesMultipleFieldsReturnedAsASingleTextRun() {
        String text = "Certificate Number: CERT-JOINED-1 Certificate Name: Backend Development "
                + "Presented to: Aarav Mehta Issued By: Northstar Academy "
                + "Issue Date: 2026-01-15 Expiry Date: 2027-12-31";
        CertificateDetails result = parser.parse(text);
        assertEquals("CERT-JOINED-1", result.certificateNumber());
        assertEquals("Backend Development", result.certificateName());
        assertEquals("Aarav Mehta", result.holderName());
        assertEquals("Northstar Academy", result.issuer());
        assertEquals("2026-01-15", result.issueDate());
        assertEquals("2027-12-31", result.expiryDate());
        assertEquals("valid", result.status());
    }

    @Test
    void extractsHolderNameWhenPdfAddsBlankLinesAfterCertificationPhrase() {
        String text = "Certificate of Completion\nCertificate Number: CERT-BLANK-1\n"
                + "This is to certify that\n\nAarav Mehta\n"
                + "Certificate Name: Backend Basics\nExpiry Date: 2027-12-31";
        CertificateDetails result = parser.parse(text);
        assertEquals("Aarav Mehta", result.holderName());
        assertEquals("Backend Basics", result.certificateName());
        assertEquals("2027-12-31", result.expiryDate());
        assertEquals("valid", result.status());
    }

    @Test
    void marksPastExpiryExpired() {
        CertificateDetails result = parser.parse("Expiry Date: 2025-12-31");
        assertEquals("expired", result.status());
    }

    @Test
    void expiryDateTodayIsStillValidForThatCalendarDay() {
        CertificateDetails result = parser.parse("Expiry Date: 2026-10-16");
        assertEquals("valid", result.status());
    }

    @Test
    void missingExpiryIsUnknownAndDoesNotInventDate() {
        CertificateDetails result = parser.parse("Issue Date: 2025-01-01\nPresented to: Yash Sample");
        assertNull(result.expiryDate());
        assertEquals("unknown", result.status());
    }

    @Test
    void explicitNonExpiringRuleCanBeValidWithoutAnExpiryDate() {
        CertificateDetails result = parser.parse("This certificate does not expire.");
        assertNull(result.expiryDate());
        assertEquals("valid", result.status());
    }

    @Test
    void ambiguousNumericExpiryIsUnknown() {
        CertificateDetails result = parser.parse("Expiry Date: 03/04/2027");
        assertNull(result.expiryDate());
        assertEquals("unknown", result.status());
    }

    @Test
    void returnsNullForUnavailableFields() {
        CertificateDetails result = parser.parse("Certificate of Completion");
        assertNull(result.certificateNumber());
        assertNull(result.holderName());
        assertNull(result.issuer());
        assertNull(result.issueDate());
        assertNull(result.expiryDate());
        assertEquals("unknown", result.status());
    }
}
