package com.ipagos.morganainvoices.controller;

import com.ipagos.morganainvoices.service.EmailService;
import com.ipagos.morganainvoices.service.CreateInvoiceWOSendingService; 

// Importaciones de DTOs de Cybersource (necesarias para construir el request)
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingRequest;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingResponse;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CustomerInformation;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.MerchantDefinedFieldValues;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.OrderInformationLineItems;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.AmountDetails;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.OrderInformation;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.InvoiceInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // Usar ArrayList
import java.util.List; // Usar List

@Controller
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

    @Autowired
    private EmailService emailService;

    // Inyección del servicio de Cybersource
    @Autowired
    private CreateInvoiceWOSendingService cybersourceService;

    @GetMapping("/")
    public String index() {
        return "index"; 
    }

    @PostMapping("/enviar-factura")
    @ResponseBody
    public ResponseEntity<String> enviarFactura(
            @RequestParam("toEmail") String toEmail,
            @RequestParam("patientName") String patientName,
            @RequestParam("doctorName") String doctorName,
            @RequestParam("date") String date,
            @RequestParam("time") String time,
            @RequestParam("amount") String amount,
            @RequestParam("telefono") String telefono
            // Eliminamos paymentLink y dueDate de los parámetros
        ) {

        log.info("Solicitud recibida para: {}", patientName);

        try {
            // --- 1. CONSTRUIR EL REQUEST DE CYBERSOURCE ---
            // (Esta es la lógica que "pegamos" y que faltaba)
            log.info("Construyendo DTO de Cybersource para generar el enlace...");

            // Cliente
            CustomerInformation customer = new CustomerInformation();
            customer.setName(patientName);
            customer.setEmail(toEmail); // El servicio lo reemplazará por el email de archivo, pero lo enviamos aquí

            // Detalles del Monto
            AmountDetails amountDetails = new AmountDetails();
            amountDetails.setTotalAmount(amount);
            amountDetails.setCurrency("MXN"); // Moneda (Asegúrate que sea la correcta)

            // Línea de Items
            OrderInformationLineItems lineItem = new OrderInformationLineItems();
            lineItem.setProductName("Consulta " + doctorName);
            lineItem.setQuantity(1); // Cantidad fija
            lineItem.setUnitPrice(amount);
            
            List<OrderInformationLineItems> lineItems = new ArrayList<>();
            lineItems.add(lineItem);

            // Información de la Orden
            OrderInformation orderInfo = new OrderInformation();
            orderInfo.setAmountDetails(amountDetails);
            orderInfo.setLineItems(lineItems);

            // Información de la Factura
            InvoiceInformation invoiceInfo = new InvoiceInformation();
            // La fecha de vencimiento de la factura de Cybersource es la fecha de la cita
            invoiceInfo.setDueDate(date); 
            invoiceInfo.setDescription("Cita prepagada para " + patientName + " con Dr(a). " + doctorName);
            invoiceInfo.setAllowPartialPayments(false); // O según tu lógica de negocio


            // Construir el Request Principal
            CreateInvoiceWOSendingRequest csRequest = new CreateInvoiceWOSendingRequest();
            csRequest.setCustomerInformation(customer);
            csRequest.setOrderInformation(orderInfo);
            csRequest.setInvoiceInformation(invoiceInfo);


            // --- 2. GENERAR EL ENLACE DE PAGO DE CYBERSOURCE ---
            log.info("Llamando al servicio de Cybersource para generar el enlace...");
            CreateInvoiceWOSendingResponse csResponse = cybersourceService.createInvoice(csRequest);

            String realPaymentLink = csResponse.getPaymentLink();
            
            if (realPaymentLink == null || realPaymentLink.isEmpty()) {
                log.error("Cybersource devolvió una respuesta exitosa PERO el enlace de pago vino nulo.");
                throw new RuntimeException("No se pudo obtener el enlace de pago de Cybersource.");
            }
            
            log.info("Enlace de pago de Cybersource generado exitosamente.");


            // --- 3. LÓGICA DE CORREO (Usando el enlace real) ---
            
            // (Cálculo de fecha de vencimiento para el *correo*)
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate appointmentDate = LocalDate.parse(date, formatter);
            LocalDate calculatedDueDate = appointmentDate.minusDays(1);
            String dueDateString = calculatedDueDate.format(formatter); // Fecha para el texto del correo

            // LLAMADA AL SERVICIO DE CORREO (ahora con el enlace real)
            emailService.sendAppointmentConfirmation(
                toEmail,
                patientName,
                doctorName,
                date, 
                time, 
                amount,
                realPaymentLink, // <-- AQUÍ SE "PEGA" EL ENLACE REAL
                dueDateString 
            );

            return ResponseEntity.status(201).body("Factura enviada y correo de confirmación iniciado.");

        } catch (Exception e) {
            // Esto capturará errores de Cybersource (ej. 401 Bad Credentials) O errores de SMTP
            log.error("Error al procesar la solicitud (Cybersource o Correo): {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }
}
