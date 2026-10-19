package com.example.certificates.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the full Base64 -> PDFBox -> field parser -> JSON path without mocking the service. */
@SpringBootTest
@AutoConfigureMockMvc
class CertificateApiEndToEndTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void extractsDetailsFromARealSyntheticPdfFixture() throws Exception {
        byte[] pdf = new ClassPathResource("samples/valid-certificate.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateNumber").value("CERT-2026-1001"))
                .andExpect(jsonPath("$.data.holderName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.certificateName").value("Full Stack Web Development"))
                .andExpect(jsonPath("$.data.expiryDate").value("2027-12-31"))
                .andExpect(jsonPath("$.data.status").value("valid"));
    }

    @Test
    void marksAnExpiredSyntheticPdfAsExpired() throws Exception {
        byte[] pdf = new ClassPathResource("samples/expired-certificate.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiryDate").value("2024-04-03"))
                .andExpect(jsonPath("$.data.status").value("expired"));
    }

    @Test
    void returnsUnknownWhenSyntheticPdfHasNoExpiryDate() throws Exception {
        byte[] pdf = new ClassPathResource("samples/missing-expiry-certificate.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiryDate").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("unknown"));
    }

    @Test
    void respectsAnExplicitNonExpiringStatement() throws Exception {
        byte[] pdf = new ClassPathResource("samples/explicit-no-expiry-certificate.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiryDate").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("valid"));
    }

    @Test
    void allowsOptionalFieldsToBeMissingWithoutFailingTheWholeRequest() throws Exception {
        byte[] pdf = new ClassPathResource("samples/missing-fields-certificate.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateNumber").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("unknown"));
    }

    @Test
    void returnsStructuredErrorForCorruptedPdf() throws Exception {
        byte[] pdf = new ClassPathResource("samples/corrupted.pdf").getInputStream().readAllBytes();
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(pdf, "pdf")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CERTIFICATE_UNREADABLE"));
    }

    private String requestJson(byte[] bytes, String fileType) {
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "{\"certificate\":\"" + base64 + "\",\"fileType\":\"" + fileType + "\"}";
    }
}
