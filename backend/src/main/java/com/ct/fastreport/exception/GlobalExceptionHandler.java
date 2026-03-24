package com.ct.fastreport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<AppErrorResponse> handleAppException(BaseAppException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new AppErrorResponse(
                        ex.getType(),
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getDetail(),
                        ex.getHttpStatus().value()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AppErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return fromValidation(
                "INVALID_REQUEST_BODY",
                "Request body is missing or malformed JSON.",
                Map.of("cause", safeMessage(ex))
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageConversionException.class
    })
    public ResponseEntity<AppErrorResponse> handleTypeMismatch(Exception ex) {
        return fromValidation(
                "INVALID_PARAMETER_TYPE",
                "A request parameter or field has the wrong type.",
                Map.of("cause", safeMessage(ex))
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<AppErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return fromValidation(
                "MISSING_REQUEST_PARAMETER",
                "A required request parameter is missing.",
                Map.of("parameter", ex.getParameterName())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()
                ))
                .toList());
        return fromValidation("METHOD_ARGUMENT_NOT_VALID", "Request validation failed.", detail);
    }

    private ResponseEntity<AppErrorResponse> fromValidation(String code,
                                                            String message,
                                                            Map<String, Object> detail) {
        ValidationError error = new ValidationError(code, message, detail);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AppErrorResponse(
                        error.getType(),
                        error.getCode(),
                        error.getMessage(),
                        error.getDetail(),
                        error.getHttpStatus().value()
                ));
    }

    private String safeMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : ex.getMessage();
    }
}
