package com.ipagos.morganainvoices.config;

import java.util.Properties;

public class CybersourceServiceConfig {
    public static Properties getMerchantDetails() {
        Properties props = new Properties();

        // Autenticación HTTP Signature (CORREGIDA)
        props.setProperty("authenticationType", "http_signature");
        props.setProperty("merchantID", "morgana_certer001");
        props.setProperty("runEnvironment", "api.cybersource.com");

        // Claves HTTP Signature
        props.setProperty("merchantKeyId", "ab0eebfb-7fd4-41eb-bbdf-018792fa3b4b");
        props.setProperty("merchantsecretKey", "rb3ornWLDB5WYj2wqkzTOtCLt/EsoOZFMlWyL85UP9c=");

        // Placeholders para evitar NPE (OAuth/Client Cert)
        props.setProperty("enableClientCert", "false");
        props.setProperty("clientCertDirectory", "src/main/resources"); // Directorio dummy
        props.setProperty("clientCertFile", "");
        props.setProperty("clientCertPassword", "");
        props.setProperty("clientId", "");
        props.setProperty("clientSecret", "");

        // Otros placeholders
        props.setProperty("requestJsonPath", "src/main/resources/request.json"); // Dummy path
        props.setProperty("portfolioID", "kedren:portfolio");
        props.setProperty("defaultDeveloperId", "");
        props.setProperty("solutionId", "");
        props.setProperty("timeout", "3000");

        // Logging
        props.setProperty("enableLog", "true");
        props.setProperty("logDirectory", "log");
        props.setProperty("logFilename", "cybs");
        props.setProperty("logMaximumSize", "5M");

        return props;
    }
}