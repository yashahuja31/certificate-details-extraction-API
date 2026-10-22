package com.example.certificates.model;

/**
 * Common result fields for certificate documents, with optional X.509 metadata for SSL/TLS certs.
 * The seven-argument constructor preserves existing course/achievement-certificate call sites.
 */
public record CertificateDetails(
        String certificateNumber,
        String holderName,
        String certificateName,
        String issuer,
        String issueDate,
        String expiryDate,
        String status,
        String certificateType,
        X509CertificateInfo x509
) {
    public CertificateDetails(
            String certificateNumber,
            String holderName,
            String certificateName,
            String issuer,
            String issueDate,
            String expiryDate,
            String status
    ) {
        this(certificateNumber, holderName, certificateName, issuer, issueDate, expiryDate, status,
                "document", null);
    }
}
