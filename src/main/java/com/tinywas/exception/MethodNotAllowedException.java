package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

import java.util.Map;

public class MethodNotAllowedException extends HttpException {
    public MethodNotAllowedException(String message) {
        super(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", Map.of("allow", message));
    }
}
