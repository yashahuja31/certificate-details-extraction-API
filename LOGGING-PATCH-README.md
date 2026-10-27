# Certificate Details Extraction API — Logging Update

This patch adds non-sensitive SLF4J/Logback logging to the current X.509-enabled source tree.

## Files included (replace these files in your project)
1. `src/main/java/com/example/certificates/api/CertificateController.java`
2. `src/main/java/com/example/certificates/service/CertificateExtractionService.java`
3. `src/main/java/com/example/certificates/service/DocumentTextExtractor.java`
4. `src/main/java/com/example/certificates/service/CertificateFieldParser.java`
5. `src/main/java/com/example/certificates/service/X509CertificateExtractor.java`
6. `src/main/java/com/example/certificates/exception/GlobalExceptionHandler.java`
7. `src/main/resources/application.properties`

## Apply
1. Stop the running Spring Boot server with Ctrl+C.
2. Back up or commit your current changes first.
3. Extract this ZIP at the project root (the folder containing `pom.xml`) and allow the listed files to overwrite matching files.
4. Confirm `pom.xml` still has your springdoc dependency and the project uses Java 25.
5. Run `mvn clean verify`.
6. Start the server again. If you need OCR on Windows, make sure `TESSDATA_PREFIX` is set to the tessdata directory in the same terminal, or set `-Dapp.ocr.tessdata-path="C:/Program Files/Tesseract-OCR/tessdata"` when starting Java.

## Logging and privacy
- Logs record stages, file types, byte/character counts, statuses, error codes and stack traces for unexpected/internal OCR failures.
- The patch does not log Base64 contents, extracted certificate text, holder names, domains, or serial numbers.
- Logs write to the console and `logs/certificate-api.log`. The existing `.gitignore` ignores `*.log`.
- `DEBUG` is enabled for the application package to help local debugging. Consider changing to `INFO` before a production deployment.

This is an overlay patch: it intentionally does not include `pom.xml` or modify your Swagger dependency.
