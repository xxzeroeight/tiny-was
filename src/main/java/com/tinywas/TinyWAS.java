package com.tinywas;

import com.tinywas.handler.Router;
import com.tinywas.handler.StaticFileHandler;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpStatus;
import com.tinywas.server.HttpServer;
import com.tinywas.server.ServerConfig;

import java.io.IOException;

public class TinyWAS {
    public static void main(String[] args) throws IOException {
        ServerConfig config = ServerConfig.builder()
                .port(8080)
                .backlog(50)
                .staticRoot("static")
                .build();

        StaticFileHandler staticFileHandler = new StaticFileHandler(config.getStaticRoot());
        Router router = new Router(staticFileHandler);

        router.register(HttpMethod.GET, "/hello", request ->
                HttpResponse.builder(HttpStatus.OK)
                        .header("Content-Type", "text/plain")
                        .body("Hello from tiny-was! " + request.getMethod() + " " + request.getPath())
                        .build());

        HttpServer server = new HttpServer(config, router);
        server.start();
    }
}
