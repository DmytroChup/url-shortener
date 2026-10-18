package com.chupryna.analytics.service;

import com.chupryna.analytics.config.UserAgentConfig;
import com.chupryna.analytics.model.DeviceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAgentParserServiceTest {

    private static UserAgentParserService service;

    @BeforeAll
    static void init() {
        service = new UserAgentParserService(new UserAgentConfig().userAgentAnalyzer());
    }

    @ParameterizedTest(name = "{index} => Device {1} for UA: {0}")
    @CsvSource(delimiter = '|', textBlock = """
            Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36 | DESKTOP
            Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36 | MOBILE
            Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36 | TABLET
            Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1 | TABLET
            Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html) | BOT
            curl/8.4.0 | BOT
            """)
    @DisplayName("Should correctly classify devices using YAUAA rules")
    void parse_ClassifiesDeviceType(String ua, DeviceType expected) {
        assertThat(service.parseUserAgent(ua).deviceType()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle null and blank safely without exceptions")
    void parse_NullOrBlank() {
        var nullMeta = service.parseUserAgent(null);
        assertThat(nullMeta.deviceType()).isEqualTo(DeviceType.UNKNOWN);
        assertThat(nullMeta.browser()).isEqualTo("Unknown");

        var blankMeta = service.parseUserAgent("   ");
        assertThat(blankMeta.deviceType()).isEqualTo(DeviceType.UNKNOWN);
    }
}
