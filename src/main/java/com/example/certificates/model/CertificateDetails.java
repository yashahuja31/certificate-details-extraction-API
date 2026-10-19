package com.example.certificates.model;

public record CertificateDetails(
        String certificateNumber,
        String holderName,
        String certificateName,
        String issuer,
        String issueDate,
        String expiryDate,
        String status
) {}
