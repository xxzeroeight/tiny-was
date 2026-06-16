package com.tinywas.connection;

import com.tinywas.exception.HttpException;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.request.HttpRequestParser;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpResponseWriter;
import com.tinywas.http.response.HttpStatus;

import java.io.IOException;
import java.net.Socket;

public class HttpConnection implements Runnable {
    private final Socket socket;
    private final HttpRequestParser parser;
    private final HttpResponseWriter writer;

    public HttpConnection(Socket socket) {
        this.socket = socket;
        this.parser = new HttpRequestParser();
        this.writer = new HttpResponseWriter();
    }

    @Override
    public void run() {
        try (socket) {
            HttpRequest request = parser.parse(socket.getInputStream());
            HttpResponse response = handle(request);
            writer.write(response, socket.getOutputStream());
        } catch (HttpException e) {
            sendErrorResponse(e.getStatus());
        } catch (IOException e) {
            sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpResponse handle(HttpRequest request) {
        return HttpResponse.builder(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body("Hello from tiny-was! " + request.getMethod() + " " + request.getPath())
                .build();
    }

    private void sendErrorResponse(HttpStatus status) {
        try {
            HttpResponse response = HttpResponse.builder(status).build();
            writer.write(response, socket.getOutputStream());
        } catch (IOException ignored) {}
    }
}
