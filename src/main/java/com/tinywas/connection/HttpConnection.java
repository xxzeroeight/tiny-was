package com.tinywas.connection;

import com.tinywas.exception.HttpException;
import com.tinywas.handler.Router;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.request.HttpRequestParser;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpResponseWriter;
import com.tinywas.http.response.HttpStatus;

import java.io.IOException;
import java.net.Socket;

public class HttpConnection implements Runnable {
    private final Socket socket;
    private final Router router;
    private final HttpRequestParser parser;
    private final HttpResponseWriter writer;

    public HttpConnection(Socket socket, Router router) {
        this.socket = socket;
        this.router = router;
        this.parser = new HttpRequestParser();
        this.writer = new HttpResponseWriter();
    }

    @Override
    public void run() {
        try {
            HttpRequest request = parser.parse(socket.getInputStream());
            HttpResponse response = router.route(request);
            writer.write(response, socket.getOutputStream());
        } catch (HttpException e) {
            sendErrorResponse(e);
        } catch (IOException | RuntimeException e) {
            sendErrorResponse(new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
        } finally {
            closeQuietly();
        }
    }

    private void sendErrorResponse(HttpException e) {
        try {
            HttpResponse.Builder builder = HttpResponse.builder(e.getStatus());
            e.getHeaders().forEach(builder::header);
            writer.write(builder.build(), socket.getOutputStream());
        } catch (IOException ignored) {}
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
