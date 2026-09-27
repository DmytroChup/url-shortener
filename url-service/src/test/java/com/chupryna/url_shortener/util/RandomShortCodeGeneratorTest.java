package com.chupryna.url_shortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RandomShortCodeGeneratorTest {

    private RandomShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RandomShortCodeGenerator();
    }

    @Test
    @DisplayName("Should generate code with exactly 7 characters")
    void generate_CorrectLength() {
        String code = generator.generate();
        assertEquals(7, code.length());
    }

    @Test
    @DisplayName("Should generate code containing only valid Base62 characters")
    void generate_OnlyBase62Characters() {
        String code = generator.generate();
        assertTrue(code.matches("^[a-zA-Z0-9]{7}$"));
    }

    @Test
    @DisplayName("Should produce different codes on subsequent invocations")
    void generate_ProducesDistinctCodes() {
        String code1 = generator.generate();
        String code2 = generator.generate();
        assertNotEquals(code1, code2);
    }
}
