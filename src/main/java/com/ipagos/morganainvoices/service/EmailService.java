package com.ipagos.morganainvoices.service;

public interface EmailService {
    /**
     * Envía un correo HTML de confirmación de cita con el enlace de pago incrustado.
     */
    void sendAppointmentConfirmation(
        String toEmail, 
        String patientName, 
        String doctorName, 
        String date, 
        String time, 
        String amount, 
        String paymentLink,
        String dueDate
    );
}