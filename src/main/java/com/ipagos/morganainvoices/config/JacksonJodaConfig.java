package com.ipagos.morganainvoices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Configuration
public class JacksonJodaConfig {

    @Bean
    public JodaModule jodaModule() {
        return new JodaModule();
    }
}