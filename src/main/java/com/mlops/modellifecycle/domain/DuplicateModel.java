package com.mlops.modellifecycle.domain;

public class DuplicateModel extends RuntimeException {
    public DuplicateModel(String businessKey) {
        super("duplicate model: " + businessKey);
    }
}