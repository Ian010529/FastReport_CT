package com.ct.fastreport.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class WarningException extends BaseAppException {

    public WarningException(String code, String message, Map<String, Object> detail) {
        super("WARNING", code, message, detail, HttpStatus.CONFLICT);
    }
}
