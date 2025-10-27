package com.ipagos.morganainvoices.controller;

import com.ipagos.morganainvoices.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

// Importaciones necesarias para manejar fechas
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

    @Autowired
    private EmailService emailService;

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
            @RequestParam("date") String date, // Fecha de la cita (ej: "2025-10-30")
            @RequestParam("time") String time,
            @RequestParam("amount") String amount,
            @RequestParam("paymentLink") String paymentLink
            // Eliminamos dueDate del @RequestParam
        ) {

        log.info("Solicitud recibida para: {}", patientName);

        try {
            // --- INICIO DE LA CORRECCIÓN DE FECHA ---
            
            // 1. Definir el formato de fecha que esperamos (YYYY-MM-DD)
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            
            // 2. Convertir la fecha de la cita (String) a un objeto LocalDate
            LocalDate appointmentDate = LocalDate.parse(date, formatter);
            
            // 3. Calcular la fecha de vencimiento (ej: 1 día antes de la cita)
            LocalDate calculatedDueDate = appointmentDate.minusDays(1);
            
            // 4. Convertir la nueva fecha de vencimiento de vuelta a String
            String dueDateString = calculatedDueDate.format(formatter);
            
            // --- FIN DE LA CORRECCIÓN DE FECHA ---


            // LLAMADA AL SERVICIO DE CORREO (ahora con la fecha correcta)
            emailService.sendAppointmentConfirmation(
                toEmail,
                patientName,
                doctorName,
                date, // Fecha de la cita
                time, // Hora de la cita
                amount,
                paymentLink,
                dueDateString // La fecha de vencimiento calculada
            );

            return ResponseEntity.status(201).body("Factura enviada y correo de confirmación iniciado.");

        } catch (Exception e) {
            log.error("Error al procesar la solicitud o enviar el correo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }
}

