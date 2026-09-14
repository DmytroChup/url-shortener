package com.chupryna.url_shortener.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class Base62Encoder {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = ALPHABET.length();
    private static final int[] CHAR_INDEX_TABLE = new int[128];

    static {
        Arrays.fill(CHAR_INDEX_TABLE, -1);

        for (int i = 0; i < ALPHABET.length(); i++) {
            CHAR_INDEX_TABLE[ALPHABET.charAt(i)] = i;
        }
    }

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
        if (shortUrl == null || shortUrl.isBlank()) {
            throw new IllegalArgumentException("Short URL cannot be empty");
        }

        long result = 0;
        for (int i = 0; i < shortUrl.length(); i++) {
            char c = shortUrl.charAt(i);

            if (c >= CHAR_INDEX_TABLE.length || CHAR_INDEX_TABLE[c] == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }

            result = result * BASE + CHAR_INDEX_TABLE[c];
        }

        return result;
    }
}
