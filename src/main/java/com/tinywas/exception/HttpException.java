package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

import java.util.Objects;

public class HttpException extends RuntimeException {
    private final HttpStatus status;

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public HttpStatus getStatus() {
        return status;
    }
}
