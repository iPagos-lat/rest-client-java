package com.ipagos.morganainvoices.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingRequest;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingResponse;
import com.ipagos.morganainvoices.service.CreateInvoiceWOSendingService;

import Invokers.ApiException;
import java.util.Map; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "*") 
public class CreateInvoiceWOSendingController {

    // --- Definición Manual del Logger (en lugar de @Slf4j) ---
	private static final Logger log = LoggerFactory.getLogger(CreateInvoiceWOSendingController.class);

	@Autowired 
	CreateInvoiceWOSendingService service;

	@PostMapping(path = "/get-link")
	public ResponseEntity<?> post(
			@RequestBody CreateInvoiceWOSendingRequest createInvoiceWOSendingRequest) {
		
        log.info("Post: /get-link");
        log.info("Request Body: {}", createInvoiceWOSendingRequest.toString()); 

        try {
            // 1. Llamar al servicio principal
    		CreateInvoiceWOSendingResponse createInvoiceWOSendingResponse = this.service
    				.createInvoice(createInvoiceWOSendingRequest);

    		// 2. Devolver OK si el enlace se generó
    		if (createInvoiceWOSendingResponse != null && createInvoiceWOSendingResponse.getPaymentLink() != null) {
                log.info("Éxito: Enlace de pago generado.");
                return new ResponseEntity<>(createInvoiceWOSendingResponse, HttpStatus.CREATED);
    		} else {
    			log.error("Fallo al crear la factura o al obtener el enlace (Respuesta nula del servicio).");
    			return new ResponseEntity<>(Map.of("message", "No se pudo obtener el enlace de pago."), HttpStatus.INTERNAL_SERVER_ERROR);
    		}
        
        } catch (ApiException e) {
            // 3. Capturar errores de Cybersource
            log.error("ApiException (Cybersource) al procesar la solicitud: {}", e.getResponseBody(), e);
            return new ResponseEntity<>(Map.of("message", "Error de Cybersource: " + e.getMessage()), HttpStatus.valueOf(e.getCode() > 0 ? e.getCode() : 500));
        
        } catch (Exception e) {
            // 4. Capturar cualquier otro error (Email, Twilio, Configuración, etc.)
            log.error("Fallo inesperado al procesar /get-link: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}
}