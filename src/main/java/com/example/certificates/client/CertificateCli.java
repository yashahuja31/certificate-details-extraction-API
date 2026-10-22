package com.example.certificates.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * Small JDK-only command-line client for submitting one local certificate to the running API.
 * It intentionally does not parse certificate fields itself: extraction stays in the backend.
 */
public final class CertificateCli {
    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "png", "jpg", "jpeg", "pem", "crt", "cer", "der");

    private CertificateCli() {}

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) System.exit(exitCode);
    }

    public static int run(String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            return args.length == 0 ? 2 : 0;
        }

        Path file = Path.of(args[0]);
        String fileType = null;
        String baseUrl = System.getenv().getOrDefault("CERTIFICATE_API_URL", "http://localhost:8080");

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--type" -> {
                    if (++i >= args.length) {
                        System.err.println("Missing value for --type.");
                        printUsage();
                        return 2;
                    }
                    fileType = args[i];
                }
                case "--url" -> {
                    if (++i >= args.length) {
                        System.err.println("Missing value for --url.");
                        printUsage();
                        return 2;
                    }
                    baseUrl = args[i];
                }
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    return 2;
                }
            }
        }

        if (fileType == null || fileType.isBlank()) {
            String name = file.getFileName() == null ? "" : file.getFileName().toString();
            int dot = name.lastIndexOf('.');
            fileType = dot >= 0 ? name.substring(dot + 1) : "";
        }
        fileType = fileType.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        if (!SUPPORTED_TYPES.contains(fileType)) {
            System.err.println("Unsupported file type. Use pdf, png, jpg, jpeg, pem, crt, cer, or der (or pass --type).");
            return 2;
        }

        final byte[] bytes;
        try {
            if (!Files.isRegularFile(file)) {
                System.err.println("Certificate path is not a file: " + file.toAbsolutePath());
                return 2;
            }
            if (Files.size(file) > 10L * 1024 * 1024) {
                System.err.println("Certificate files must be 10 MB or smaller.");
                return 2;
            }
            bytes = Files.readAllBytes(file);
        } catch (IOException ex) {
            System.err.println("Could not read certificate file: " + ex.getMessage());
            return 2;
        }
        if (bytes.length == 0) {
            System.err.println("Certificate file is empty.");
            return 2;
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);
        // Standard Base64 contains no JSON-special quote or backslash characters.
        String requestJson = "{\"certificate\":\"" + base64 + "\",\"fileType\":\"" + fileType + "\"}";
        String endpoint = baseUrl.replaceAll("/+$", "") + "/api/certificates/extract";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("HTTP " + response.statusCode());
            System.out.println(response.body());
            return response.statusCode() >= 200 && response.statusCode() < 300 ? 0 : 1;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted.");
            return 1;
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid API URL: " + baseUrl);
            return 2;
        } catch (IOException ex) {
            System.err.println("Could not connect to the certificate API at " + endpoint + ": " + ex.getMessage());
            return 1;
        }
    }

    private static boolean isHelp(String argument) {
        return "--help".equals(argument) || "-h".equals(argument);
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar target/certificate-details-extraction-api.jar --extract <path> [--type pdf|png|jpg|jpeg|pem|crt|cer|der] [--url http://localhost:8080]");
        System.out.println("Example: java -jar target/certificate-details-extraction-api.jar --extract \"C:\\Certificates\\my certificate.pdf\"");
        System.out.println("If --type is omitted, the file extension is used. CERTIFICATE_API_URL can override the default API URL.");
    }
}
