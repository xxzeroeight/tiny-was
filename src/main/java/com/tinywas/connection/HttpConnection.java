package com.tinywas.connection;

import com.tinywas.exception.HttpException;
import com.tinywas.handler.Router;
import com.tinywas.http.HttpVersion;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.request.HttpRequestParser;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpResponseWriter;
import com.tinywas.http.response.HttpStatus;
import com.tinywas.server.KeepAliveConfig;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class HttpConnection implements Runnable {
    private final Socket socket;
    private final Router router;
    private final KeepAliveConfig keepAliveConfig;
    private final HttpRequestParser parser;
    private final HttpResponseWriter writer;

    public HttpConnection(Socket socket, Router router, KeepAliveConfig keepAliveConfig) {
        this.socket = socket;
        this.router = router;
        this.keepAliveConfig = keepAliveConfig;
        this.parser = new HttpRequestParser();
        this.writer = new HttpResponseWriter();
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(keepAliveConfig.getTimeoutMillis());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            int requestCount = 0;
            while (requestCount < keepAliveConfig.getMaxRequests()) {
                HttpRequest request;
                try {
                    request = parser.parse(in);
                } catch (SocketTimeoutException e) {
                    break;
                }

                requestCount++;
                boolean isLastRequest = requestCount >= keepAliveConfig.getMaxRequests();
                boolean keepAlive = !isLastRequest && isKeepAlive(request);

                HttpResponse response = router.route(request);
                HttpResponse finalResponse = applyConnectionHeader(response, keepAlive);
                writer.write(finalResponse, socket.getOutputStream());

                if (!keepAlive) break;
            }
        } catch (HttpException e) {
            sendErrorResponse(e);
        } catch (IOException | RuntimeException e) {
            sendErrorResponse(new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
        } finally {
            closeQuietly();
        }
    }

    private boolean isKeepAlive(HttpRequest request) {
        String connection = request.getHeader("connection");
        if (connection != null) {
            return Arrays.stream(connection.split(","))
                    .map(String::trim)
                    .noneMatch(token -> token.equalsIgnoreCase("close"));
        }

        return request.getVersion() == HttpVersion.HTTP_1_1;
    }

    private HttpResponse applyConnectionHeader(HttpResponse response, boolean keepAlive) {
        String connectionValue = keepAlive ? "keep-alive" : "close";

        HttpResponse.Builder builder = HttpResponse.builder(response.getStatus())
                .version(response.getVersion())
                .header("connection", connectionValue);

        response.getHeaders().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("connection")) {
                builder.header(k, v);
            }
        });

        if (response.getBody().length > 0) {
            builder.body(response.getBody());
        }

        return builder.build();
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
