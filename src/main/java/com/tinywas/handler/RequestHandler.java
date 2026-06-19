package com.tinywas.handler;

import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.response.HttpResponse;

import java.io.IOException;

@FunctionalInterface
public interface RequestHandler {
    HttpResponse handle(HttpRequest request) throws IOException;
}
