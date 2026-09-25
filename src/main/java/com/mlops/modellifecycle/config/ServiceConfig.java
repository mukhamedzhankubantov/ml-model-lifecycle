package com.mlops.modellifecycle.config;

import com.mlops.modellifecycle.domain.AccuracyStopRule;
import com.mlops.modellifecycle.domain.ModelPolicy;
import com.mlops.modellifecycle.domain.TransitionRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class ServiceConfig {
    @Bean
    public ModelPolicy modelPolicy() {
        return new ModelPolicy(List.of(
                new TransitionRule(),
                new AccuracyStopRule(0.80)
        ));
    }
}