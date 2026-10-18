package com.chupryna.analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        geoIpService = new GeoIpService(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",
            "192.168.1.10",
            "10.0.0.1",
            "172.20.0.0",
            "169.254.1.1",
            "100.64.0.1",          // CGNAT boundary (start)
            "100.127.255.255",     // CGNAT boundary (end)
            "0:0:0:0:0:0:0:0",     // Masked IPv6 ::1
            "::1",
            "fd12:3456::",         // IPv6 ULA
            "fe80::1"              // IPv6 Link-Local
    })
    @DisplayName("Should identify all private and loopback networks as LOCAL without database")
    void resolveCountryCode_NonPublic_ReturnsLocal(String ip) {
        assertThat(geoIpService.resolveCountryCode(ip)).isEqualTo("LOCAL");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "100.63.255.255", // Directly in front of CGNAT (public)
            "100.128.0.1",    // Directly after CGNAT (public)
            "8.8.8.8"         // Public DNS
    })
    @DisplayName("Should not classify public boundary IPs as LOCAL")
    void resolveCountryCode_PublicBoundary_IsNotLocal(String ip) {
        assertThat(geoIpService.resolveCountryCode(ip)).isNotEqualTo("LOCAL");
    }

    @Test
    @DisplayName("Should return UNKNOWN for malformed strings or 'UNKNOWN' literal without DNS lookup")
    void resolveCountryCode_InvalidStrings_ReturnsUnknown() {
        assertThat(geoIpService.resolveCountryCode("UNKNOWN")).isEqualTo("UNKNOWN");
        assertThat(geoIpService.resolveCountryCode("not-an-ip-address")).isEqualTo("UNKNOWN");
        assertThat(geoIpService.resolveCountryCode(null)).isEqualTo("UNKNOWN");
        assertThat(geoIpService.resolveCountryCode("")).isEqualTo("UNKNOWN");
    }
}
