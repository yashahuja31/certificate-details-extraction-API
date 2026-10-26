package com.example.certificates.exception;

import com.example.certificates.model.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        LOGGER.warn("Request failed with API error; status={}, code={}", ex.getStatus().value(), ex.getCode());
        return ResponseEntity.status(ex.getStatus()).body(
                new ApiErrorResponse(false, ex.getMessage(), new ApiErrorResponse.ErrorDetail(ex.getCode())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(err -> err.getDefaultMessage()).orElse("Request validation failed");
        LOGGER.warn("Request validation failed");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(false, message,
                new ApiErrorResponse.ErrorDetail("INVALID_REQUEST")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableJson(HttpMessageNotReadableException ex) {
        LOGGER.warn("Request body could not be parsed as JSON");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(false, "Request body must be valid JSON",
                new ApiErrorResponse.ErrorDetail("INVALID_JSON")));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        LOGGER.warn("Request constraint validation failed");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(false, "Request validation failed",
                new ApiErrorResponse.ErrorDetail("INVALID_REQUEST")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        // Keep stack traces in server logs only; never send them or certificate contents to clients.
        LOGGER.error("Unexpected error while processing certificate API request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(false,
                "An unexpected error occurred while processing the certificate",
                new ApiErrorResponse.ErrorDetail("INTERNAL_SERVER_ERROR")));
    }
}
