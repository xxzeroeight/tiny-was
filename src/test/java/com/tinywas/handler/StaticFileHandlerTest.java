package com.tinywas.handler;

import com.tinywas.exception.NotFoundException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.response.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticFileHandlerTest {
    private Path staticRoot;
    private StaticFileHandler handler;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        staticRoot = tempDir;
        Files.writeString(staticRoot.resolve("index.html"), "<h1>Hello</h1>");
        Files.writeString(staticRoot.resolve("style.css"), "body { color: red; }");
        handler = new StaticFileHandler(staticRoot);
    }

    private HttpRequest request(String path) {
        return new HttpRequest(HttpMethod.GET, path, HttpVersion.HTTP_1_1, Collections.emptyMap(), "");
    }

    @Test
    @DisplayName("존재하는 파일을 정상적으로 읽어 응답한다")
    void serveExistingFile() throws IOException {
        HttpResponse response = handler.handle(request("/style.css"));

        assertArrayEquals("body { color: red; }".getBytes(StandardCharsets.UTF_8), response.getBody());
        assertEquals("text/css", response.getHeaders().get("content-type"));
    }

    @Test
    @DisplayName("루트 경로(/) 요청 시 index.html을 서빙한다")
    void serveIndexHtmlForRootPath() throws IOException {
        HttpResponse response = handler.handle(request("/"));

        assertArrayEquals("<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8), response.getBody());
        assertEquals("text/html", response.getHeaders().get("content-type"));
    }

    @Test
    @DisplayName("존재하지 않는 파일이면 NotFoundException을 던진다")
    void throwExceptionWhenFileDoesNotExist() {
        assertThrows(NotFoundException.class, () -> handler.handle(request("/missing.html")));
    }

    @Test
    @DisplayName("path traversal 시도는 NotFoundException으로 차단한다 (정보 노출 방지)")
    void blockPathTraversal() {
        assertThrows(NotFoundException.class, () -> handler.handle(request("/../../etc/passwd")));
    }

    @Test
    @DisplayName("확장자가 없는 파일은 기본 content-type을 반환한다")
    void defaultContentTypeForUnknownExtension() throws IOException {
        Files.writeString(staticRoot.resolve("data"), "raw");

        HttpResponse response = handler.handle(request("/data"));

        assertEquals("application/octet-stream", response.getHeaders().get("content-type"));
    }
}