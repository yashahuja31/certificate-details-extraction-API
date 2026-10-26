# Certificate Details Extraction API — Java Spring Boot

A REST API built with Java and Spring Boot to extract structured information from digital certificates. It supports text-based PDF certificates, scanned documents, certificate images, and SSL/TLS X.509 certificates.

The API accepts certificate files encoded as Base64 in JSON and returns extracted metadata, certificate validity status, and structured error responses.

## Features

- **REST API:** `POST /api/certificates/extract`, designed to follow the same request and response contract as the Node.js version.
- **Multiple file formats:** Supports PDF, PNG, JPG, JPEG, and X.509 certificate formats such as PEM, CRT, CER, and DER.
- **PDF extraction:** Apache PDFBox extracts embedded text from PDFs.
- **Scanned PDF processing:** PDFs without usable embedded text are rendered and processed using Tesseract OCR.
- **Image OCR:** Tess4J integrates Tesseract to extract text from image-based certificates.
- **X.509 parsing:** Extracts SSL/TLS certificate metadata, including serial number, subject, issuer, validity period, signature algorithm, and subject alternative names.
- **Rule-based field parsing:** Extracts certificate number, holder name, certificate name, issuer, issue date, and expiry date when available.
- **Validity status:** Returns `valid`, `expired`, or `unknown`. Missing or ambiguous expiry dates are not guessed.
- **Input validation:** Includes file signature checks, Base64 validation, supported file type checks, and a 10 MB decoded-file size limit.
- **Structured errors:** Handles invalid requests and unreadable certificates with structured error responses.
- **Safe logging:** Records application events and processing diagnostics without logging full certificate contents or Base64 payloads.
- **Java CLI:** Supports submitting individual local certificate files to a running API.
- **API documentation:** Provides OpenAPI documentation and an interactive Swagger UI.
- **Testing and samples:** Includes automated tests and synthetic sample certificates.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| Build tool | Apache Maven |
| PDF text extraction | Apache PDFBox |
| OCR integration | Tess4J |
| OCR engine | Tesseract OCR |
| SSL/TLS parsing | Java X.509 certificate APIs |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit and Spring Boot testing tools |
| Logging | SLF4J and Logback |

## Prerequisites

Install the following before running the project:

- Java 21
- Apache Maven
- Tesseract OCR with English language data, required for OCR-based extraction

Verify Java and Maven installations:

```bash
java -version
mvn -version
```

Verify that Tesseract is available:

```bash
tesseract --version
tesseract --list-langs
```

The language list should include `eng`.

### Configure Tesseract OCR

Tesseract must be installed and its `tessdata` directory configured correctly for Tess4J.

For example, on Windows, the directory may be:

```text
C:\Program Files\Tesseract-OCR\tessdata
```

The directory must contain `eng.traineddata`.

If Tesseract is installed elsewhere, update the OCR data path in the application configuration or source code as appropriate for your setup.

PDF text extraction and X.509 parsing do not require OCR when their respective inputs can be processed directly.

## Getting Started

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd certificate-details-extraction-API
```

Replace `<YOUR_REPOSITORY_URL>` with the repository's actual Git URL.

### 2. Build and test

```bash
mvn clean verify
```

This compiles the application and runs the automated verification and test lifecycle. A successful build should finish with:

```text
BUILD SUCCESS
```

### 3. Start the application

Run the application with Maven:

```bash
mvn spring-boot:run
```

Alternatively, package the application and run the JAR:

```bash
mvn clean package
java -jar target/certificate-details-extraction-api.jar
```

The application uses port `8080` by default unless configured otherwise.

To run the packaged application on another port:

```bash
java -jar target/certificate-details-extraction-api.jar --server.port=8081
```

## API Reference

### Extract Certificate Details

**Endpoint:** `POST /api/certificates/extract`

**Content-Type:** `application/json`

#### Request body

```json
{
  "certificate": "<BASE64_ENCODED_CERTIFICATE>",
  "fileType": "pdf"
}
```

| Field | Type | Description |
|---|---|---|
| `certificate` | String | Base64-encoded contents of the certificate file |
| `fileType` | String | Input format, such as `pdf`, `png`, `jpg`, `jpeg`, `pem`, `crt`, `cer`, or `der` |

The `certificate` field must contain the Base64 representation of the file's actual bytes. For PEM certificates, encode the PEM file contents, including the `BEGIN CERTIFICATE` and `END CERTIFICATE` lines.

The decoded file must not exceed 10 MB.

#### Example successful response

A typical document-based certificate response may look like this:

```json
{
  "success": true,
  "message": "Certificate details extracted successfully",
  "data": {
    "certificateNumber": "CERT-2026-1001",
    "holderName": "Aarav Mehta",
    "certificateName": "Full Stack Web Development",
    "issuer": "Northstar Technical Academy",
    "issueDate": "2026-01-15",
    "expiryDate": "2027-12-31",
    "status": "valid"
  }
}
```

This is an illustrative example using synthetic certificate details. Fields may be `null` when the input does not contain information that can be reliably extracted.

#### SSL/TLS certificate response

For an X.509 certificate, the response can also include `certificateType` and an `x509` object with certificate-specific metadata.

Example:

```json
{
  "success": true,
  "message": "Certificate details extracted successfully",
  "data": {
    "certificateNumber": "EEA8BA6FB5D01634C29E217DEB00ED8A",
    "holderName": null,
    "certificateName": "*.example.com",
    "issuer": "CN=Example Certificate Authority",
    "issueDate": null,
    "expiryDate": "2027-12-09",
    "status": "valid",
    "certificateType": "ssl_tls",
    "x509": {
      "serialNumber": "EEA8BA6FB5D01634C29E217DEB00ED8A",
      "subject": "CN=*.example.com",
      "subjectAlternativeNames": [
        "*.example.com",
        "example.com"
      ],
      "validFrom": "2026-05-25T00:00:00Z",
      "validUntil": "2027-12-09T23:59:59Z",
      "signatureAlgorithm": "SHA256withRSA"
    }
  }
}
```

The SSL/TLS example is illustrative; actual values depend on the supplied X.509 certificate.

### Certificate status rules

| Status | Meaning |
|---|---|
| `valid` | The certificate has not expired, or the source explicitly establishes that it does not expire. |
| `expired` | The certificate's expiry date is in the past. |
| `unknown` | Expiry information is missing, unreadable, or ambiguous. |

The API does not assume that a missing expiry date means a certificate never expires.

## Testing with the API

### Using Swagger UI

Start the application and open:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI specification:** `http://localhost:8080/v3/api-docs`

Use the interactive interface to inspect the endpoint schema and submit requests.

### Testing PEM Certificates with Postman

You can test SSL/TLS certificate extraction by submitting a PEM certificate to the API using Postman.

#### 1. Convert the PEM file to a Base64 JSON request

Open PowerShell in the project root and run:

```powershell
$path = (Resolve-Path ".\samples\zoho.pem").Path
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($path))
$body = @{ certificate = $base64; fileType = "pem" } | ConvertTo-Json -Compress
Set-Clipboard -Value $body
```

Replace `samples\zoho.pem` with the path to your PEM certificate file if necessary.

This command reads the PEM file, encodes its contents as Base64, creates the JSON request body, and copies the complete JSON to your clipboard.

#### 2. Configure Postman

Create a new request with the following settings:

| Setting | Value |
|---|---|
| Method | `POST` |
| URL | `http://localhost:8080/api/certificates/extract` |
| Body type | `raw` |
| Format | `JSON` |
| Content-Type | `application/json` |

In Postman, select **Body → raw → JSON**, paste the copied request body, and click **Send**.

The request body will have this structure:

```json
{
  "certificate": "<BASE64_ENCODED_PEM_FILE>",
  "fileType": "pem"
}
```

The placeholder represents the actual Base64-encoded file contents. The command copies the complete request, so you do not need to encode the file or construct the JSON manually.

#### 3. Inspect the response

For a successfully parsed SSL/TLS certificate, the API can return:

- Certificate serial number and subject
- Issuer information
- Certificate validity period
- Expiry date and status (`valid`, `expired`, or `unknown`)
- Subject alternative names
- Signature algorithm

The exact fields depend on the supplied certificate and the metadata available in it.

**Note:** Start the API before sending the request. Use a PEM file containing a certificate, not a private key. Do not submit private keys or confidential certificate material to the API or commit them to the repository.


### Using PowerShell

To encode a local certificate file and prepare a JSON request:

```powershell
$path = (Resolve-Path ".\samples\valid-certificate.pdf").Path

$base64 = [Convert]::ToBase64String(
    [IO.File]::ReadAllBytes($path)
)

$body = @{
    certificate = $base64
    fileType = "pdf"
} | ConvertTo-Json -Compress
```

Submit it to the running API:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/certificates/extract" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

Change the file path and `fileType` to match the certificate you want to test. Verify the actual sample filenames available in your local `samples` directory.

### Using the Java CLI

The project also provides a CLI for submitting a local certificate file to the running API.

For example, from the project root:

```powershell
java -jar .\target\certificate-details-extraction-api.jar `
    --extract ".\samples\valid-certificate.pdf" `
    --url "http://localhost:8080"
```

Run the CLI against an available sample file. The API must already be running, and the JAR must have been built.

## Health Check

The health endpoint is:

```http
GET /api/certificates/health
```

For a local instance, use:

```text
http://localhost:8080/api/certificates/health
```

## Logging

Application logs are written to:

```text
logs/certificate-api.log
```

The log file location and logging levels are configured in `src/main/resources/application.properties`.

The application logs processing events, extraction progress, and error diagnostics while avoiding full certificate text and Base64 payloads.

Generated log files and the `logs/` directory should remain excluded from version control.

## Project Structure

The exact package layout may vary, but the main components are organized around these responsibilities:

```text
certificate-details-extraction-API/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../
│   │   │       ├── CertificateController.java
│   │   │       ├── CertificateExtractionService.java
│   │   │       ├── DocumentTextExtractor.java
│   │   │       ├── CertificateFieldParser.java
│   │   │       ├── X509CertificateExtractor.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── samples/
├── logs/
├── pom.xml
├── .gitignore
└── README.md
```

- **CertificateController:** Exposes the REST endpoints.
- **CertificateExtractionService:** Coordinates format-specific extraction and certificate processing.
- **DocumentTextExtractor:** Extracts PDF text and performs OCR when required.
- **CertificateFieldParser:** Parses certificate fields from extracted text.
- **X509CertificateExtractor:** Processes X.509 SSL/TLS certificates.
- **GlobalExceptionHandler:** Converts supported application failures into structured error responses.

The tree is a conceptual guide; retain the actual package names and directories present in the repository.

## Sample Certificates and Tests

The repository includes synthetic sample certificates and automated tests for supported processing paths.

Useful test scenarios include:

- Valid certificates
- Expired certificates
- Missing or ambiguous expiry dates
- Explicitly non-expiring certificates
- PDF text extraction
- Image OCR
- X.509 certificate parsing
- Invalid Base64 input
- Unsupported file types
- Corrupted or unreadable files
- File size and signature validation

Run the full verification suite with:

```bash
mvn clean verify
```

Use synthetic or otherwise authorized certificate files for local testing. Avoid committing real private keys, sensitive credentials, or confidential certificate documents.

## Security and Limitations

- Input files are Base64-encoded in JSON; Base64 is not encryption.
- The decoded file size is limited to 10 MB.
- File type and signature validation help reject invalid input but do not make arbitrary files inherently safe.
- Certificate fields are extracted using text processing and OCR, so formatting, image quality, handwriting, or decorative fonts may affect accuracy.
- Fields that cannot be reliably extracted may be returned as `null`.
- The validity status is derived from the available certificate date information; it is not a full trust-chain, revocation, or hostname-verification assessment.
- Do not expose private keys or sensitive certificate contents in public repositories, logs, or issue reports.

## License

Add the license and ownership information applicable to this repository before publishing it for reuse.
