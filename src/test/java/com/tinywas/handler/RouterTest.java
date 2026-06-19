package com.tinywas.handler;

import com.tinywas.exception.NotFoundException;
import com.tinywas.http.HttpMethod;
import com.tinywas.http.HttpVersion;
import com.tinywas.http.request.HttpRequest;
import com.tinywas.http.response.HttpResponse;
import com.tinywas.http.response.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouterTest {
    private HttpRequest request(HttpMethod method, String path) {
        return new HttpRequest(method, path, HttpVersion.HTTP_1_1, Collections.emptyMap(), "");
    }

    @Test
    @DisplayName("등록된 라우트와 일치하면 해당 핸들러를 호출한다")
    void shouldCallRegisteredHandlerWhenRouteMatches() throws Exception {
        RequestHandler staticFallback = req -> { throw new NotFoundException("not found"); };
        Router router = new Router(staticFallback);

        router.register(HttpMethod.GET, "/hello", req ->
                HttpResponse.builder(HttpStatus.OK).body("registered handler").build());

        HttpResponse response = router.route(request(HttpMethod.GET, "/hello"));

        assertArrayEquals("registered handler".getBytes(), response.getBody());
    }

    @Test
    @DisplayName("일치하는 라우트가 없으면 정적 파일 핸들러로 위임한다")
    void shouldFallbackToStaticHandlerWhenNoRouteMatches() throws Exception {
        RequestHandler staticFallback = req ->
                HttpResponse.builder(HttpStatus.OK).body("static fallback").build();
        Router router = new Router(staticFallback);

        HttpResponse response = router.route(request(HttpMethod.GET, "/index.html"));

        assertArrayEquals("static fallback".getBytes(), response.getBody());
    }

    @Test
    @DisplayName("동일한 라우트를 중복 등록하면 예외를 던진다")
    void shouldThrowWhenRegisteringDuplicateRoute() {
        RequestHandler staticFallback = req -> { throw new NotFoundException("not found"); };
        Router router = new Router(staticFallback);
        router.register(HttpMethod.GET, "/hello", req -> HttpResponse.builder(HttpStatus.OK).build());

        assertThrows(IllegalStateException.class,
                () -> router.register(HttpMethod.GET, "/hello",
                        req -> HttpResponse.builder(HttpStatus.OK).build()));
    }
}