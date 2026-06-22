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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpServerIntegrationTest {
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp(@TempDir Path tempStaticRoot) throws InterruptedException, IOException {
        Files.writeString(tempStaticRoot.resolve("hello.txt"), "static content");

        ServerConfig config = ServerConfig.builder()
                .port(0)
                .staticRoot(tempStaticRoot.toString())
                .build();

        StaticFileHandler staticFileHandler = new StaticFileHandler(config.getStaticRoot());
        Router router = new Router(staticFileHandler);
        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK).body("Hello from tiny-was!").build());

        server = new HttpServer(config, router);

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        boolean started = server.awaitStart(2, TimeUnit.SECONDS);
        assertTrue(started, "서버가 제한 시간 내에 시작되지 않음");

        port = server.getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    @DisplayName("등록된 라우트로 요청하면 200 OK를 응답한다")
    void shouldRespondOkToRegisteredRoute() throws IOException {
        String statusLine = sendRequest("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n");
        assertEquals("HTTP/1.1 200 OK", statusLine);
    }

    @Test
    @DisplayName("등록된 라우트가 없으면 정적 파일을 서빙한다")
    void shouldServeStaticFileWhenNoRouteMatches() throws IOException {
        String statusLine = sendRequest("GET /hello.txt HTTP/1.1\r\nHost: localhost\r\n\r\n");
        assertEquals("HTTP/1.1 200 OK", statusLine);
    }

    @Test
    @DisplayName("라우트도 정적 파일도 없으면 404를 응답한다")
    void shouldReturn404WhenNothingMatches() throws IOException {
        String statusLine = sendRequest("GET /missing HTTP/1.1\r\nHost: localhost\r\n\r\n");
        assertEquals("HTTP/1.1 404 Not Found", statusLine);
    }

    private String sendRequest(String rawRequest) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            return in.readLine();
        }
    }
}
