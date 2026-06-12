package com.tinywas.server;

public class TinyWAS {
    private final int port;

    public TinyWAS(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("TinyWAS started on port " + port);
    }

    public static void main(String[] args) {
        int port = 8080;
        TinyWAS server = new TinyWAS(port);
        server.start();
    }
}
