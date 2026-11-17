package com.mlops.modellifecycle.handler;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationIds {
    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private CorrelationIds() {
    }

    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && SAFE.matcher(header.trim()).matches()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }
}