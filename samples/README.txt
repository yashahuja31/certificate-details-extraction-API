All sample certificate files should contain synthetic, non-sensitive data and are for local testing only.
valid-certificate.pdf: future expiry date; expected status valid
expired-certificate.pdf: past expiry date; expected status expired
missing-expiry-certificate.pdf and no-expiry-certificate.pdf: no expiry date; expected status unknown
explicit-no-expiry-certificate.pdf: explicitly states that it does not expire
missing-fields-certificate.pdf: several optional fields are missing
sample-certificate.png: high-contrast image to demonstrate the OCR path
corrupted.pdf: deliberately unreadable PDF
domain-certificate.pem: synthetic self-signed X.509 SSL/TLS certificate; demonstrates direct certificate parsing, serial number, subject, DNS names, issuer and validity timestamps. Its private key is not included.
