package com.mlops.modellifecycle.handler;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    public static final String BEARER = "Bearer";

    @Override
    public String toString() {
        return "LoginResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
