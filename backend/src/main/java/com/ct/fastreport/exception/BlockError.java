package com.ct.fastreport.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BlockError extends BaseAppException {

    public BlockError(String code, String message) {
        this(code, message, Map.of());
    }

    public BlockError(String code, String message, Map<String, Object> detail) {
        super("BLOCK", code, message, detail, HttpStatus.CONFLICT);
    }
}
