package com.ct.fastreport.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationError extends BaseAppException {

    public ValidationError(String code, String message) {
        this(code, message, Map.of());
    }

    public ValidationError(String code, String message, Map<String, Object> detail) {
        super("VALIDATION_ERROR", code, message, detail, HttpStatus.BAD_REQUEST);
    }
}
