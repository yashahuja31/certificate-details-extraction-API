package com.example.certificates.api;

import com.example.certificates.model.ExtractRequest;
import com.example.certificates.model.ExtractionResponse;
import com.example.certificates.service.CertificateExtractionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    private final CertificateExtractionService extractionService;

    public CertificateController(CertificateExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ExtractionResponse extract(@Valid @RequestBody ExtractRequest request) {
        return new ExtractionResponse(true, "Certificate details extracted successfully", extractionService.extract(request));
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP");
    }
}
