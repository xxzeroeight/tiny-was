package com.tinywas.http.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpResponseWriter {
    private static final String CRLF = "\r\n";

    public void write(HttpResponse response, OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 1. 상태 라인
        sb.append(response.getVersion().getValue())
                .append(" ")
                .append(response.getStatus())
                .append(CRLF);

        // 2. 헤더
        response.getHeaders().forEach((key, value) ->
                sb.append(key).append(": ").append(value).append(CRLF));

        // 3. 빈 줄
        sb.append(CRLF);

        // 4. 바디
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8)); // 헤더
        outputStream.write(response.getBody()); // 바디
        outputStream.flush();
    }
}
