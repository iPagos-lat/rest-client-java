package com.ipagos.morganainvoices.service;

import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.EmailRequest;

/**
 * Interfaz para el servicio de envío de correos.
 * Contiene la definición original para la confirmación de citas y un método genérico para depuración.
 */
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

    /**
     * Método genérico para el envío de correo simple (utilizado para pruebas SMTP/Debug).
     * @param request Objeto EmailRequest con destino, asunto y cuerpo.
     * @return true si el envío es exitoso.
     */
    boolean sendEmail(EmailRequest request); 
}
