package com.ipagos.morganainvoices.service;

public interface EmailService {
    void sendAppointmentConfirmation(
        String toEmail, 
        String patientName, 
        String doctorName, 
        String date, 
        String time, 
        String amount, 
        String paymentLink, 
        String dueDate
    ) throws Exception;
}