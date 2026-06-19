package com.tinywas.http.response;

import com.tinywas.http.HttpVersion;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
    private final HttpVersion version;
    private final HttpStatus status;
    private final Map<String, String> headers;
    private final byte[] body;

    private HttpResponse(Builder builder) {
        this.version = builder.version;
        this.status = builder.status;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        this.body = builder.body;
    }

    public HttpVersion getVersion() { return version; }
    public HttpStatus getStatus() { return status; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    public static Builder builder(HttpStatus status) {
        return new Builder(status);
    }

    public static class Builder {
        private HttpVersion version = HttpVersion.HTTP_1_1;
        private final HttpStatus status;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private byte[] body = new byte[0];

        private Builder(HttpStatus status) {
            this.status = status;
        }

        public Builder version(HttpVersion version) {
            this.version = version;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key.toLowerCase(), value);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            this.headers.put("content-length", String.valueOf(body.length));
            return this;
        }

        public Builder body(String body) {
            return body(body.getBytes(StandardCharsets.UTF_8));
        }

        public HttpResponse build() {
            headers.putIfAbsent("connection", "close");
            return new HttpResponse(this);
        }
    }
}
