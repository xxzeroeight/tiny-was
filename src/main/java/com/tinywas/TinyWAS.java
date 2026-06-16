package com.tinywas;

import com.tinywas.server.HttpServer;
import com.tinywas.server.ServerConfig;

import java.io.IOException;

public class TinyWAS {
    public static void main(String[] args) throws IOException {
        ServerConfig config = ServerConfig.builder()
                .port(8080)
                .backlog(50)
                .build();

        HttpServer server = new HttpServer(config);

        server.start();
    }
}
