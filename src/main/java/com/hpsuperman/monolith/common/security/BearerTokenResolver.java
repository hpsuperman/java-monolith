package com.hpsuperman.monolith.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class BearerTokenResolver {
    public static final String HEADER_NAME = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private BearerTokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
