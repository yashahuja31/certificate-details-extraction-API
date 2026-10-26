package com.example.certificates.service;

import com.example.certificates.exception.ApiException;
import com.example.certificates.model.CertificateDetails;
import com.example.certificates.model.X509CertificateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses PEM/DER X.509 certificates directly; it does not use OCR. */
@Service
public class X509CertificateExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(X509CertificateExtractor.class);
    private static final Pattern PEM_CERTIFICATE = Pattern.compile(
            "-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----",
            Pattern.DOTALL
    );

    private final Clock clock;

    public X509CertificateExtractor() {
        this(Clock.systemUTC());
    }

    X509CertificateExtractor(Clock clock) {
        this.clock = clock;
    }

    public CertificateDetails extract(byte[] fileBytes, String fileType) {
        LOGGER.debug("Starting X.509 parsing; fileType={}, sizeBytes={}", fileType, fileBytes.length);
        try {
            byte[] derBytes = decodePemIfNeeded(fileBytes, fileType);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(derBytes));

            String commonName = commonName(cert.getSubjectX500Principal());
            String serial = cert.getSerialNumber().toString(16).toUpperCase(Locale.ROOT);
            Instant notBefore = cert.getNotBefore().toInstant();
            Instant notAfter = cert.getNotAfter().toInstant();
            Instant now = clock.instant();

            // This API exposes only valid/expired/unknown for compatibility with the assignment.
            // A certificate that has not reached notBefore is classified as unknown.
            String status;
            if (now.isBefore(notBefore)) {
                status = "unknown";
            } else if (now.isAfter(notAfter)) {
                status = "expired";
            } else {
                status = "valid";
            }

            String validFrom = DateTimeFormatter.ISO_INSTANT.format(notBefore);
            String validUntil = DateTimeFormatter.ISO_INSTANT.format(notAfter);
            String validFromDate = LocalDate.ofInstant(notBefore, ZoneOffset.UTC).toString();
            String validUntilDate = LocalDate.ofInstant(notAfter, ZoneOffset.UTC).toString();
            List<String> dnsNames = getDnsSubjectAlternativeNames(cert);
            if (dnsNames.isEmpty() && commonName != null && !commonName.isBlank()) {
                dnsNames = List.of(commonName);
            }

            X509CertificateInfo x509 = new X509CertificateInfo(
                    serial,
                    cert.getSubjectX500Principal().getName(X500Principal.RFC2253),
                    List.copyOf(dnsNames),
                    validFrom,
                    validUntil,
                    cert.getSigAlgName()
            );

            // A domain certificate has no human holder and no separate issueDate field.
            // certificateName carries the subject CN/domain for the existing response contract;
            // validFrom/validUntil preserve the precise X.509 validity timestamps in x509 metadata.
            LOGGER.debug("X.509 parsing completed; DNS name count={}, status={}", dnsNames.size(), status);
            return new CertificateDetails(
                    serial,
                    null,
                    commonName == null || commonName.isBlank() ? "SSL/TLS certificate" : commonName,
                    cert.getIssuerX500Principal().getName(X500Principal.RFC2253),
                    null,
                    validUntilDate,
                    status,
                    "ssl_tls",
                    x509
            );
        } catch (CertificateException | IllegalArgumentException ex) {
            LOGGER.warn("X.509 certificate parsing failed; causeType={}", ex.getClass().getSimpleName(), ex);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_X509_CERTIFICATE",
                    "The supplied file is not a readable X.509 certificate");
        }
    }

    private byte[] decodePemIfNeeded(byte[] fileBytes, String fileType) {
        String text = new String(fileBytes, StandardCharsets.US_ASCII).trim();
        Matcher matcher = PEM_CERTIFICATE.matcher(text);
        if (matcher.find()) {
            String body = matcher.group(1).replaceAll("\\s", "");
            try {
                return Base64.getDecoder().decode(body);
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_X509_CERTIFICATE",
                        "The PEM certificate contains invalid Base64 data");
            }
        }

        if (fileType.equals("pem")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TYPE_MISMATCH",
                    "fileType pem requires a BEGIN/END CERTIFICATE PEM block");
        }
        // .cer/.crt/.der files may contain a binary DER-encoded X.509 certificate.
        return fileBytes;
    }

    private String commonName(X500Principal principal) {
        try {
            LdapName name = new LdapName(principal.getName(X500Principal.RFC2253));
            for (Rdn rdn : name.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (InvalidNameException ignored) {
            // A missing/unreadable CN is not a fatal error; the full subject is still returned.
        }
        return null;
    }

    private List<String> getDnsSubjectAlternativeNames(X509Certificate cert) throws CertificateException {
        Collection<List<?>> names = cert.getSubjectAlternativeNames();
        if (names == null || names.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();
        for (List<?> entry : names) {
            if (entry.size() >= 2 && entry.get(0) instanceof Integer type && type == 2
                    && entry.get(1) instanceof String dnsName && !dnsName.isBlank()) {
                result.add(dnsName);
            }
        }
        return result;
    }
}
