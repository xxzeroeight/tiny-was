package com.tinywas.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerConfigTest {
    @Test
    @DisplayName("port가 범위를 벗어나면 빌드 시점에 예외를 던진다")
    void throwExceptionWhenPortOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.builder().port(99999).build());
    }

    @Test
    @DisplayName("backlog가 0 이하이면 빌드 시점에 예외를 던진다")
    void throwExceptionWhenBacklogIsNotPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.builder().backlog(0).build());
    }

    @Test
    @DisplayName("staticRoot이 존재하지 않으면 빌드 시점에 예외를 던진다")
    void throwExceptionWhenStaticRootDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.builder().staticRoot("/no/such/directory").build());
    }

    @Test
    @DisplayName("staticRoot이 디렉토리가 아니면 빌드 시점에 예외를 던진다")
    void throwExceptionWhenStaticRootIsNotDirectory(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("not-a-dir.txt");
        Files.createFile(file);

        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.builder().staticRoot(file.toString()).build());
    }

    @Test
    @DisplayName("staticRoot이 정상 디렉토리면 절대경로로 정규화되어 저장된다")
    void normalizeStaticRootToAbsolutePath(@TempDir Path tempDir) {
        ServerConfig config = ServerConfig.builder()
                .staticRoot(tempDir.toString())
                .build();

        assertEquals(tempDir.toAbsolutePath().normalize(), config.getStaticRoot());
    }
}