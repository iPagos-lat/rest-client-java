package com.ipagos.morganainvoices.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendAppointmentConfirmation(String toEmail, String patientName, String doctorName, String date, String time, String amount, String paymentLink, String dueDate) throws Exception {
        log.info("Intentando enviar correo de confirmación a: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlBody = String.format(
                "<html><body>" +
                "<h2>Confirmación de Cita y Solicitud de Pago</h2>" +
                "<p>Hola %s,</p>" +
                "<p>Tu cita con <strong>%s</strong> ha sido registrada para el <strong>%s</strong> a las <strong>%s</strong>.</p>" +
                "<p>El monto total es: <strong>$%s MXN</strong>.</p>" +
                "<p>Para confirmar tu cita, por favor realiza tu pago usando el siguiente enlace:</p>" +
                "<a href='%s' style='font-size:16px; padding:10px 15px; background-color:#007bff; color:white; text-decoration:none; border-radius:5px;'>Pagar Ahora</a>" +
                "<br/><br/><p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>" +
                "<p>%s</p>" +
                "</body></html>",
                patientName, doctorName, date, time, amount, paymentLink, paymentLink
            );

            helper.setTo(toEmail);
            helper.setSubject("Confirmación de Cita y Enlace de Pago");
            helper.setText(htmlBody, true); // true = HTML
            helper.setFrom("sinaptikon@recibos.ipagos.lat"); // O inyectarlo desde properties

            mailSender.send(message);
            log.info("Correo de confirmación enviado exitosamente vía SMTP a: {}", toEmail);

        } catch (Exception e) {
            log.error("Fallo al enviar el correo a {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Fallo al enviar el correo de confirmación.", e);
        }
    }
}