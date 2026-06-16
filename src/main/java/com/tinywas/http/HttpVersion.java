package com.tinywas.http;

import com.tinywas.exception.BadRequestException;

public enum HttpVersion {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1");

    private final String value;

    HttpVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HttpVersion fromString(String value) {
        for (HttpVersion version : values()) {
            if (version.value.equals(value)) return version;
        }

        throw new BadRequestException("Unsupported HTTP version: " + value);
    }
}
