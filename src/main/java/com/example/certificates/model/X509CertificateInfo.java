package com.example.certificates.model;

import java.util.List;

/** Extra fields that are meaningful for an X.509 SSL/TLS certificate. */
public record X509CertificateInfo(
        String serialNumber,
        String subject,
        List<String> subjectAlternativeNames,
        String validFrom,
        String validUntil,
        String signatureAlgorithm
) {}
