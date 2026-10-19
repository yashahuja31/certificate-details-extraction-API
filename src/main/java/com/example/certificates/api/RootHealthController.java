package com.example.certificates.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Root health endpoint kept for parity with the original Node.js project. */
@RestController
public class RootHealthController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("success", true, "status", "ok");
    }
}
