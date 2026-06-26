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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpServerIntegrationTest {
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp(@TempDir Path tempStaticRoot) throws Exception {
        Files.writeString(tempStaticRoot.resolve("hello.txt"), "static content");

        StaticFileHandler staticFileHandler = new StaticFileHandler(tempStaticRoot);
        Router router = new Router(staticFileHandler);
        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK)
                        .body("hello".getBytes())
                        .build()
        );

        ServerConfig serverConfig = ServerConfig.builder().port(0).build();
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.builder().corePoolSize(50).build();
        KeepAliveConfig keepAliveConfig = KeepAliveConfig.builder().build();
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
    @DisplayName("동시 요청 100개를 모두 처리하면 200 응답을 반환한다")
    void concurrentRequestsAllReturn200() throws Exception {
        int concurrency = 100;

        ExecutorService clientExecutor = Executors.newFixedThreadPool(concurrency);
        ExecutorService testPool = Executors.newFixedThreadPool(concurrency);
        HttpClient client = HttpClient.newBuilder()
                .executor(clientExecutor)
                .build();

        List<Future<java.net.http.HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            futures.add(testPool.submit(() ->
                    client.send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create("http://localhost:" + port + "/hello"))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString()
                    )
            ));
        }

        int successCount = 0;
        for (Future<java.net.http.HttpResponse<String>> f : futures) {
            java.net.http.HttpResponse<String> response = f.get(10, TimeUnit.SECONDS);
            if (response.statusCode() == 200) successCount++;
        }

        testPool.shutdown();
        clientExecutor.shutdown();
        assertTrue(testPool.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(clientExecutor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(concurrency, successCount, "모든 동시 요청이 200 응답을 받아야 합니다");
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
