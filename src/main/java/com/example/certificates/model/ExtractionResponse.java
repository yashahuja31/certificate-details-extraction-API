package com.example.certificates.model;

public record ExtractionResponse(boolean success, String message, CertificateDetails data) {}
