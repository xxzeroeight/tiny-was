package com.tinywas.handler;

import com.tinywas.exception.MethodNotAllowedException;
import com.tinywas.exception.NotFoundException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class StaticFileHandler implements RequestHandler {
    private static final String DEFAULT_INDEX = "index.html";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html",
            "css", "text/css",
            "js", "application/javascript",
            "json", "application/json",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "ico", "image/x-icon",
            "txt", "text/plain"
    );

    private static final List<HttpMethod> ALLOWED_METHODS = List.of(
            HttpMethod.GET,
            HttpMethod.HEAD
    );

    private final Path staticRoot;

    public StaticFileHandler(Path staticRoot) {
        this.staticRoot = staticRoot;
    }

    @Override
    public HttpResponse handle(HttpRequest request) throws IOException {
        if (!ALLOWED_METHODS.contains(request.getMethod())) {
            throw new MethodNotAllowedException("GET, HEAD");
        }

        Path resolved = resolveSafely(request.getPath());

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new NotFoundException("File not found");
        }

        byte[] body = Files.readAllBytes(resolved);

        return HttpResponse.builder(HttpStatus.OK)
                .header("Content-Type", getDefaultContentType(resolved))
                .body(body)
                .build();
    }

    private Path resolveSafely(String requestPath) {
        String relativePath = requestPath.equals("/") ? DEFAULT_INDEX : requestPath.substring(1);
        Path resolved = staticRoot.resolve(relativePath).normalize();

        if (!resolved.startsWith(staticRoot)) {
            throw new NotFoundException("File Not Found");
        }

        return resolved;
    }

    private String getDefaultContentType(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');

        if (index == -1) {
            return DEFAULT_CONTENT_TYPE;
        }

        String extension = fileName.substring(index + 1).toLowerCase();

        return CONTENT_TYPES.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }
}
