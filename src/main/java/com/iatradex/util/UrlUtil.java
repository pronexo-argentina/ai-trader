package com.iatradex.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlUtil {
    private UrlUtil() {}

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
