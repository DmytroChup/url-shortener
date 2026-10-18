package com.chupryna.analytics.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

@Service
@Slf4j
public class GeoIpService {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String LOCAL = "LOCAL";

    private final DatabaseReader databaseReader;

    public GeoIpService() {
        DatabaseReader reader = null;
        try (InputStream stream = getClass().getResourceAsStream("/geoip/GeoLite2-Country.mmdb")) {
            if (stream != null) {
                reader = new DatabaseReader.Builder(stream).build();
                log.info("GeoLite2-Country database successfully loaded into memory");
            } else {
                log.warn("GeoLite2-Country.mmdb not found in classpath, GeoIP lookup will return UNKNOWN");
            }
        } catch (Exception e) {
            log.error("Failed to initialize GeoIP DatabaseReader", e);
        }
        this.databaseReader = reader;
    }

    public GeoIpService(@Nullable DatabaseReader databaseReader) {
        this.databaseReader = databaseReader;
    }

    public String resolveCountryCode(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return UNKNOWN;
        }

        InetAddress address;
        try {
            address = InetAddress.ofLiteral(ipAddress.trim());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }

        if (isNonPublic(address)) {
            return LOCAL;
        }

        if (databaseReader == null) {
            return UNKNOWN;
        }

        try {
            CountryResponse response = databaseReader.country(address);
            if (response != null && response.country() != null && response.country().isoCode() != null) {
                return response.country().isoCode();
            }
        } catch (AddressNotFoundException e) {
            return UNKNOWN;
        } catch (Exception e) {
            log.debug("Could not resolve country for IP {}: {}", ipAddress, e.getMessage());
        }

        return UNKNOWN;
    }

    private static boolean isNonPublic(InetAddress a) {
        byte[] b = a.getAddress();
        boolean uniqueLocalV6 = a instanceof Inet6Address && (b[0] & 0xFE) == 0xFC;
        boolean cgnatV4 = a instanceof Inet4Address && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
        return a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || uniqueLocalV6 || cgnatV4;
    }

    @PreDestroy
    public void close() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
            } catch (Exception e) {
                log.error("Error closing GeoIP database reader", e);
            }
        }
    }
}
