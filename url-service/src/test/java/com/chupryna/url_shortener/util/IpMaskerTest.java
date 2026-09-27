package com.chupryna.url_shortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("IpMasker Unit Tests")
class IpMaskerTest {

    private IpMasker ipMasker;

    @BeforeEach
    void setUp() {
        ipMasker = new IpMasker();
    }

    @ParameterizedTest
    @CsvSource({
            "192.168.1.123,   192.168.1.0",
            "10.0.0.255,      10.0.0.0",
            "8.8.8.8,         8.8.8.0"
    })
    @DisplayName("Should zero out the last octet of an IPv4 address")
    void mask_Ipv4_ZerosLastOctet(String input, String expected) {
        assertEquals(expected, ipMasker.mask(input));
    }

    @ParameterizedTest
    @CsvSource({
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334,   2001:db8:85a3:0:0:0:0:0",
            "2001:db8:85a3::8a2e:370:7334,              2001:db8:85a3:0:0:0:0:0",
            "2001:0db8:0000:0000:0000:ff00:0042:8329,   2001:db8:0:0:0:0:0:0"
    })
    @DisplayName("Should produce the SAME masked result for full and shortened notation of the same IPv6 address")
    void mask_Ipv6_FullAndShortNotation_ProduceSameResult(String input, String expected) {
        assertEquals(expected, ipMasker.mask(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"::1", "0:0:0:0:0:0:0:1"})
    @DisplayName("Should mask IPv6 loopback consistently regardless of notation")
    void mask_Ipv6_Loopback(String input) {
        assertEquals("0:0:0:0:0:0:0:0", ipMasker.mask(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"::ffff:192.168.1.123", "0:0:0:0:0:ffff:c0a8:17b"})
    @DisplayName("Should handle IPv4-mapped IPv6 addresses without throwing")
    void mask_Ipv4MappedIpv6_DoesNotThrow(String input) {
        assertDoesNotThrow(() -> ipMasker.mask(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should return UNKNOWN for null or empty input")
    void mask_NullOrEmpty_ReturnsUnknown(String input) {
        assertEquals("UNKNOWN", ipMasker.mask(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-ip", "999.999.999.999", "  "})
    @DisplayName("Should return UNKNOWN for malformed input instead of throwing")
    void mask_InvalidInput_ReturnsUnknown(String input) {
        assertEquals("UNKNOWN", ipMasker.mask(input));
    }

    @Test
    @DisplayName("Should mask short-form IPv4 notation per InetAddress semantics (192.168.1 = 192.168.0.1)")
    void mask_ShortFormIpv4_MasksAsInterpretedByInetAddress() {
        assertEquals("192.168.0.0", ipMasker.mask("192.168.1"));
    }
}