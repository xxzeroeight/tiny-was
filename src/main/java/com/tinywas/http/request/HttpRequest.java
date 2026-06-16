package com.tinywas.http.request;

import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;

import java.util.Map;

public class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final HttpVersion version;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(HttpMethod method, String path, HttpVersion version,
                       Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = Map.copyOf(headers);
        this.body = body;
    }

    public HttpMethod getMethod() { return method; }
    public String getPath() { return path; }
    public HttpVersion getVersion() { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }

    public String getHeader(String name) { return headers.get(name.toLowerCase()); }

    public String toString() {
        return method + " " + path + " " + version.getValue();
    }
}
