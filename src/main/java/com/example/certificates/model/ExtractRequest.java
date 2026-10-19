package com.example.certificates.model;

import jakarta.validation.constraints.NotBlank;

public record ExtractRequest(
        @NotBlank(message = "certificate is required") String certificate,
        @NotBlank(message = "fileType is required") String fileType
) {}
