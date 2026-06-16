package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

public class HttpException extends RuntimeException {
    private final HttpStatus status;

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
