package com.ipagos.morganainvoices.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

/**
 * Clase de utilidad para conversiones de fechas y horas.
 */
public class DateUtils {

    /**
     * Convierte milisegundos (epoch millis) a un String en formato 
     * ISO 8601 (ej. 2025-12-31T12:00:00-06:00) para una zona horaria específica.
     *
     * @param millis El valor long de iLocalMillis
     * @param zoneId El ID de la zona horaria (ej. "America/Mexico_City")
     * @return El string ISO formateado.
     */
    public static String convertMillisToISO(long millis, String zoneId) {
        
        try {
            Instant instant = Instant.ofEpochMilli(millis);
            ZoneId zone = ZoneId.of(zoneId);
            
            return instant
                    .atZone(zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        } catch (ZoneRulesException e) {
            // Manejar error de zona inválida
            System.err.println("Error: Zona horaria no válida: " + zoneId);
            // Fallback a UTC si la zona falla
            return Instant.ofEpochMilli(millis).toString();
        } catch (Exception e) {
            // Manejar cualquier otro error
            System.err.println("Error al convertir milisegundos: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convierte milisegundos a formato ISO UTC (ej. 2025-12-31T18:00:00Z).
     */
    public static String convertMillisToISO_UTC(long millis) {
        return Instant.ofEpochMilli(millis).toString();
    }
}