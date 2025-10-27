package com.ipagos.morganainvoices.controller;

import com.ipagos.morganainvoices.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

// Usamos @Controller para poder usar @GetMapping para servir HTML (vistas)
@Controller
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

    // INYECCIÓN DEL SERVICIO DE CORREO
    @Autowired
    private EmailService emailService;

    // 1. Endpoint para servir la página HTML principal
    @GetMapping("/")
    public String index() {
        // Asume que tienes un archivo index.html en src/main/resources/static
        // o un template index en src/main/resources/templates (si usas Thymeleaf/FreeMarker)
        return "index"; 
    }

    // 2. Endpoint que recibe la solicitud del formulario HTML
    @PostMapping("/enviar-factura")
    @ResponseBody // Indica que este método devuelve datos (no una vista)
    public ResponseEntity<String> enviarFactura(
            @RequestParam("toEmail") String toEmail,
            @RequestParam("patientName") String patientName,
            @RequestParam("doctorName") String doctorName,
            @RequestParam("date") String date,
            @RequestParam("time") String time,
            @RequestParam("amount") String amount,
            @RequestParam("paymentLink") String paymentLink,
            @RequestParam("dueDate") String dueDate) {

        log.info("Solicitud recibida para: {}", patientName);

        try {
            // LLAMADA AL SERVICIO DE CORREO
            emailService.sendAppointmentConfirmation(
                toEmail,
                patientName,
                doctorName,
                date,
                time,
                amount,
                paymentLink,
                dueDate
            );

            // Si el correo se intenta enviar, devuelve éxito. 
            // La excepción de SMTP, si ocurre, ahora se propagará desde el servicio.
            return ResponseEntity.status(201).body("Factura enviada y correo de confirmación iniciado.");

        } catch (Exception e) {
            // Captura la excepción propagada desde EmailServiceImpl (que contiene el error SMTP)
            log.error("Error al procesar la solicitud o enviar el correo: {}", e.getMessage(), e);
            
            // Devuelve un error 500 para indicar que el proceso falló
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }
}
