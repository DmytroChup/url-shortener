package com.chupryna.url_shortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("IdObfuscator Unit Tests")
public class IdObfuscatorTest {

    private IdObfuscator idObfuscator;

    @BeforeEach
    void setUp() {
        idObfuscator = new IdObfuscator(1542469173L);
    }

    @ParameterizedTest(name = "ID {0} should be restored after round-trip")
    @DisplayName("Should restore original ID after obfuscation and deobfuscation")
    @ValueSource(longs = {0L, 1L, 2L, 3L, 50L, 123456L, 4_294_967_295L})
    void roundTripTests(long id) {
        assertEquals(id, idObfuscator.deobfuscate(idObfuscator.obfuscate(id)));
    }

    @ParameterizedTest(name = "Obfuscated ID for {0} should not equal original")
    @DisplayName("Should produce obfuscated ID that differs from original ID")
    @ValueSource(longs = {0L, 1L, 2L, 3L, 50L, 123456L, 4_294_967_295L})
    void name(long id) {
        assertNotEquals(id, idObfuscator.obfuscate(id));
    }
}
