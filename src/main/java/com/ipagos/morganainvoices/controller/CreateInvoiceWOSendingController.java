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
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j // Quitado AllArgsConstructor, @Autowired maneja la inyección
@CrossOrigin(origins = "*")
public class CreateInvoiceWOSendingController {

	@Autowired // Inyección por campo
	CreateInvoiceWOSendingService service;

	@PostMapping(path = "/get-link")
	public ResponseEntity<CreateInvoiceWOSendingResponse> post(
			@RequestBody CreateInvoiceWOSendingRequest createInvoiceWOSendingRequest) {
		log.info("Post: get-link");
        log.info("Request Body: {}", createInvoiceWOSendingRequest); // Log para depurar entrada

		CreateInvoiceWOSendingResponse createInvoiceWOSendingResponse = this.service
				.createInvoice(createInvoiceWOSendingRequest);

        // Devolver OK si el enlace se generó, o error si no
        if (createInvoiceWOSendingResponse != null && createInvoiceWOSendingResponse.getPaymentLink() != null) {
            return new ResponseEntity<>(createInvoiceWOSendingResponse, HttpStatus.OK); // O HttpStatus.CREATED (201)
        } else {
             log.error("Fallo al crear la factura o al obtener el enlace.");
             // Puedes devolver un objeto de respuesta de error si lo tienes definido
             return new ResponseEntity<>(createInvoiceWOSendingResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}
}