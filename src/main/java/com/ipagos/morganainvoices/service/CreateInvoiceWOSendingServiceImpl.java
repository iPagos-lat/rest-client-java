package com.ipagos.morganainvoices.service;

import Api.InvoicesApi;
import Invokers.ApiClient;
import Invokers.ApiException;
import Model.*;
import org.joda.time.LocalDate; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate; 
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// DTO Imports (Beans)
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingRequest;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingResponse;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CustomerInformation;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.MerchantDefinedFieldValues;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.OrderInformationLineItems;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.AmountDetails;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.OrderInformation;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.InvoiceInformation;
import com.fasterxml.jackson.databind.ObjectMapper;

// TWILIO Imports
import com.ipagos.morganainvoices.service.EmailService;
import com.ipagos.morganainvoices.service.TwilioService; 

// SDK Imports (para la llamada estándar)
import com.cybersource.authsdk.core.ConfigException;
import Model.CreateInvoiceRequest; 

// Imports para parche de Jackson Joda
import com.fasterxml.jackson.datatype.joda.JodaModule;
import jakarta.annotation.PostConstruct; 

@Service
public class CreateInvoiceWOSendingServiceImpl implements CreateInvoiceWOSendingService { 

    private static final Logger log = LoggerFactory.getLogger(CreateInvoiceWOSendingServiceImpl.class);

    private static final String DEFAULT_CURRENCY = "MXN"; 
    private static final String ARCHIVE_EMAIL = "archivo@ipagos.lat"; 

    // --- Inyección de Dependencias ---
    @Autowired private InvoicesApi invoicesApi; 
    @Autowired private EmailService emailService;
    @Autowired private TwilioService twilioService; 
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RestTemplate restTemplate; 

    // --- Cybersource Config Details ---
    @Value("${cybersource.merchant.id}")
    private String merchantId;
    @Value("${cybersource.merchant.keyId}")
    private String merchantKeyId;
    @Value("${cybersource.merchant.secretKey}")
    private String merchantSecretKey;
    @Value("${cybersource.run.environment}")
    private String runEnvironmentHost;
    
    // --- TWILIO Config Details ---
    @Value("${TWILIO_TEMPLATE_SID}") 
    private String twilioTemplateSid;

    @PostConstruct
    public void setupObjectMapper() {
        objectMapper.registerModule(new JodaModule());
        log.info("Jackson JodaModule registrado en ObjectMapper para logging.");
    }

    @Override
    @Transactional
    public CreateInvoiceWOSendingResponse createInvoice(CreateInvoiceWOSendingRequest request) throws ApiException {
        String createdInvoiceId = null;
        InvoicingV2InvoicesPost201Response createResult = null; 
        String finalPaymentLink = null;
        
        String finalCustomerEmail = request.getCustomerInformation().getEmail();
        String patientPhone = getMdfValue(request.getMerchantDefinedFieldValuesList(), "3", null);

        String timestamp = String.valueOf(System.currentTimeMillis());
        String shortTimestamp = timestamp.substring(0, Math.min(timestamp.length(), 12));
        String uniqueInvoiceNumber = "SINAPT" + shortTimestamp;
        log.info("Generated uniqueInvoiceNumber (20 chars): {}", uniqueInvoiceNumber);

        try {
            request.getCustomerInformation().setEmail(ARCHIVE_EMAIL);
            log.info("📧 Customer email temporarily changed to {} for Cybersource payload.", ARCHIVE_EMAIL);

            // 3. CONSTRUIR EL OBJETO DEL SDK (usando setters)
            CreateInvoiceRequest sdkRequest = buildCreateInvoiceRequest(request, uniqueInvoiceNumber);
            
            request.getCustomerInformation().setEmail(finalCustomerEmail);

            try {
                log.info("=== SDK Request Object Structure (Jackson Log) ===\n{}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sdkRequest));
            } catch (Exception e) {
                log.error("Error serializing sdkRequest with Jackson for log: {}", e.getMessage());
            }

            // --- 5. LLAMADA ESTÁNDAR AL SDK ---
            log.info("Calling SDK method createInvoice (DRAFT) with ID: {}", uniqueInvoiceNumber);
            try {
                createResult = invoicesApi.createInvoice(sdkRequest);
            } catch (ConfigException ce) {
                log.error("ConfigException al crear invoice: {}", ce.getMessage(), ce);
                throw new ApiException(500, "Configuration error: " + ce.getMessage());
            } catch (ApiException ae) {
                log.error("ApiException al llamar a CyberSource (SDK createInvoice). Código: {}. Headers: {}. Body: {}",
                          ae.getCode(), ae.getResponseHeaders(), ae.getResponseBody(), ae);
                WriteLogAudit(ae.getCode()); 
                throw ae; // Re-lanzar
            }
            // ---------------------------------------------------------------------

            if (createResult == null || createResult.getId() == null) {
                 throw new IllegalStateException("Invoice creation failed: ID was not returned.");
            }
            createdInvoiceId = createResult.getId();
            WriteLogAudit(201); 
            log.info("CyberSource Create call successful. ID: {}", createdInvoiceId);


            // 6. --- ACTIVACIÓN Y EXTRACCIÓN DEL LINK (Usando el SDK) ---
            log.warn("Llamando a performSendAction (Link Activation). CS notification sent to ARCHIVE_EMAIL.");
            
            Object sendResultRaw = null;
            InvoicingV2InvoicesSend200Response sendResult = null;
            
            try {
                sendResultRaw = invoicesApi.performSendAction(createdInvoiceId);
                sendResult = (InvoicingV2InvoicesSend200Response) sendResultRaw;
                log.info("CyberSource Send action successful. Link is now active.");
            
            } catch (ConfigException ce) {
                log.error("ConfigException al activar el link (performSendAction): {}", ce.getMessage(), ce);
                throw new ApiException(500, "Configuration error during link activation: " + ce.getMessage());
            } catch (ApiException ae) {
                log.error("ApiException al activar el link (performSendAction). Código: {}. Headers: {}. Body: {}",
                          ae.getCode(), ae.getResponseHeaders(), ae.getResponseBody(), ae);
                WriteLogAudit(ae.getCode()); 
                throw ae; // Re-lanzar
            }

            if (sendResult != null && sendResult.getInvoiceInformation() != null && sendResult.getInvoiceInformation().getPaymentLink() != null) {
                finalPaymentLink = sendResult.getInvoiceInformation().getPaymentLink(); 
                log.info("🔗 Payment Link FINAL (Extraído de performSendAction): {}", finalPaymentLink);
            } else { 
                 log.error("⚠️ FINAL PAYMENT LINK NOT FOUND in performSendAction response.");
            }

            // 7. --- Enviar correo y WhatsApp (USO FINAL DEL LINK) ---
            if (finalPaymentLink != null) {
                String doctorName = getMdfValue(request.getMerchantDefinedFieldValuesList(), "1", "Doctor");
                String timeStr = getMdfValue(request.getMerchantDefinedFieldValuesList(), "2", "12:00");
                
                String formattedTime = timeStr;
                try { 
                    formattedTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                                             .format(DateTimeFormatter.ofPattern("hh:mm a")); 
                } catch (DateTimeParseException ignored) { /* Usar valor original si falla */ }
                
                String date = request.getInvoiceInformation().getDueDate();
                String amount = request.getOrderInformation().getAmountDetails().getTotalAmount();
                String patientName = request.getCustomerInformation().getName(); 
                
                // A. Envío por Correo Personalizado (al cliente real)
                try {
                	emailService.sendAppointmentConfirmation(finalCustomerEmail, patientName, doctorName, date, formattedTime, amount, finalPaymentLink, date);
                     log.info("Correo personalizado enviado a: {}, Doctor: {}", finalCustomerEmail, doctorName);
                } catch (Exception e) { 
                    log.error("Fallo al enviar correo: {}", e.getMessage(), e); 
                }

                // B. Envío por WhatsApp
                if (patientPhone != null && patientPhone.length() >= 10) {
                    try {
                        Map<String, String> variables = new HashMap<>();
                        variables.put("1", patientName);
                        variables.put("2", doctorName);
                        variables.put("3", date);
                        variables.put("4", formattedTime);
                        variables.put("5", amount);
                        variables.put("6", finalPaymentLink);
                        
                        twilioService.sendTemplateNotification(patientPhone, twilioTemplateSid, variables);
                        
                    } catch (Exception e) {
                        log.error("Fallo al enviar notificación de WhatsApp/SMS: {}", e.getMessage());
                    }
                } else {
                     log.warn("Teléfono inválido/nulo para Twilio. Omitting WhatsApp notification.");
                }

            } else { 
                log.error("Link nulo, correo/WhatsApp NO fueron enviados."); 
            }
            // -----------------------------------

        } catch (IllegalArgumentException e) { 
             log.error("Input data validation error: {}", e.getMessage(), e);
             WriteLogAudit(400);
             throw new ApiException(400, "Invalid input data: " + e.getMessage());
        } 
        
        // Devolvemos la respuesta
        return CreateInvoiceWOSendingResponse.builder()
                 .result(null) 
                 .paymentLink(finalPaymentLink)
                 .build();
    }


    // =========================================================================
    // ==================== MÉTODOS AUXILIARES =================================
    // =========================================================================

    /**
     * Mapea el DTO local de entrada (CreateInvoiceRequest) al DTO del SDK (Model.CreateInvoiceRequest).
     */
    private CreateInvoiceRequest buildCreateInvoiceRequest(CreateInvoiceWOSendingRequest request, String uniqueInvoiceNumber) {
        
        CreateInvoiceRequest sdkRequest = new CreateInvoiceRequest(); 

        // 1. Customer information
        if (request.getCustomerInformation() != null) {
            Invoicingv2invoicesCustomerInformation sdkCustomer = new Invoicingv2invoicesCustomerInformation();
            sdkCustomer.setName(request.getCustomerInformation().getName()); 
            sdkCustomer.setEmail(request.getCustomerInformation().getEmail()); 
            sdkRequest.setCustomerInformation(sdkCustomer); 
        } else { throw new IllegalArgumentException("CustomerInformation cannot be null."); }

     // 2. Invoice information
        if (request.getInvoiceInformation() != null) {
            Invoicingv2invoicesInvoiceInformation sdkInvoiceInfo = new Invoicingv2invoicesInvoiceInformation();
            sdkInvoiceInfo.setInvoiceNumber(uniqueInvoiceNumber);
            sdkInvoiceInfo.setDescription(request.getInvoiceInformation().getDescription()); 

            // Conversión de Fecha (String a JodaTime)
            String dueDateString = request.getInvoiceInformation().getDueDate();
            if (dueDateString != null && !dueDateString.trim().isEmpty()) {
                 try {
                     java.time.LocalDate local = java.time.LocalDate.parse(dueDateString);
                     org.joda.time.LocalDate joda = new org.joda.time.LocalDate(local.getYear(), local.getMonthValue(), local.getDayOfMonth());
                     sdkInvoiceInfo.setDueDate(joda); // Setter
                 } catch (DateTimeParseException e) {
                     log.error("Invalid dueDate format: '{}'. Must be YYYY-MM-DD.", dueDateString, e);
                     throw new IllegalArgumentException("Invalid dueDate format: " + dueDateString);
                 }
            } else { throw new IllegalArgumentException("dueDate cannot be null or empty."); }
            
            sdkInvoiceInfo.setDeliveryMode("none"); // "none" para que no lo envíe Cybersource
            
            // ***** INICIO DE LA CORRECCIÓN *****
            // El código fuente del SDK v0.0.82 que me enviaste
            // confirma que este es el lugar correcto para estas líneas.
            sdkInvoiceInfo.setSendImmediately(false);
            sdkInvoiceInfo.setAllowPartialPayments(false);
            // ***** FIN DE LA CORRECCIÓN *****
            
            sdkRequest.setInvoiceInformation(sdkInvoiceInfo); 
        } else { throw new IllegalArgumentException("InvoiceInformation cannot be null."); }

        // 3. Order information & amount details
        if (request.getOrderInformation() != null && request.getOrderInformation().getAmountDetails() != null) {
            Invoicingv2invoicesOrderInformation sdkOrderInfo = new Invoicingv2invoicesOrderInformation();
            Invoicingv2invoicesOrderInformationAmountDetails sdkAmountDetails = new Invoicingv2invoicesOrderInformationAmountDetails();

            sdkAmountDetails.setTotalAmount( safeAmountToString(request.getOrderInformation().getAmountDetails().getTotalAmount()) );
            sdkAmountDetails.setCurrency( String.valueOf(request.getOrderInformation().getAmountDetails().getCurrency()) );
            
            sdkOrderInfo.setAmountDetails(sdkAmountDetails);

            // Line items
            List<Invoicingv2invoicesOrderInformationLineItems> sdkLineItems = new ArrayList<>(); 
            if (request.getOrderInformation().getLineItems() != null && !request.getOrderInformation().getLineItems().isEmpty()) { 
                for (OrderInformationLineItems li : request.getOrderInformation().getLineItems()) {
                    if (li != null) {
                        Invoicingv2invoicesOrderInformationLineItems sdkItem = new Invoicingv2invoicesOrderInformationLineItems();
                        
                        sdkItem.setProductSku(li.getProductSku() != null ? li.getProductSku() : "CONSULTA_MEDICA_VIRTUAL");
                        sdkItem.setProductName(li.getProductName());
                        
                        Integer quantity = 1; 
                        try { 
                            quantity = Integer.parseInt(String.valueOf(li.getQuantity())); 
                        } catch (Exception e) { 
                            log.warn("Invalid quantity '{}', using 1", li.getQuantity());
                        }
                        sdkItem.setQuantity(quantity);
                        
                        sdkItem.setUnitPrice( safeAmountToString(li.getUnitPrice()) );
                        sdkLineItems.add(sdkItem);
                    }
                }
            } else { throw new IllegalArgumentException("lineItems cannot be empty."); }
            sdkOrderInfo.setLineItems(sdkLineItems);

            sdkRequest.setOrderInformation(sdkOrderInfo);
        } else { throw new IllegalArgumentException("OrderInformation and AmountDetails cannot be null."); }
        
        // 4. --- AÑADIR MDFs AL SDK Request ---
        List<Invoicingv2invoicesMerchantDefinedFieldValues> sdkMdfs = new ArrayList<>();
        List<MerchantDefinedFieldValues> mdfList = request.getMerchantDefinedFieldValuesList() != null ? request.getMerchantDefinedFieldValuesList() : new ArrayList<>();
        if (!mdfList.isEmpty()) {
             for (MerchantDefinedFieldValues field : mdfList) {
                 if (field != null) {
                     Long definitionId = mapMerchantFieldNameToId(field.getDefinitionId());
                     if (definitionId != null) {
                         Invoicingv2invoicesMerchantDefinedFieldValues mdf = new Invoicingv2invoicesMerchantDefinedFieldValues();
                         mdf.setDefinitionId(definitionId); 
                         mdf.setValue(field.getValue());
                         sdkMdfs.add(mdf);
                     }
                 }
             }
        }
        if (!sdkMdfs.isEmpty()) {
             sdkRequest.setMerchantDefinedFieldValues(sdkMdfs); // <-- Setter
        }
        // ---------------------------------------
        
        // Se eliminó la corrección incorrecta que estaba aquí (en la raíz 'sdkRequest')

        return sdkRequest;
    }

    /**
     * Helper to safely convert Amounts (BigDecimal or String or other Number) to String.
     */
    private String safeAmountToString(Object amountObj) {
        if (amountObj == null) { return null; }
        if (amountObj instanceof BigDecimal) { return ((BigDecimal) amountObj).toPlainString(); }
        if (amountObj instanceof Number) { return amountObj.toString(); }
        return String.valueOf(amountObj); 
    }

    private Long mapMerchantFieldNameToId(String fieldNameOrId) {
        if (fieldNameOrId == null || fieldNameOrId.trim().isEmpty()) { return null; }
        switch (fieldNameOrId.trim()) {
            case "1": case "MerchantDefinedData1": return 1L;
            case "2": case "MerchantDefinedData2": return 2L;
            case "3": case "MerchantDefinedData3": return 3L;
            default: log.warn("Unmapped Merchant Defined Field ID/Name: {}", fieldNameOrId); return null;
        }
    }

    private String getMdfValue(List<MerchantDefinedFieldValues> mdfList, String definitionId, String defaultValue) {
        if (mdfList == null) { return defaultValue; }
        return mdfList.stream() 
            .filter(mdf -> mdf != null && definitionId.equals(mdf.getDefinitionId()))
            .findFirst()
            .map(MerchantDefinedFieldValues::getValue)
            .orElse(defaultValue);
    }
    
    private void WriteLogAudit(int code) { log.info("Audit log saved with response code: {}", code); }

}