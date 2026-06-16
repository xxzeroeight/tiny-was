package com.tinywas.server;

import com.tinywas.connection.HttpConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private final ServerConfig config;
    private volatile boolean running = false;

    public HttpServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort(), config.getBacklog())) {
            running = true;

            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(new HttpConnection(socket)).start();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
