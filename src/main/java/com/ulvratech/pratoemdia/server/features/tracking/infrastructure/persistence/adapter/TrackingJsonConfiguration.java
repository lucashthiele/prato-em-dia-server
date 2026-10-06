package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrackingJsonConfiguration {
    @Bean
    ObjectMapper trackingObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
