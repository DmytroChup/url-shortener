package com.chupryna.url_shortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALPHABET.length();

    public String encode(long id) {
        if (id == 0) return String.valueOf(ALPHABET.charAt(0));

        StringBuilder encodedUrl = new StringBuilder();
        long currentId = id;

        while(currentId > 0) {
            int remainder = (int) (currentId % BASE);
            encodedUrl.append(ALPHABET.charAt(remainder));
            currentId /= BASE;
        }
        return encodedUrl.reverse().toString();
    }

    public long decode(String shortUrl) {
        return shortUrl.chars()
                .map(ALPHABET::indexOf)
                .asLongStream()
                .reduce(0L, (result, index) -> result * BASE + index);
    }
}
