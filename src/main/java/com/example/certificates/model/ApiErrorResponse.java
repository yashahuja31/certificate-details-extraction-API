package com.example.certificates.model;

public record ApiErrorResponse(boolean success, String message, ErrorDetail error) {
    public record ErrorDetail(String code) {}
}
