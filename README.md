# Certificate Details Extraction API — Java + Spring Boot

A backend REST API that accepts a certificate as a Base64-encoded PDF, PNG, JPG, or JPEG and returns extracted fields as JSON. The implementation uses traditional PDF parsing, local Tesseract OCR, regex/label-based extraction, and calendar-date comparison. **No AI model or AI-based extraction service is used.**

This Java project is the Spring Boot counterpart of the Node.js version and includes a Java command-line client so you can submit an individual certificate from any local file path.

## Features

- `POST /api/certificates/extract` with the same request and response contract as the Node.js version.
- Supports PDF, PNG, JPG, and JPEG, passed as Base64 in JSON.
- Apache PDFBox extracts embedded PDF text; scanned PDFs are rendered and passed to Tesseract OCR.
- Tess4J/Tesseract performs OCR on image certificates.
- Rule-based field parsing for certificate number, holder name, certificate name, issuer, issue date, and expiry date.
- Status values: `valid`, `expired`, or `unknown`. Missing or ambiguous expiry dates are not guessed.
- File signature checks, a 10 MB decoded-file limit, structured error responses, and no full certificate/Base64 logging.
- Java CLI to submit an individual local file to the API.
- OpenAPI documentation, synthetic sample certificates, unit tests, and controller tests.

## Requirements

- JDK 21 (recommended; Java 17+ should work with this codebase)
- Apache Maven 3.9+
- Internet access on the first build so Maven can download project dependencies
- On Windows, if Tess4J reports native-library startup errors, install the current Microsoft Visual C++ Redistributable. OCR is local and may have native-runtime requirements depending on OS.

Verify your tools in PowerShell:

```powershell
java -version
mvn -v
```

If `mvn` is not recognized, install Apache Maven and add its `bin` directory to PATH before proceeding. The official Maven install guide is at https://maven.apache.org/install.html.

## Build and test

From the project root:

```powershell
mvn clean package
```

This compiles the application and runs the tests. The executable JAR is created at:

```text
target/certificate-details-extraction-api.jar
```

## Start the API

Terminal 1:

```powershell
java -jar target/certificate-details-extraction-api.jar
```

Expected startup message includes port `8080`.

Health checks:

```powershell
Invoke-RestMethod http://localhost:8080/health
Invoke-RestMethod http://localhost:8080/api/certificates/health
```

The root `/health` endpoint retains the `{ "success": true, "status": "ok" }` shape used by the Node.js version.

## Submit any individual certificate from a file path

Keep the server running in Terminal 1. Open Terminal 2 and go to the project root. Use the new Java CLI mode:

```powershell
java -jar target/certificate-details-extraction-api.jar --extract "C:\Users\comp\Documents\my-certificate.pdf"
```

Replace the example path with the real certificate path. Spaces in paths are supported when the path is quoted. The CLI reads the file, Base64-encodes it, sends a request to the Spring Boot API, and prints the HTTP status and JSON response. No Node.js or JavaScript script is needed.

Supported extensions: `.pdf`, `.png`, `.jpg`, `.jpeg`. The file extension selects `fileType` by default. If needed, override it explicitly:

```powershell
java -jar target/certificate-details-extraction-api.jar --extract "C:\Certificates\certificate-file" --type pdf
```

Change the API URL for a non-default host/port:

```powershell
java -jar target/certificate-details-extraction-api.jar --extract "C:\Certificates\my certificate.png" --url http://localhost:8080
```

You can also set a default URL for the CLI with:

```powershell
$env:CERTIFICATE_API_URL = "~`http://localhost:8080`~"
```

The CLI is only a client; it does not run extraction locally. The API server must be running first.

## Stringify the .pem file to Base64 string value 

```powershell

$path = (Resolve-Path ".\samples\CERTIFICATE-NAME.pem").Path //certificate name here depends on .pem file

$base64 = [Convert]::ToBase64String(
    [System.IO.File]::ReadAllBytes($path)
)

$body = @{
    certificate = $base64
    fileType = "pem"
} | ConvertTo-Json -Compress

Set-Clipboard -Value $body

Write-Host "Request Copied to Clipboard."
```

## Try the included sample certificates

```powershell
java -jar target/certificate-details-extraction-api.jar --extract "samples\valid-certificate.pdf"
java -jar target/certificate-details-extraction-api.jar --extract "samples\expired-certificate.pdf"
java -jar target/certificate-details-extraction-api.jar --extract "samples\missing-expiry-certificate.pdf"
java -jar target/certificate-details-extraction-api.jar --extract "samples\sample-certificate.png"
java -jar target/certificate-details-extraction-api.jar --extract "samples\corrupted.pdf"
```

All sample certificates contain synthetic, non-sensitive data. The PNG sample exercises OCR and may take longer than the embedded-text PDFs.

## API contract

### Request

`POST http://localhost:8080/api/certificates/extract`

Content-Type: `application/json`

```json
{
  "certificate": "BASE64_ENCODED_CERTIFICATE_STRING",
  "fileType": "pdf"
}
```

Supported values for `fileType`: `pdf`, `png`, `jpg`, `jpeg`. A Base64 data URL prefix such as `data:application/pdf;base64,...` is accepted too.

### Success response

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

These values are for illustration and match one synthetic sample. The backend does not return these fixed values for every request; it parses the uploaded content.

### Error response

```json
{
  "success": false,
  "message": "certificate is not valid Base64",
  "error": {
    "code": "INVALID_BASE64"
  }
}
```

Common status codes: `400` invalid input, Base64, unsupported type, or file/signature mismatch; `413` decoded file over 10 MB; `422` corrupted/unreadable file or OCR failure; `500` unexpected processing failure. Public errors do not expose stack traces, the input Base64, or the full certificate text.

## Expiry-status behavior

- `valid`: an identifiable expiry date is today or in the future.
- `expired`: an identifiable expiry date is before today's UTC calendar date.
- `unknown`: expiry is missing, ambiguous, or unreadable.
- A certificate is marked `valid` without an expiry date only when the extracted text explicitly establishes that it does not expire (for example, “This certificate does not expire”). A missing date by itself does not imply lifetime validity.
- Numeric dates such as `03/04/2027` are treated as ambiguous when month/day order cannot be determined safely; they are not guessed.

## Extraction approach

1. Validate required request fields and the allow-list for `fileType`.
2. Decode Base64 and enforce the 10 MB decoded-file limit.
3. Validate file signatures so a PNG renamed as a PDF is rejected.
4. PDFBox reads embedded PDF text. If there is little/no embedded text, PDFBox renders up to five pages at 220 DPI and Tess4J OCRs them. PNG/JPG/JPEG images go directly to Tess4J OCR.
5. Label and regex rules identify the certificate fields. Missing or unreliable fields are `null`.
6. Dates are normalized to ISO `YYYY-MM-DD`; the expiry date is compared with the current UTC calendar date.

## Project layout

```text
src/main/java/com/example/certificates/
  CertificateApiApplication.java        Spring Boot entry point + CLI dispatch
  api/CertificateController.java        POST extraction endpoint
  api/RootHealthController.java         GET /health
  client/CertificateCli.java            Submit a local file to running API
  model/                                Request and response records
  service/                              Decode, PDF/OCR, parsing and status logic
  exception/                            Structured API exception handling
src/test/java/                           JUnit unit and controller tests
src/test/resources/samples/              Synthetic fixtures for tests
samples/                                 Synthetic certificates for manual demos
docs/openapi.yaml                        OpenAPI 3.1 API documentation
docs/examples/                            JSON request/response examples
```

## Run tests separately

```powershell
mvn test
```

The tests cover valid and expired statuses, missing/ambiguous expiry, explicitly non-expiring wording, field parsing including concatenated OCR text, Base64 validation, file signature checks, corrupted/unreadable inputs, request validation, and the HTTP endpoint shape. OCR recognition quality should additionally be demonstrated locally against the included image fixture because native OCR behavior can vary by operating system.

## Limitations

Regex and OCR are not universal certificate-understanding systems. Unfamiliar layouts, low-quality scans, rotated images, non-English documents, handwriting, or inconsistent labels can cause fields to remain `null` or be extracted incorrectly. The implementation intentionally avoids inventing values. It does not verify a certificate's authenticity with the issuer or validate a digital signature; it extracts information and calculates an expiry status only.
