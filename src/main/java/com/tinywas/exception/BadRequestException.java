package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

public class BadRequestException extends HttpException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
