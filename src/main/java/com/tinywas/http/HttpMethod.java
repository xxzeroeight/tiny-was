package com.tinywas.http;

import com.tinywas.exception.BadRequestException;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    OPTIONS,
    PATCH;

    public static HttpMethod fromString(String value){
        try {
            return HttpMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported HTTP method: " + value);
        }
    }
}
