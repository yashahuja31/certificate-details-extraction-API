package com.example.certificates.api;

import com.example.certificates.model.CertificateDetails;
import com.example.certificates.model.ExtractRequest;
import com.example.certificates.model.ExtractionResponse;
import com.example.certificates.service.CertificateExtractionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateController.class);

    private final CertificateExtractionService extractionService;

    public CertificateController(CertificateExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ExtractionResponse extract(@Valid @RequestBody ExtractRequest request) {
        // Never log the Base64 string or the content of the certificate.
        LOGGER.info("Certificate extraction request received");

        CertificateDetails details = extractionService.extract(request);

        LOGGER.info("Certificate extraction request completed; type={}, status={}",
                details.certificateType(), details.status());
        return new ExtractionResponse(true, "Certificate details extracted successfully", details);
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        LOGGER.debug("Certificate API health endpoint requested");
        return java.util.Map.of("status", "UP");
    }
}
