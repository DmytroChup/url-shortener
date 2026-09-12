package com.chupryna.url_shortener.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IdObfuscator {

    private static final long MULTIPLIER = 1588635697L;
    private static final long INVERSE = 741716177L;
    private static final long MASK_32 = 0xFFFFFFFFL;

    private final long xorMask;

    public IdObfuscator(@Value("${app.obfuscation.xor-mask}") long xorMask) {
        this.xorMask = xorMask;
    }

    public long obfuscate(long id) {
        long x = (id * MULTIPLIER) & MASK_32;
        return x ^ xorMask;
    }

    public long deobfuscate(long obfuscatedId) {
        long x = obfuscatedId ^ xorMask;
        return (x * INVERSE) & MASK_32;
    }
}
