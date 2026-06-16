package com.tinywas.http.request;

import com.tinywas.exception.BadRequestException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRequestParserTest {
    private HttpRequestParser parser;

    @BeforeEach
    void setUp() {
        parser = new HttpRequestParser();
    }

    private InputStream toInputStream(String raw) {
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("GET 요청을 정상적으로 파싱한다.")
    void parseGetRequest() throws IOException {
        String raw = "GET /index.html HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "\r\n";

        HttpRequest request = parser.parse(toInputStream(raw));

        assertAll(
                () -> assertEquals(HttpMethod.GET, request.getMethod()),
                () -> assertEquals("/index.html", request.getPath()),
                () -> assertEquals(HttpVersion.HTTP_1_1, request.getVersion()),
                () -> assertEquals("localhost:8080", request.getHeader("host"))
        );
    }

    @Test
    @DisplayName("POST 요청의 바디를 정상적으로 파싱한다")
    void parsePostRequestWithBody() throws IOException {
        String body = "Hello World";
        String raw = "POST /submit HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;

        HttpRequest request = parser.parse(toInputStream(raw));

        assertAll(
                () -> assertEquals(HttpMethod.POST, request.getMethod()),
                () -> assertEquals(body, request.getBody())
        );
    }

    @Test
    @DisplayName("요청 라인이 비어있으면 BadRequestException을 던진다")
    void throwExceptionWhenRequestLineIsEmpty() {
        String raw = "\r\n";
        assertThrows(BadRequestException.class,
                () -> parser.parse(toInputStream(raw)));
    }

    @Test
    @DisplayName("요청 라인 형식이 잘못되면 BadRequestException을 던진다")
    void throwExceptionWhenRequestLineIsInvalid() {
        String raw = "GET /index.html\r\n\r\n";
        assertThrows(BadRequestException.class,
                () -> parser.parse(toInputStream(raw)));
    }

    @Test
    @DisplayName("헤더 형식이 잘못되면 BadRequestException을 던진다")
    void throwExceptionWhenHeaderFormatIsInvalid() {
        String raw = "GET /index.html HTTP/1.1\r\n" +
                "InvalidHeader\r\n" +
                "\r\n";
        assertThrows(BadRequestException.class,
                () -> parser.parse(toInputStream(raw)));
    }

    @Test
    @DisplayName("헤더 키는 소문자로 저장된다")
    void headerKeyIsLowerCase() throws IOException {
        String raw = "GET / HTTP/1.1\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";

        HttpRequest request = parser.parse(toInputStream(raw));

        assertEquals("text/html", request.getHeader("content-type"));
    }

    @Test
    @DisplayName("Content-Length는 바이트 수 기준으로 멀티바이트 문자를 정확히 파싱한다")
    void parseBodyWithMultiByteCharacters() throws IOException {
        String body = "안녕"; // UTF-8로 6바이트, 2글자
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String raw = "POST /submit HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "\r\n" + body;

        HttpRequest request = parser.parse(toInputStream(raw));

        assertEquals(body, request.getBody());
    }

    @Test
    @DisplayName("Content-Length가 숫자가 아니면 BadRequestException을 던진다")
    void throwExceptionWhenContentLengthIsNotNumber() {
        String raw = "POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: abc\r\n\r\n";
        assertThrows(BadRequestException.class, () -> parser.parse(toInputStream(raw)));
    }

    @Test
    @DisplayName("Content-Length가 음수이면 BadRequestException을 던진다")
    void throwExceptionWhenContentLengthIsNegative() {
        String raw = "POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: -5\r\n\r\n";
        assertThrows(BadRequestException.class, () -> parser.parse(toInputStream(raw)));
    }

    @Test
    @DisplayName("선언된 Content-Length보다 적은 데이터가 오면 BadRequestException을 던진다")
    void throwExceptionWhenBodyIsShorterThanContentLength() {
        String raw = "POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: 100\r\n\r\nshort";
        assertThrows(BadRequestException.class, () -> parser.parse(toInputStream(raw)));
    }
}