package com.tinywas.exception;

import com.tinywas.http.response.HttpStatus;

public class NotFoundException extends HttpException{
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
