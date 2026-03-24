package com.ct.fastreport.exception;

import java.util.Map;

public class AppErrorResponse {
    public final String type;
    public final String code;
    public final String message;
    public final Map<String, Object> detail;
    public final int httpStatus;

    public AppErrorResponse(String type,
                            String code,
                            String message,
                            Map<String, Object> detail,
                            int httpStatus) {
        this.type = type;
        this.code = code;
        this.message = message;
        this.detail = detail;
        this.httpStatus = httpStatus;
    }
}
