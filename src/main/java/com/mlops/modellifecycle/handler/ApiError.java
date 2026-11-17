package com.mlops.modellifecycle.handler;

public record ApiError(String code, String message, String correlationId) {
}