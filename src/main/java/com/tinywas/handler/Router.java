package com.tinywas.handler;

import com.tinywas.http.HttpMethod;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.response.HttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {
    private final Map<String, RequestHandler> routes = new ConcurrentHashMap<>();
    private final RequestHandler staticFileHandler;

    public Router(RequestHandler staticFileHandler) {
        this.staticFileHandler = staticFileHandler;
    }

    public void register(HttpMethod method, String path, RequestHandler handler) {
        String key = key(method, path);

        RequestHandler previous = routes.putIfAbsent(key, handler);
        if (previous != null) {
            throw new IllegalStateException("Router already registered");
        }

        routes.put(key, handler);
    }

    public HttpResponse route(HttpRequest request) throws IOException {
        RequestHandler handler = routes.get(key(request.getMethod(), request.getPath()));

        if (handler != null) {
            return handler.handle(request);
        }

        // 등록된 헨들러가 없으면 정적 파일로 fallback
        // 404 처리는 StaticFileHandler
        return staticFileHandler.handle(request);
    }

    private String key(HttpMethod method, String path) {
        return method.name() + " " + path;
    }
}
