package com.mlops.modellifecycle.client;

import com.mlops.modellifecycle.config.DownstreamProperties;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelRegistryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingModelRegistryClient implements ModelRegistryPort {
    private static final Logger log = LoggerFactory.getLogger(LoggingModelRegistryClient.class);

    private final DownstreamProperties downstream;

    public LoggingModelRegistryClient(DownstreamProperties downstream) {
        this.downstream = downstream;
    }

    @Override
    public void publish(ModelId id) {
        log.info("Publishing model {} to {} (timeout {})",
                id.getValue(), downstream.baseUrl(), downstream.timeout());
    }
}