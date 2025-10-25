package com.ipagos.morganainvoices.config; // O tu paquete de config

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cybersource.authsdk.core.ConfigException;
import com.cybersource.authsdk.core.MerchantConfig;
import Api.InvoicesApi;
import Invokers.ApiClient;

// Imports Críticos para la corrección de Joda-Time
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter; // Import base TypeAdapter
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken; // Import JsonToken
import org.joda.time.LocalDate;
import org.joda.time.DateTime; // <-- Import Joda DateTime
import org.joda.time.format.ISODateTimeFormat; // <-- Import ISO Formatter
import Invokers.JSON; // El serializador interno del SDK
import java.io.IOException; // Import IOException

@Configuration
public class CybersourceClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CybersourceClientConfig.class);

    /**
     * Adaptador para org.joda.time.LocalDate <-> "YYYY-MM-DD" String.
     */
    private static class JodaLocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString()); // Formato ISO "YYYY-MM-DD"
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                try {
                    return new LocalDate(in.nextString());
                } catch (IllegalArgumentException e) {
                    LOG.error("Error parsing Joda LocalDate from string", e);
                    // Decide how to handle parse errors, maybe return null or throw IOException
                    // For robustness, returning null might be safer if the API sometimes sends bad data
                     return null; 
                    // throw new IOException("Could not parse Joda LocalDate", e);
                }
            }
        }
    }

    /**
     * --- ¡NUEVO ADAPTADOR! ---
     * Adaptador para org.joda.time.DateTime <-> ISO8601 String (ej. "2025-10-23T17:04:18Z").
     * Esto debería arreglar el error de deserialización en getInvoice.
     */
    private static class JodaDateTimeAdapter extends TypeAdapter<DateTime> {
        // Usar el parser/printer estándar ISO8601 de JodaTime
        private final org.joda.time.format.DateTimeFormatter isoParser = ISODateTimeFormat.dateTimeParser().withOffsetParsed();
        private final org.joda.time.format.DateTimeFormatter isoPrinter = ISODateTimeFormat.dateTime().withZoneUTC(); // Siempre escribir en UTC

        @Override
        public void write(JsonWriter out, DateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                // Escribir como String ISO8601 en UTC
                out.value(isoPrinter.print(value));
            }
        }

        @Override
        public DateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                String dateString = in.nextString();
                try {
                    // Intentar parsear el String ISO8601
                    return isoParser.parseDateTime(dateString);
                } catch (IllegalArgumentException e) {
                    LOG.error("Error parsing Joda DateTime from string '{}'", dateString, e);
                    // Decide how to handle parse errors
                    return null; // Return null on failure
                    // throw new IOException("Could not parse Joda DateTime: " + dateString, e);
                }
            }
        }
    }


    /**
     * Este método crea el ApiClient con el serializador Gson CORREGIDO
     * para LocalDate Y DateTime.
     */
    @Bean
    public ApiClient apiClient() {
        LOG.info("Inicializando ApiClient de CyberSource (CON PARCHE JODA LocalDate y DateTime)...");
        try {
            // Asumiendo que CybersourceServiceConfig existe y funciona
            Properties merchantProps = CybersourceServiceConfig.getMerchantDetails(); 
            MerchantConfig merchantConfig = new MerchantConfig(merchantProps);
            
            // 1. Crear el ApiClient
            ApiClient apiClient = new ApiClient();
            
            // 2. Crear el GsonBuilder con AMBOS adaptadores
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDate.class, new JodaLocalDateAdapter());
            gsonBuilder.registerTypeAdapter(DateTime.class, new JodaDateTimeAdapter()); // <-- AÑADIR ADAPTADOR DateTime
            
            // 3. Obtener el serializador JSON interno del SDK
            JSON sdkJson = apiClient.getJSON();
            
            // 4. Reemplazar el Gson roto del SDK por nuestro Gson corregido
            sdkJson.setGson(gsonBuilder.create());
            
            // 5. Asignar la configuración (usando reflexión como antes)
            try {
                // Acceder al campo 'merchantConfig' directamente si es público o usar reflexión si es privado
                 // Si 'merchantConfig' es público: apiClient.merchantConfig = merchantConfig;
                 // Si es privado (como parece):
                java.lang.reflect.Field field = ApiClient.class.getDeclaredField("merchantConfig");
                field.setAccessible(true); // Permitir acceso a campo privado
                field.set(apiClient, merchantConfig);
                LOG.info("MerchantConfig asignado directamente al ApiClient via reflexión.");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                 LOG.error("No se pudo asignar MerchantConfig via reflexión. Verifica el nombre del campo en ApiClient.", e);
                 // Considera añadir un método setMerchantConfig si es posible, o usar otra forma de configuración
                 throw new RuntimeException("Fallo crítico al configurar ApiClient.", e);
            } catch (Exception e) {
                 LOG.error("Error inesperado al asignar MerchantConfig.", e);
                 throw new RuntimeException("Fallo inesperado al configurar ApiClient.", e);
            }

            LOG.info("ApiClient inicializado (con parche DateTime). Entorno: {}", merchantConfig.getRunEnvironment());
            return apiClient;

        } catch (ConfigException e) {
            LOG.error("Error de configuración de CyberSource: {}", e.getMessage(), e);
            throw new RuntimeException("Error al construir MerchantConfig de CyberSource.", e);
        } catch (Exception e) {
            LOG.error("Error inesperado al inicializar ApiClient: {}", e.getMessage(), e);
            throw new RuntimeException("Fallo general al inicializar ApiClient de CyberSource.", e);
        }
    }

    @Bean
    public InvoicesApi invoicesApi(ApiClient apiClient) { // Spring inyectará el ApiClient parchado
        LOG.info("Creando bean InvoicesApi...");
        return new InvoicesApi(apiClient);
    }
}