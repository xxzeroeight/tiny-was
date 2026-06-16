package com.tinywas.http.request;

import com.tinywas.exception.BadRequestException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {
    public HttpRequest parse(InputStream inputStream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(inputStream);

        // 1. 요청 라인 파싱
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isBlank()) {
            throw new BadRequestException("Request line is empty");
        }

        // ["GET", "/hello", "HTTP/1.1"]
        String[] tokens = requestLine.split(" ");
        if (tokens.length != 3) {
            throw new BadRequestException("Invalid request line: " + requestLine);
        }

        HttpMethod method = HttpMethod.fromString(tokens[0]);
        String path = tokens[1];
        HttpVersion version = HttpVersion.fromString(tokens[2]);

        // 2. 헤더 파싱
        Map<String, String> headers = parseHeaders(in);

        // 3. 바디 파싱
        String body = parseBody(in, headers.get("content-length"));

        return new HttpRequest(method, path, version, headers, body);
    }

    private Map<String, String> parseHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;

        while ((line = readLine(in)) != null && !line.isBlank()) {
            int colonIndex = line.indexOf(":");
            if (colonIndex == -1) {
                throw new BadRequestException("Invalid header format: " + line);
            }
            String key = line.substring(0, colonIndex).trim().toLowerCase();
            String value = line.substring(colonIndex + 1).trim();
            headers.put(key, value);
        }

        return headers;
    }

    private String parseBody(InputStream in, String contentLength) throws IOException {
        if (contentLength == null) return "";

        int length;
        try {
            length = Integer.parseInt(contentLength);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid content length: " + contentLength);
        }

        if (length < 0) throw new BadRequestException("Content length must not be negative");

        byte[] bodyBytes = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int result = in.read(bodyBytes, totalRead, length - totalRead);
            if (result == -1) {
                throw new BadRequestException("Unexpected end of stream");
            }
            totalRead += result;
        }

        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') out.write(b);
        }

        if (b == -1 && out.size() == 0) return null;

        return out.toString(StandardCharsets.ISO_8859_1);
    }
}
