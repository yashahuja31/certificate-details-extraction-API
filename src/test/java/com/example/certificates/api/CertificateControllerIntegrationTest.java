package com.example.certificates.api;

import com.example.certificates.model.CertificateDetails;
import com.example.certificates.service.CertificateExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CertificateControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CertificateExtractionService extractionService;

    @Test
    void extractEndpointReturnsStructuredCertificateResponse() throws Exception {
        when(extractionService.extract(any())).thenReturn(new CertificateDetails(
                "CERT-DEMO-1", "Aarav Mehta", "Backend Development", "Northstar Academy",
                "2026-01-15", "2027-12-31", "valid"));

        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificate\":\"JVBERi0xLjQ=\",\"fileType\":\"pdf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateNumber").value("CERT-DEMO-1"))
                .andExpect(jsonPath("$.data.status").value("valid"));
    }

    @Test
    void missingCertificateReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/certificates/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileType\":\"pdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void rootHealthEndpointMatchesTheOriginalApiContract() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
