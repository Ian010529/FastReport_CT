package com.ct.fastreport.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public abstract class BaseAppException extends RuntimeException {

    private final String type;
    private final String code;
    private final Map<String, Object> detail;
    private final HttpStatus httpStatus;

    protected BaseAppException(String type,
                               String code,
                               String message,
                               Map<String, Object> detail,
                               HttpStatus httpStatus) {
        super(message);
        this.type = type;
        this.code = code;
        this.detail = detail;
        this.httpStatus = httpStatus;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
