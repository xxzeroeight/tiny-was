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

        server = buildServer(tempStaticRoot, KeepAliveConfig.builder().build(), router);
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
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            for (int i = 0; i < 5; i++) {
                String request =
                        "GET /hello HTTP/1.1\r\n" +
                                "Host: localhost\r\n" +
                                "Connection: keep-alive\r\n" +
                                "\r\n";
                out.write(request.getBytes(StandardCharsets.UTF_8));
                out.flush();

                String connectionHeader = readConnectionHeader(in);
                assertEquals("keep-alive", connectionHeader,
                        (i + 1) + "번째 요청 응답에 Connection: keep-alive가 포함되어야 합니다");
            }
        }
    }

    @Test
    @DisplayName("maxRequests 초과 시 마지막 응답 헤더에 Connection: close가 포함된다")
    void shouldCloseWhenMaxRequestsExceeded(@TempDir Path tempStaticRoot) throws Exception {
        Files.writeString(tempStaticRoot.resolve("hello.txt"), "static content");

        StaticFileHandler staticFileHandler = new StaticFileHandler(tempStaticRoot);
        Router router = new Router(staticFileHandler);
        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK)
                        .body("hello".getBytes())
                        .build()
        );

        KeepAliveConfig limitConfig = KeepAliveConfig.builder()
                .maxRequests(3)
                .timeoutMillis(5000)
                .build();

        HttpServer limitServer = buildServer(tempStaticRoot, limitConfig, router);

        try {
            assertTrue(limitServer.awaitStart(5, TimeUnit.SECONDS));
            int limitPort = limitServer.getPort();

            try (Socket socket = new Socket("localhost", limitPort)) {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                String connectionHeader = null;
                for (int i = 0; i < 3; i++) {
                    String request =
                            "GET /hello HTTP/1.1\r\n" +
                                    "Host: localhost\r\n" +
                                    "Connection: keep-alive\r\n" +
                                    "\r\n";
                    out.write(request.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    connectionHeader = readConnectionHeader(in);
                }
                assertEquals("close", connectionHeader,
                        "마지막 요청 응답에 Connection: close가 포함되어야 합니다");
            }
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

            String request =
                    "GET /hello HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            assertEquals("close", readConnectionHeader(in),
                    "응답 헤더에 Connection: close가 포함되어야 합니다");
        }
    }

    private String readConnectionHeader(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String connectionHeader = "";
        String line;
        while ((line = reader.readLine()) != null && !line.isBlank()) {
            if (line.toLowerCase().startsWith("connection:")) {
                connectionHeader = line.substring("connection:".length()).trim();
            }
        }
        return connectionHeader;
    }

    private HttpServer buildServer(Path staticRoot, KeepAliveConfig keepAliveConfig, Router router) {
        ServerConfig serverConfig = ServerConfig.builder().port(0).build();
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.builder().build();
        HttpServer httpServer = new HttpServer(serverConfig, threadPoolConfig, keepAliveConfig, router);

        Thread serverThread = new Thread(() -> {
            try { httpServer.start(); } catch (IOException ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        return httpServer;
    }
}