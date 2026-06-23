package com.tinywas.server;

import com.tinywas.connection.HttpConnection;
import com.tinywas.handler.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private final ServerConfig config;
    private final ThreadPoolConfig threadPoolConfig;
    private final Router router;

    private volatile boolean running = false;
    private volatile ServerSocket serverSocket;
    private volatile ExecutorService executor;

    private final CountDownLatch startedSignal = new CountDownLatch(1);

    public HttpServer(ServerConfig config, ThreadPoolConfig threadPoolConfig, Router router) {
        this.config = config;
        this.threadPoolConfig = threadPoolConfig;
        this.router = router;
    }

    public void start() throws IOException {
        executor = Executors.newFixedThreadPool(threadPoolConfig.getCorePoolSize());

        try (ServerSocket ss = new ServerSocket(config.getPort(), config.getBacklog())) {
            this.serverSocket = ss;
            running = true;
            startedSignal.countDown();
            System.out.println("[tiny-was] Server started on port " + config.getPort());

            while (running) {
                try {
                    Socket socket = ss.accept();
                    executor.submit(new HttpConnection(socket, router));
                } catch (SocketException e) {
                    if (!running) break;
                    throw e;
                }
            }
        } finally {
            this.serverSocket = null;
            shutdownExecutor();
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

    private void shutdownExecutor() {
        ExecutorService es = this.executor;
        if (es == null) return;

        es.shutdown();
        try {
            if (!es.awaitTermination(threadPoolConfig.getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException e) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
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
