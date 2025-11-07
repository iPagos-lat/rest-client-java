package com.ipagos.morganainvoices.config;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.cybersource.authsdk.core.ConfigException;
import com.cybersource.authsdk.core.MerchantConfig;
import Api.InvoicesApi;
import Invokers.ApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import org.joda.time.LocalDate;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import Invokers.JSON; // <-- Importante
import java.io.IOException;

@Configuration
public class CybersourceClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CybersourceClientConfig.class);

    @Value("${cybersource.merchant.id}")
    private String merchantId;
    @Value("${cybersource.merchant.keyId}")
    private String merchantKeyId;
    @Value("${cybersource.merchant.secretKey}")
    private String merchantSecretKey; 
    @Value("${cybersource.run.environment}")
    private String runEnvironment;

    // Adaptador para "YYYY-MM-DD"
    private static class JodaLocalDateAdapter extends TypeAdapter<LocalDate> {
         @Override public void write(JsonWriter out, LocalDate value) throws IOException { 
             if (value == null) { out.nullValue(); } else { out.value(value.toString()); } 
         }
         @Override public LocalDate read(JsonReader in) throws IOException { 
             if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; } 
             else { try { return new LocalDate(in.nextString()); } catch (IllegalArgumentException e) { LOG.error("Error parsing Joda LocalDate", e); return null; } } 
         }
    }
    
    // Adaptador para DateTime
    private static class JodaDateTimeAdapter extends TypeAdapter<DateTime> {
         private final org.joda.time.format.DateTimeFormatter isoParser = ISODateTimeFormat.dateTimeParser().withOffsetParsed();
         private final org.joda.time.format.DateTimeFormatter isoPrinter = ISODateTimeFormat.dateTime().withZoneUTC();
         @Override public void write(JsonWriter out, DateTime value) throws IOException { if (value == null) { out.nullValue(); } else { out.value(isoPrinter.print(value)); } }
         @Override public DateTime read(JsonReader in) throws IOException { if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; } else { String dateString = in.nextString(); try { return isoParser.parseDateTime(dateString); } catch (IllegalArgumentException e) { LOG.error("Error parsing Joda DateTime '{}'", dateString, e); return null; } } }
    }


    @Bean
    public ApiClient apiClient() {
        LOG.info("Inicializando ApiClient de CyberSource (CON PARCHE JODA LocalDate y DateTime)...");
        
        try {
            // 1. Crear el objeto Properties
            Properties properties = new Properties();
            properties.setProperty("merchantID", this.merchantId);
            properties.setProperty("merchantKeyId", this.merchantKeyId);
            properties.setProperty("merchantsecretKey", this.merchantSecretKey); // <-- con 's' minúscula
            properties.setProperty("runEnvironment", this.runEnvironment);
            properties.setProperty("authenticationType", "http_signature"); 
            
            // 2. Crear el MerchantConfig
            MerchantConfig merchantConfig = new MerchantConfig(properties);
            
            // 3. Aplicar el Parche GSON
            ApiClient apiClient = new ApiClient();
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDate.class, new JodaLocalDateAdapter());
            gsonBuilder.registerTypeAdapter(DateTime.class, new JodaDateTimeAdapter()); 
            Gson gson = gsonBuilder.create();
            
            JSON sdkJson = new JSON(apiClient); 
            sdkJson.setGson(gson);
            apiClient.setJSON(sdkJson);
            
            // 4. Asignar el MerchantConfig (usando reflexión)
            try {
                java.lang.reflect.Field field = ApiClient.class.getDeclaredField("merchantConfig");
                field.setAccessible(true); 
                field.set(apiClient, merchantConfig);
                LOG.info("MerchantConfig (desde application.properties) asignado directamente al ApiClient via reflexión.");
            } catch (Exception e) {
                 LOG.error("No se pudo asignar MerchantConfig via reflexión.", e);
                 throw new RuntimeException("Fallo crítico al configurar ApiClient.", e);
            }

            LOG.info("ApiClient inicializado (con parche DateTime). Entorno: {}", merchantConfig.getRunEnvironment());
            return apiClient;

        } catch (ConfigException e) {
            LOG.error("Error de configuración de CyberSource: {}", e.getMessage(), e);
            throw new RuntimeException("Error al construir MerchantConfig.", e);
        } catch (Exception e) {
            LOG.error("Error inesperado al inicializar ApiClient: {}", e.getMessage(), e);
            throw new RuntimeException("Fallo general al inicializar ApiClient.", e);
        }
    }

    @Bean
    public InvoicesApi invoicesApi(ApiClient apiClient) { 
        LOG.info("Creando bean InvoicesApi...");
        return new InvoicesApi(apiClient);
    }
}