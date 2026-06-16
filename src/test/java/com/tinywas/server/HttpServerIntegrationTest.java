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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpServerIntegrationTest {
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws InterruptedException {
        ServerConfig config = ServerConfig.builder()
                .port(0)
                .build();
        server = new HttpServer(config);

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
    void shouldRespondOkToGetRequest() throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
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
