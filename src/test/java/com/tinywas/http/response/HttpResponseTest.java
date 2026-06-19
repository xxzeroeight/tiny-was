package com.tinywas.http.response;

import com.tinywas.http.HttpVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpResponseTest {
    @Test
    @DisplayName("200 OK 응답을 정상적으로 생성한다")
    void buildOkResponse() {
        HttpResponse response = HttpResponse.builder(HttpStatus.OK)
                .header("Content-Type", "text/html")
                .body("Hello, World!")
                .build();

        assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatus()),
                () -> assertEquals(HttpVersion.HTTP_1_1, response.getVersion()),
                () -> assertArrayEquals("Hello, World!".getBytes(StandardCharsets.UTF_8), response.getBody()),
                () -> assertEquals("13", response.getHeaders().get("content-length")),
                () -> assertEquals("close", response.getHeaders().get("connection"))
        );
    }

    @Test
    @DisplayName("응답을 HTTP 형식 문자열로 직렬화한다")
    void writeResponse() throws IOException {
        HttpResponse response = HttpResponse.builder(HttpStatus.OK)
                .header("Content-Type", "text/html")
                .body("Hello!")
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new HttpResponseWriter().write(response, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);

        assertAll(
                () -> assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n")),
                () -> assertTrue(result.contains("content-type: text/html\r\n")),
                () -> assertTrue(result.contains("content-length: 6\r\n")),
                () -> assertTrue(result.contains("connection: close\r\n")),
                () -> assertTrue(result.endsWith("Hello!"))
        );
    }

    @Test
    @DisplayName("바디가 없는 응답도 정상적으로 직렬화한다")
    void writeResponseWithoutBody() throws IOException {
        HttpResponse response = HttpResponse.builder(HttpStatus.NOT_FOUND)
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new HttpResponseWriter().write(response, outputStream);
        String result = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(result.startsWith("HTTP/1.1 404 Not Found\r\n"));
    }
}
