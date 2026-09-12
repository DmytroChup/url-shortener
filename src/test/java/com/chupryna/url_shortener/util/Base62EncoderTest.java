package com.chupryna.url_shortener.util;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Base62EncoderTest {

    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();
    }

    @Test
    @DisplayName("Should return 'a' when encoding 0")
    void encodeShouldReturnA() {
        String encoded = base62Encoder.encode(0);
        assertEquals("a", encoded);
    }

    @ParameterizedTest(name = "Encoding {0} should produce \"{1}\"")
    @CsvSource({
            "1, b",
            "2, c",
            "61, 9",
            "62, ba"
    })
    void baseEncodeTests(long id, String expected) {
        assertEquals(expected, base62Encoder.encode(id));
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 123456L, 9999999L})
    void roundTripTests(long id) {
        assertEquals(id, base62Encoder.decode(base62Encoder.encode(id)));
    }

    @ParameterizedTest(name = "Decoding {0} should produce \"{1}\"")
    @CsvSource({
            "a, 0",
            "b, 1",
            "ba, 62"
    })
    void baseDecodeTests(String shortUrl, long id) {
        assertEquals(id, base62Encoder.decode(shortUrl));
    }
}
