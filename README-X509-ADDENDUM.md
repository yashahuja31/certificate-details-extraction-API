# X.509 SSL/TLS PEM support (README addition)

Append this section to the main project `README.md` so the repository documentation matches the new functionality.

## SSL/TLS certificate files

In addition to PDF/image award certificates, the API can parse X.509 certificate files supplied as PEM, CRT, CER, or DER using Java's built-in `CertificateFactory`. X.509 files are parsed directly; OCR is not used.

```powershell
# Start the API in one terminal
java -jar .\target\certificate-details-extraction-api.jar --server.port=8080

# In a second terminal, submit a PEM certificate
java -jar .\target\certificate-details-extraction-api.jar --extract ".\samples\domain-certificate.pem" --url http://localhost:8080
```

The request still uses the existing Base64 file contract: the CLI Base64-encodes the whole PEM file, then sends it with `fileType: "pem"`.

For X.509 results, `certificateType` is `ssl_tls`. The `x509` object exposes the serial number, full subject, DNS Subject Alternative Names, exact validity start/end timestamps, and signature algorithm. The common `certificateName` field contains the subject common name (usually the domain), `expiryDate` is the UTC date portion of `notAfter`, and `status` is derived from `notBefore`/`notAfter`. A human `holderName` and separate `issueDate` are returned as null because those fields are not generally encoded as such in a domain certificate.

This is metadata extraction and validity-window evaluation, not full TLS trust validation. The API does not currently build a chain to trusted roots, perform revocation checking, or validate a certificate against a hostname during a TLS handshake. The included `domain-certificate.pem` is a synthetic self-signed sample; its private key is not included and it must not be used in production.
