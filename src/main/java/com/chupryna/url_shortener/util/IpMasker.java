package com.chupryna.url_shortener.util;

import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class IpMasker {

    public String mask(String ip) {
        if(ip == null || ip.isEmpty()) {
            return "UNKNOWN";
        }

        try {
            InetAddress addr = InetAddress.getByName(ip);
            byte[] bytes = addr.getAddress();

            if (addr instanceof Inet4Address) {
                bytes[3] = 0;
            } else if (addr instanceof Inet6Address) {
                for (int i = 6; i < 16; i++) {
                    bytes[i] = 0;
                }
            }

            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return "UNKNOWN";
        }
    }
}
