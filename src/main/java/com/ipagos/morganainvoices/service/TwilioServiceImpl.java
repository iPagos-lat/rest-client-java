package com.ipagos.morganainvoices.service; // O 'service.impl'

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.exception.TwilioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct; // Import Jakarta
import java.util.Map;
import org.json.JSONObject; 

import com.ipagos.morganainvoices.service.TwilioService;

@Service
public class TwilioServiceImpl implements TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioServiceImpl.class);

    @Value("${TWILIO_ACCOUNT_SID}") private String twilioAccountSid;
    @Value("${TWILIO_AUTH_TOKEN}") private String twilioAuthToken;
    @Value("${TWILIO_FROM_NUMBER}") private String twilioFromNumber; // Ej. whatsapp:+1775...

    @PostConstruct
    public void init() {
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio client initialized successfully for SID: {}", twilioAccountSid);
        } catch (Exception e) {
             log.error("CRITICAL: Failed to initialize Twilio client.", e);
             throw new RuntimeException("Twilio client initialization failed.", e);
        }
    }

    @Override
    public void sendTemplateNotification(String toPhoneNumber, String contentSid, Map<String, String> variables) {
        
        String formattedToNumber = toPhoneNumber;
        
        if (formattedToNumber != null && !formattedToNumber.startsWith("+")) {
            log.warn("El número {} no tiene código de país. Asumiendo +52.", toPhoneNumber);
            formattedToNumber = "+52" + formattedToNumber;
        }

        if (formattedToNumber != null && !formattedToNumber.startsWith("whatsapp:")) {
            formattedToNumber = "whatsapp:" + formattedToNumber;
        }

        String contentVariables = new JSONObject(variables).toString();
        log.debug("Enviando plantilla de Twilio a: {}. SID: {}. Variables: {}", formattedToNumber, contentSid, contentVariables);

        try {
            Message message = Message.creator(
                new PhoneNumber(formattedToNumber),
                new PhoneNumber(twilioFromNumber),
                (String) null
            )
            .setContentSid(contentSid)
            .setContentVariables(contentVariables)
            .create();
            
            log.info("Twilio Template Notification Sent. SID: {}", message.getSid());
            
        } catch (TwilioException e) {
            
            // --- ¡AQUÍ ESTÁ LA CORRECCIÓN DEFINITIVA! ---
            // Eliminamos la llamada a e.getCode() y solo logueamos el mensaje.
            log.error("Failed to send Twilio template message to {}. Error: {}", 
                formattedToNumber, 
                e.getMessage(), // <-- CORREGIDO
                e // <-- Incluimos la traza de la excepción
            );
            // ------------------------------------------
            
            throw new RuntimeException("Twilio message failed.", e);
        }
    }
}