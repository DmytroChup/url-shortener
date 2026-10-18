package com.chupryna.analytics.service;

import com.chupryna.analytics.model.DeviceType;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAgentParserService {

    private final UserAgentAnalyzer userAgentAnalyzer;

    public record UserAgentMetadata(
            String browser,
            String operatingSystem,
            DeviceType deviceType
    ) {}

    public UserAgentMetadata parseUserAgent(String userAgentString) {
        if (userAgentString == null || userAgentString.isBlank()) {
            return new UserAgentMetadata("Unknown", "Unknown", DeviceType.UNKNOWN);
        }

        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        String browser = agent.getValue(UserAgent.AGENT_NAME);
        String os = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);

        return new UserAgentMetadata(
                browser != null ? browser : "Unknown",
                os != null ? os : "Unknown",
                mapToDeviceType(deviceClass)
        );
    }

    private DeviceType mapToDeviceType(String deviceClass) {
        if (deviceClass == null) {
            return DeviceType.UNKNOWN;
        }

        if (deviceClass.startsWith("Robot")) {
            return DeviceType.BOT;
        }
        return switch (deviceClass) {
            case "Desktop" -> DeviceType.DESKTOP;
            case "Phone", "Mobile" -> DeviceType.MOBILE;
            case "Tablet" -> DeviceType.TABLET;
            case "Hacker" -> DeviceType.BOT;
            default -> DeviceType.UNKNOWN;
        };
    }
}
