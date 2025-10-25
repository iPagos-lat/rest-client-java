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


@Service
public class CreateInvoiceWOSendingServiceImpl implements CreateInvoiceWOSendingService {

    private static final Logger log = LoggerFactory.getLogger(CreateInvoiceWOSendingServiceImpl.class);

    private static final String DEFAULT_CURRENCY = "MXN";
    // Define el correo interno para archivar el recibo de Cybersource. (SOLUCIÓN CORREO DOBLE)
    private static final String ARCHIVE_EMAIL = "archivo@ipagos.lat"; 

    // --- Inyección de Dependencias ---
    @Autowired private InvoicesApi invoicesApi; 
    @Autowired private EmailService emailService;
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


    @Override
    @Transactional
    public CreateInvoiceWOSendingResponse createInvoice(CreateInvoiceWOSendingRequest request) {
        String createdInvoiceId = null;
        Map<String, Object> createResultMap = null;
        String finalPaymentLink = null;
        
        // 1. GUARDAR el email original del cliente
        String finalCustomerEmail = request.getCustomerInformation().getEmail();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String shortTimestamp = timestamp.substring(0, Math.min(timestamp.length(), 12));
        String uniqueInvoiceNumber = "SINAPT" + shortTimestamp;
        log.info("Generated uniqueInvoiceNumber (20 chars): {}", uniqueInvoiceNumber);

        try {
            // 2. REEMPLAZAR TEMPORALMENTE el email del cliente con el email de archivo
            request.getCustomerInformation().setEmail(ARCHIVE_EMAIL);
            log.info("📧 Customer email temporarily changed to {} for Cybersource payload.", ARCHIVE_EMAIL);

            Map<String, Object> payloadMap = buildPayloadMap(request, uniqueInvoiceNumber);
            
            // 3. RESTAURAR el email original del cliente
            request.getCustomerInformation().setEmail(finalCustomerEmail);

            String jsonPayload = "";
            try {
                jsonPayload = objectMapper.writeValueAsString(payloadMap);
            } catch (Exception e) {
                 log.error("Error fatal al serializar payload manual con Jackson: {}", e.getMessage());
                 throw new RuntimeException("Error creando JSON para Cybersource", e);
            }

            // --- LLAMADA HTTP MANUAL CON RestTemplate (Creación de Factura) ---
            String resourcePath = "/invoicing/v2/invoices";
            HttpMethod method = HttpMethod.POST;
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
            headers.add(HttpHeaders.ACCEPT, "application/json");
            headers.add("v-c-merchant-id", merchantId);
            headers.add("User-Agent", "MySpringApp/1.0");

            Map<String, String> signatureHeaders = generateCybersourceSignature(method.name(), resourcePath, jsonPayload, runEnvironmentHost);
            headers.setAll(signatureHeaders);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonPayload, headers);
            
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://" + runEnvironmentHost + resourcePath, method, httpEntity, new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                createResultMap = response.getBody();
                int statusCode = response.getStatusCode().value();

                if (statusCode != 201 || createResultMap == null || createResultMap.get("id") == null) {
                     throw new IllegalStateException("Invoice creation failed via manual HTTP: ID not found or invalid status code.");
                }
                createdInvoiceId = String.valueOf(createResultMap.get("id"));
                WriteLogAudit(statusCode);

            } catch (HttpClientErrorException e) {
                 log.error("HttpClientErrorException calling Cybersource (Manual HTTP). Code: {}. Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                 WriteLogAudit(e.getStatusCode().value());
                 throw new RuntimeException("Error calling Cybersource: " + e.getStatusCode(), e);
            }


            // 4. --- ACTIVACIÓN Y EXTRACCIÓN DEL LINK ---
            log.warn("Llamando a performSendAction (Link Activation). CS notification sent to ARCHIVE_EMAIL.");
            
            // Almacena la respuesta de performSendAction
            Object sendResultRaw = invoicesApi.performSendAction(createdInvoiceId);
            
            // Casteamos la respuesta para acceder a los campos (omitiendo la llamada getInvoice que fallaba por GSON)
            InvoicingV2InvoicesSend200Response sendResult = (InvoicingV2InvoicesSend200Response) sendResultRaw;

            log.info("CyberSource Send action successful. Link is now active.");

            // 5. EXTRACCIÓN DEL LINK DE PAGO de la respuesta del performSendAction
            if (sendResult != null && sendResult.getInvoiceInformation() != null && sendResult.getInvoiceInformation().getPaymentLink() != null) {
                finalPaymentLink = sendResult.getInvoiceInformation().getPaymentLink(); 
                log.info("🔗 Payment Link FINAL (Extraído de performSendAction): {}", finalPaymentLink);
            } else { 
                 log.error("⚠️ FINAL PAYMENT LINK NOT FOUND in performSendAction response. Fallback required.");
            }

            // 6. --- Enviar correo (CORREO PERSONALIZADO) ---
            if (finalPaymentLink != null) {
                // 7. EXTRACCIÓN DINÁMICA DE DATOS DEL REQUEST
                // Extracción de productName
                String productName = request.getOrderInformation().getLineItems().stream()
                    .findFirst()
                    .map(li -> li.getProductName())
                    .orElse("Consulta Médica"); 
                    
                // Limpieza del nombre (elimina "Consulta ")
                String doctorName = productName.replaceFirst("Consulta\\s*", "").trim(); 
                
                // Extracción de la hora (desde MDF #2)
                String timeStr = request.getMerchantDefinedFieldValuesList().stream()
                    .filter(mdf -> mdf.getDefinitionId() != null && "2".equals(mdf.getDefinitionId())) 
                    .map(mdf -> mdf.getValue())
                    .findFirst()
                    .orElse("12:00");
                
                // Formateo de la hora
                String formattedTime = timeStr;
                try { 
                    formattedTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                                             .format(DateTimeFormatter.ofPattern("hh:mm a")); 
                } catch (DateTimeParseException ignored) {
                    log.warn("Formato de hora ({}) no estándar, usando valor original.", timeStr);
                }
                
                // Resto de variables:
                String date = request.getInvoiceInformation().getDueDate();
                String amount = request.getOrderInformation().getAmountDetails().getTotalAmount();
                String patientName = request.getCustomerInformation().getName(); 
                
                try {
                     // USAR el finalCustomerEmail (el original) para el envío personalizado
                     emailService.sendAppointmentConfirmation(finalCustomerEmail, patientName, doctorName, date, formattedTime, amount, finalPaymentLink, date);
                     log.info("Correo personalizado enviado a: {}, Doctor: {}", finalCustomerEmail, doctorName);
                } catch (Exception e) { 
                    log.error("Fallo al enviar correo: {}", e.getMessage(), e); 
                }
            } else { log.error("Link nulo, correo no enviado."); }
            // -----------------------------------

        } catch (IllegalArgumentException e) { 
             log.error("Input data error: {}", e.getMessage(), e);
             WriteLogAudit(400);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) { 
             log.error("Error criptográfico al generar firma Cybersource: {}", e.getMessage(), e);
             WriteLogAudit(500); 
        } catch (Exception e) { 
             log.error("Unexpected error in createInvoice (Manual HTTP): {}", e.getMessage(), e);
             WriteLogAudit(500); 
        }

        // Devolvemos la respuesta
        return CreateInvoiceWOSendingResponse.builder()
                 .result(createResultMap) 
                 .paymentLink(finalPaymentLink)
                 .build();
    }


    // =========================================================================
    // ==================== MÉTODOS AUXILIARES =================================
    // =========================================================================

    private Map<String, Object> buildPayloadMap(CreateInvoiceWOSendingRequest request, String uniqueInvoiceNumber) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> customerInfo = new HashMap<>();
        if (request.getCustomerInformation() == null) throw new IllegalArgumentException("CustomerInformation no puede ser nulo.");
        customerInfo.put("name", request.getCustomerInformation().getName());
        customerInfo.put("email", request.getCustomerInformation().getEmail());
        payload.put("customerInformation", customerInfo);

        Map<String, Object> invoiceInfo = new HashMap<>();
        if (request.getInvoiceInformation() == null) throw new IllegalArgumentException("InvoiceInformation no puede ser nulo.");
        invoiceInfo.put("invoiceNumber", uniqueInvoiceNumber);
        invoiceInfo.put("description", request.getInvoiceInformation().getDescription());
        String dueDate = request.getInvoiceInformation().getDueDate();
        if (dueDate == null || dueDate.trim().isEmpty()) throw new IllegalArgumentException("dueDate no puede ser nulo o vacío.");
        try { java.time.LocalDate.parse(dueDate); } catch (Exception e) { throw new IllegalArgumentException("Formato de dueDate inválido, debe ser YYYY-MM-DD: " + dueDate); }
        invoiceInfo.put("dueDate", dueDate);
        invoiceInfo.put("allowPartialPayments", request.getInvoiceInformation().isAllowPartialPayments());
        invoiceInfo.put("deliveryMode", "none"); 
        payload.put("invoiceInformation", invoiceInfo);

        Map<String, Object> orderInfo = new HashMap<>();
        Map<String, Object> amountDetails = new HashMap<>();
        if (request.getOrderInformation() == null || request.getOrderInformation().getAmountDetails() == null) throw new IllegalArgumentException("OrderInformation y AmountDetails no pueden ser nulos.");
        amountDetails.put("totalAmount", request.getOrderInformation().getAmountDetails().getTotalAmount());
        amountDetails.put("currency", DEFAULT_CURRENCY);
        orderInfo.put("amountDetails", amountDetails);

        List<Map<String, Object>> lineItems = new ArrayList<>();
        if (request.getOrderInformation().getLineItems() != null) {
            for (OrderInformationLineItems li : request.getOrderInformation().getLineItems()) {
                if (li != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productSku", li.getProductSku() != null ? li.getProductSku() : "CONSULTA_MEDICA_VIRTUAL");
                    item.put("productName", li.getProductName());
                    try { item.put("quantity", Integer.parseInt(String.valueOf(li.getQuantity()))); }
                    catch (NumberFormatException e) { item.put("quantity", 1); log.warn("Cantidad inválida '{}', usando 1", li.getQuantity()); }
                    item.put("unitPrice", li.getUnitPrice());
                    lineItems.add(item);
                }
            }
        }
        if (lineItems.isEmpty()) { throw new IllegalArgumentException("La lista 'lineItems' no puede estar vacía."); }
        orderInfo.put("lineItems", lineItems);
        payload.put("orderInformation", orderInfo);

        return payload;
    }


    private Map<String, String> generateCybersourceSignature(String httpMethod, String resourcePath, String payload, String host)
             throws NoSuchAlgorithmException, InvalidKeyException {

        log.debug("Generating Cybersource Signature (Minimal Headers)...");
        Map<String, String> signatureHeaders = new HashMap<>();

        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        signatureHeaders.put("Date", date);
        signatureHeaders.put("Host", host);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] payloadBytes = (payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8);
        byte[] hash = digest.digest(payloadBytes);
        String digestHeaderValue = Base64.getEncoder().encodeToString(hash);
        String digestHeader = "SHA-256=" + digestHeaderValue;
        signatureHeaders.put("Digest", digestHeader);
        log.debug("Generated Digest: {}", digestHeader);

        String headersToSignString = "host date request-target digest v-c-merchant-id";
        String requestTarget = httpMethod.toLowerCase() + " " + resourcePath;

        StringBuilder signingStringBuilder = new StringBuilder();
        signingStringBuilder.append("host: ").append(host).append("\n");
        signingStringBuilder.append("date: ").append(date).append("\n");
        signingStringBuilder.append("request-target: ").append(requestTarget).append("\n");
        signingStringBuilder.append("digest: ").append(digestHeader).append("\n");
        signingStringBuilder.append("v-c-merchant-id: ").append(merchantId);

        String signingString = signingStringBuilder.toString();
        log.debug("String to Sign:\n{}", signingString);

        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        byte[] secretKeyBytes;
        try { secretKeyBytes = Base64.getDecoder().decode(merchantSecretKey); }
        catch (IllegalArgumentException e) { throw new InvalidKeyException("Secret Key is not valid Base64", e); }
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] signatureBytes = hmacSha256.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);
        log.debug("Generated Signature (Base64): {}", signature);

        String signatureHeaderValue = String.format(
            "keyid=\"%s\", algorithm=\"HmacSHA256\", headers=\"%s\", signature=\"%s\"",
            merchantKeyId,
            headersToSignString,
            signature
        );
        signatureHeaders.put("Signature", signatureHeaderValue);

        log.info("Generated Cybersource Signature Headers (Date, Host, Digest, Signature)");
        return signatureHeaders;
    }

    private void WriteLogAudit(int code) { log.info("Audit log saved with response code: {}", code); }

}