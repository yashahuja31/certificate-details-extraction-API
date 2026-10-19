package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import com.example.certificates.model.CertificateDetails;
import com.example.certificates.model.ExtractRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;

@Service
public class CertificateExtractionService {
    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private final DocumentTextExtractor textExtractor;
    private final CertificateFieldParser parser;

    public CertificateExtractionService(DocumentTextExtractor textExtractor, CertificateFieldParser parser) {
        this.textExtractor = textExtractor;
        this.parser = parser;
    }

    public CertificateDetails extract(ExtractRequest request) {
        if (request.certificate() == null || request.certificate().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CERTIFICATE_REQUIRED", "certificate must be a non-empty Base64 string");
        }
        String type = request.fileType() == null ? "" : request.fileType().trim().toLowerCase(Locale.ROOT);
        if (type.startsWith(".")) type = type.substring(1);
        if (!type.equals("pdf") && !type.equals("png") && !type.equals("jpg") && !type.equals("jpeg")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", "fileType must be pdf, png, jpg, or jpeg");
        }

        String encoded = request.certificate().trim();
        if (encoded.startsWith("data:")) {
            int comma = encoded.indexOf(',');
            if (comma < 0 || !encoded.substring(0, comma).toLowerCase(Locale.ROOT).contains(";base64")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BASE64", "certificate must contain valid Base64 data");
            }
            encoded = encoded.substring(comma + 1);
        }
        byte[] bytes;
        try {
            // Whitespace is common when Base64 is copied across lines; all other invalid characters fail.
            bytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BASE64", "certificate is not valid Base64");
        }
        if (bytes.length == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Decoded certificate file is empty");
        if (bytes.length > MAX_FILE_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Certificate files must be 10 MB or smaller");

        String text;
        if (type.equals("pdf")) {
            if (!startsWith(bytes, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw invalidFileType("The decoded file does not appear to be a PDF");
            }
            text = textExtractor.extractFromPdf(bytes);
        } else {
            boolean png = startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            if ((type.equals("png") && !png) || ((type.equals("jpg") || type.equals("jpeg")) && !jpeg)) {
                throw invalidFileType("The decoded file signature does not match fileType");
            }
            text = textExtractor.extractFromImage(bytes);
        }
        if (text == null || text.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CERTIFICATE_UNREADABLE", "No readable text was found in the certificate");
        }
        return parser.parse(text);
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
        return true;
    }

    private ApiException invalidFileType(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "FILE_TYPE_MISMATCH", message);
    }
}
