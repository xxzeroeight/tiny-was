package com.tinywas;

import com.tinywas.server.TinyWAS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TinyWASTest {
    @Test
    void shouldCreateServerWithPort() {
        assertDoesNotThrow(() -> new TinyWAS(8080));
    }
}
