package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class HttpException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, String> headers;

    public HttpException(HttpStatus status, String message) {
        this(status, message, Collections.emptyMap());
    }

    public HttpException(HttpStatus status, String message, Map<String, String> headers) {
        super(message);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.headers = Map.copyOf(Objects.requireNonNull(headers, "headers must not be null"));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
