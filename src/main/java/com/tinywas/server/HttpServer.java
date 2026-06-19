package com.tinywas.server;

import com.tinywas.connection.HttpConnection;
import com.tinywas.handler.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private final ServerConfig config;
    private final Router router;
    private volatile boolean running = false;
    private volatile ServerSocket serverSocket;
    private final CountDownLatch startedSignal = new CountDownLatch(1);

    public HttpServer(ServerConfig config, Router router) {
        this.config = config;
        this.router = router;
    }

    public void start() throws IOException {
        try (ServerSocket ss = new ServerSocket(config.getPort(), config.getBacklog())) {
            this.serverSocket = ss;
            running = true;
            startedSignal.countDown();
            System.out.println("[tiny-was] Server started on port " + config.getPort());

            while (running) {
                try {
                    Socket socket = ss.accept();
                    new Thread(new HttpConnection(socket, router)).start();
                } catch (SocketException e) {
                    if (!running) break;
                    throw e;
                }
            }
        } finally {
            this.serverSocket = null;
        }
    }

    public void stop() {
        running = false;
        ServerSocket ss = this.serverSocket;
        if (ss != null && !ss.isClosed()) {
            try {
                ss.close();
            } catch (IOException ignored) {}
        }
    }

    public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
        return startedSignal.await(timeout, unit);
    }

    public int getPort() {
        ServerSocket ss = this.serverSocket;
        if (ss == null) {
            throw new IllegalStateException("Server is not running");
        }
        return ss.getLocalPort();
    }
}
