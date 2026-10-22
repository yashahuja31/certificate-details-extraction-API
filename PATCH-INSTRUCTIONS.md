# Add X.509 SSL/TLS certificate support

This is a source patch for the existing `certificate-details-extraction-api-java-spring-boot` project. It adds PEM/CRT/CER/DER support while preserving PDF/PNG/JPG/JPEG extraction.

## Apply it

1. Stop the Spring Boot server (`Ctrl+C`).
2. Extract this ZIP directly into the root of your existing project folder. Allow the included files to overwrite matching files.
3. Do **not** replace `DocumentTextExtractor.java` or `pom.xml`. The patch uses Java's built-in X.509 APIs and doesn't need a new Maven dependency, and leaving the document extractor alone preserves your Tesseract configuration fix.
4. Review `docs/openapi.yaml` and `samples/README.txt` if you maintain these documentation files.
5. Run `mvn clean verify` from the project root.
6. Start the API and test the new sample:

   ```powershell
   java -jar .\target\certificate-details-extraction-api.jar --server.port=8080
   ```

   In another PowerShell window:

   ```powershell
   java -jar .\target\certificate-details-extraction-api.jar --extract ".\samples\domain-certificate.pem" --url http://localhost:8080
   ```

## How the request works

The CLI Base64-encodes the entire PEM file and submits it with `fileType: "pem"`. The API decodes that outer Base64, then the X.509 extractor decodes the PEM certificate body to DER and parses it using Java's `CertificateFactory`. It does not use OCR for PEM/CRT/CER/DER certificates.

Supported `fileType` values after the patch: `pdf`, `png`, `jpg`, `jpeg`, `pem`, `crt`, `cer`, `der`.

For X.509 results:
- `certificateNumber` is the hexadecimal X.509 serial number.
- `certificateName` is the subject common name, often a domain such as `*.example.com`.
- `holderName` and `issueDate` are null because a domain certificate normally has no human holder and X.509 `notBefore` is a validity start, not an explicit issuance date.
- `expiryDate` is the UTC calendar date from `notAfter`.
- `certificateType` is `ssl_tls` and `x509` provides serial number, full subject, DNS Subject Alternative Names, exact validity timestamps, and signature algorithm.
- `status` is `expired` if current time is past `notAfter`, `valid` if the current time is within the certificate validity interval, and `unknown` if it is not yet valid.

This extracts certificate metadata and evaluates the validity window. It does **not** validate a certificate chain against trusted roots, verify revocation, or prove that the certificate is trusted for a particular TLS connection.

The included `samples/domain-certificate.pem` is a synthetic self-signed example certificate. It does not include its private key and is for local parsing tests only; do not deploy it for an actual service.
