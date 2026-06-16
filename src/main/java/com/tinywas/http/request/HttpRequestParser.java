package com.tinywas.http.request;

import com.tinywas.exception.BadRequestException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {
    public HttpRequest parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // 1. 요청 라인 파싱
        String requestLine = reader.readLine();
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
        Map<String, String> headers = parseHeaders(reader);

        // 3. 바디 파싱
        String body = parseBody(reader, headers.get("content-length"));

        return new HttpRequest(method, path, version, headers, body);
    }

    private Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;

        while ((line = reader.readLine()) != null && !line.isBlank()) {
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

    private String parseBody(BufferedReader reader, String contentLength) throws IOException {
        if (contentLength == null) return "";

        int length = Integer.parseInt(contentLength);
        char[] bodyChars = new char[length];
        int totalRead = 0;

        while (totalRead < length) {
            int result = reader.read(bodyChars, totalRead, length - totalRead);
            if (result == -1) break;
            totalRead += result;
        }

        return new String(bodyChars, 0, totalRead);
    }
}
