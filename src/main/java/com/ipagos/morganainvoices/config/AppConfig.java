package com.ipagos.morganainvoices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // You can add further configuration here if needed (e.g., error handling, timeouts)
        return new RestTemplate();
    }
}