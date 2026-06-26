package com.tinywas.server;

import com.tinywas.handler.Router;
import com.tinywas.handler.StaticFileHandler;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeepAliveIntegrationTest {
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp(@TempDir Path tempStaticRoot) throws Exception {
        Files.writeString(tempStaticRoot.resolve("hello.txt"), "static content");

        StaticFileHandler staticFileHandler = new StaticFileHandler(tempStaticRoot);
        Router router = new Router(staticFileHandler);
        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK)
                        .header("Content-Type", "text/plain; charset=utf-8")
                        .body("hello".getBytes())
                        .build()
        );

        ServerConfig serverConfig = ServerConfig.builder().port(0).build();
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.builder().build();
        KeepAliveConfig keepAliveConfig = KeepAliveConfig.builder()
                .maxRequests(100)
                .timeoutMillis(5000)
                .build();

        server = new HttpServer(serverConfig, threadPoolConfig, keepAliveConfig, router);

        Thread serverThread = new Thread(() -> {
            try { server.start(); } catch (IOException ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(server.awaitStart(5, TimeUnit.SECONDS));
        port = server.getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("하나의 연결에서 여러 요청을 처리하면 응답 헤더에 Connection: keep-alive가 포함된다")
    void shouldKeepAliveOnMultipleRequests() throws Exception {
        // Java HttpClient는 기본적으로 HTTP/1.1 Keep-Alive를 사용
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        int requestCount = 5;
        for (int i = 0; i < requestCount; i++) {
            java.net.http.HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/hello"))
                            .GET()
                            .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(200, response.statusCode());
            assertEquals("keep-alive", response.headers().firstValue("connection").orElse(""));
        }
    }

    @Test
    @DisplayName("maxRequests 초과 시 마지막 응답 헤더에 Connection: close가 포함된다")
    void shouldCloseWhenMaxRequestsExceeded() throws Exception {
        KeepAliveConfig limitConfig = KeepAliveConfig.builder()
                .maxRequests(3)
                .timeoutMillis(5000)
                .build();

        StaticFileHandler staticFileHandler = new StaticFileHandler(Path.of("static"));
        Router router = new Router(staticFileHandler);
        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK)
                        .body("hello".getBytes())
                        .build()
        );

        ServerConfig serverConfig = ServerConfig.builder().port(0).build();
        HttpServer limitServer = new HttpServer(
                serverConfig,
                ThreadPoolConfig.builder().build(),
                limitConfig,
                router
        );

        Thread serverThread = new Thread(() -> {
            try { limitServer.start(); } catch (IOException ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        assertTrue(limitServer.awaitStart(5, TimeUnit.SECONDS));
        int limitPort = limitServer.getPort();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            java.net.http.HttpResponse<String> response = null;
            for (int i = 0; i < 3; i++) {
                response = client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:" + limitPort + "/hello"))
                                .GET()
                                .build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString()
                );
            }
            assertEquals("close", response.headers().firstValue("connection").orElse(""));
        } finally {
            limitServer.stop();
        }
    }

    @Test
    @DisplayName("요청 헤더에 Connection: close가 있으면 응답 헤더에 Connection: close가 포함된다")
    void shouldCloseWhenClientRequestsClose() throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Connection: close 헤더를 직접 작성
            String request =
                    "GET /hello HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean hasCloseHeader = false;
            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                if (line.equalsIgnoreCase("connection: close")) {
                    hasCloseHeader = true;
                }
            }

            assertTrue(hasCloseHeader, "응답 헤더에 Connection: close가 포함되어야 합니다");
        }
    }
}