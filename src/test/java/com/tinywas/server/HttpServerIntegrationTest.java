package com.tinywas.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpServerIntegrationTest {
    private static final int TEST_PORT = 18080;

    private HttpServer server;

    @BeforeEach
    void setUp() throws InterruptedException {
        ServerConfig config = ServerConfig.builder()
                .port(TEST_PORT)
                .build();
        server = new HttpServer(config);

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
                // 테스트 종료 시 소켓이 닫히며 발생하는 예외는 무시
            }
        });
        serverThread.setDaemon(true); // 테스트 프로세스 종료를 막지 않도록
        serverThread.start();

        Thread.sleep(200); // accept 루프 진입 대기
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void shouldRespondOkToGetRequest() throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT)) {
            OutputStream out = socket.getOutputStream();
            String request = "GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = in.readLine();
            assertEquals("HTTP/1.1 200 OK", statusLine);
        }
    }
}
