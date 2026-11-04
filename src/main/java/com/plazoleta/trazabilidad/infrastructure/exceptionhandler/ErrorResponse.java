package com.plazoleta.trazabilidad.infrastructure.exceptionhandler;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, List<String> details) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return of(status, message, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, List<String> details) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, details);
    }
}
