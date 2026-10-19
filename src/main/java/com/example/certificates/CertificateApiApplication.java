package com.example.certificates;

import com.example.certificates.client.CertificateCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class CertificateApiApplication {
    public static void main(String[] args) {
        // `--extract` runs the included Java CLI client instead of starting a second server.
        // Start the API in one terminal first, then use this mode in another terminal.
        if (args.length > 0 && "--extract".equals(args[0])) {
            int exitCode = CertificateCli.run(Arrays.copyOfRange(args, 1, args.length));
            if (exitCode != 0) System.exit(exitCode);
            return;
        }
        SpringApplication.run(CertificateApiApplication.class, args);
    }
}
