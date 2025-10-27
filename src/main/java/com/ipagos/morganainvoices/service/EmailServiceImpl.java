package com.ipagos.morganainvoices.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.EmailRequest;

// Importamos el DTO


@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String SENDER_EMAIL = "sinaptikon@recibos.ipagos.lat"; // Tu correo de remite

    @Autowired
    private JavaMailSender mailSender; // Utiliza tu configuración SMTP

    @Override
    public void sendAppointmentConfirmation(
            String toEmail, 
            String patientName, 
            String doctorName, 
            String date, 
            String time, 
            String amount, 
            String paymentLink,
            String dueDate) // Este parámetro (dueDate) ya no se usará en la plantilla, pero se mantiene por la firma del método.
    {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // El segundo parámetro 'true' permite el contenido HTML y adjuntos
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(SENDER_EMAIL);
            helper.setTo(toEmail);
            helper.setSubject("Confirmación de cita médica y enlace de pago");

            // Pasamos el dueDate, aunque el HTML lo ignore
            String htmlContent = buildEmailHtml(patientName, doctorName, date, time, amount, paymentLink, dueDate);
            helper.setText(htmlContent, true); // true indica contenido HTML

            mailSender.send(message);
            log.info("Correo de confirmación enviado exitosamente vía SMTP a: {}", toEmail);

        } catch (Exception e) {
            log.error("Fallo al enviar el correo a {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Fallo al enviar el correo de confirmación.", e);
        }
    }

    /**
     * Construye el cuerpo HTML del correo con el botón de pago y estilos corporativos.
     */
    private String buildEmailHtml(
            String patientName, 
            String doctorName, 
            String date, 
            String time, 
            String amount, 
            String paymentLink, 
            String dueDate) { // dueDate es ignorado
        
        // Colores corporativos basados en el análisis del HTML de iPagos/Sinaptikon
        final String COLOR_CORPORATIVO = "#3299CD"; 
        final String COLOR_ADVERTENCIA = "#e74c3c"; 
        final String COLOR_FONDO_CLARO = "#F8F8F8"; 
        final String COLOR_TEXTO = "#333333";
        final String BASE_FONT = "font-family: Arial, sans-serif; line-height: 1.6;";

        // ⚠️ NOTA: Usar una URL pública y segura para el logo es crucial
        final String LOGO_URL = "https://sinaptikon.com/wp-content/uploads/2025/05/sinaptikon_SIN_SLOGAN-300x139.png"; 


        // --- CÓDIGO HTML COMPLETO DE LA PLANTILLA ---
        String template = 
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "    <title>Confirmación de Cita</title>" +
            "</head>" +
            "<body style='" + BASE_FONT + " color: " + COLOR_TEXTO + "; margin: 0; padding: 0; background-color: #f6f6f6;'>" +
            "    <div style='max-width: 600px; margin: 20px auto; border: 1px solid #e0e0e0; border-radius: 8px; background-color: white; box-shadow: 0 4px 6px rgba(0,0,0,0.05);'>" +
            "        " +
            "        " +
            "        <div style='padding: 20px 30px; border-bottom: 2px solid " + COLOR_CORPORATIVO + "; background-color: " + COLOR_FONDO_CLARO + "; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;'>" +
            "             <a href='https://www.sinaptikon.com' style='text-decoration: none; display: inline-block;'>" +
            "                 <img src='" + LOGO_URL + "' alt='Logo Sinaptikon' style='max-width: 150px; height: auto; display: block; margin: 0 auto;'>" +
            "             </a>" +
            "             <h1 style='color: " + COLOR_CORPORATIVO + "; margin: 10px 0 0 0; font-size: 20px; font-weight: normal;'>" +
            "                 Confirmación de Cita" +
            "             </h1>" +
            "        </div>" +
            "        " +
            "        " +
            "        <div style='padding: 30px;'>" +
            "            <h2 style='color: " + COLOR_CORPORATIVO + "; margin-top: 0;'>Detalles de Pago</h2>" +
            "            <p>Estimado/a <strong>" + patientName + "</strong>,</p>" +
            "            <p>Gracias por agendar tu consulta con el <strong>Dr(a). " + doctorName + "</strong>. Queremos recordarte los detalles de tu cita y facilitarte el proceso de pago para asegurar tu lugar:</p>" +
            "            " +
            "            " +
            "            <div style='background-color: #f4f4f4; padding: 18px; border-radius: 6px; margin: 25px 0; border-left: 5px solid " + COLOR_CORPORATIVO + ";'>" +
            "                <p style='margin: 0 0 5px 0;'><strong>🗓 Fecha de la cita:</strong> " + date + "</p>" +
            "                <p style='margin: 0 0 5px 0;'><strong>⏰ Hora:</strong> " + time + "</p>" +
            "                <p style='margin: 15px 0 0 0; font-size: 1.2em; color: " + COLOR_CORPORATIVO + ";'><strong>💳 Monto a pagar:</strong> " + amount + " MXN</p>" +
            "            </div>" +
            "            " +
            "            <p style='text-align: center; margin: 30px 0 15px 0;'><strong>Para confirmar tu espacio, haz clic en el botón para realizar el pago:</strong></p>" +
            "            " +
            "            " +
            "            <div style='text-align: center; margin-bottom: 30px;'>" +
            "                <a href='" + paymentLink + "' style='display: inline-block; padding: 15px 30px; background-color: " + COLOR_CORPORATIVO + "; color: white; text-decoration: none; border-radius: 8px; font-size: 18px; font-weight: bold;'>" +
            "                    PAGAR CITA Y CONFIRMAR" +
            "                </a>" +
            "            </div>" +
            "            " +
            "            <p>Al momento de confirmar tu pago, te haremos llegar la liga de videoconferencia en la cual podrás recibir tu consulta en línea.</p>" +
            "            " +
            "            " +
            "            <!-- INICIO DE LA CORRECCIÓN: Texto fijo en lugar de la variable dueDate -->" +
            "            <p style='color: " + COLOR_ADVERTENCIA + "; font-weight: bold; padding: 10px; background-color: #FEEAE8; border-radius: 4px;'>Para confirmar tu espacio y no perder el espacio disponible, es necesario realizar el pago en las siguientes 24 horas.</p>" +
            "            <!-- FIN DE LA CORRECCIÓN -->" +
            "            " +
            "            <p style='font-size: 0.9em;'>En caso de no recibir el pago en ese plazo, la cita será cancelada automáticamente y deberás reprogramarla según disponibilidad.</p>" +
            "        " +
            "        </div>" +
            "        " +
            "        " +
            "        <div style='border-top: 1px solid #e0e0e0; padding: 20px 30px; background-color: " + COLOR_CORPORATIVO + "; font-size: 0.9em; color: white; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;'>" +
            "            <p style='margin: 0 0 5px 0; font-weight: bold;'>Gracias por tu confianza, Equipo Sinaptikon</p>" +
            "            <p style='margin: 0;'><a href='http://www.sinaptikon.com' style='color: white; text-decoration: underline;'>www.sinaptikon.com</a></p>" +
            "            <p style='margin: 0;'>info@sinaptikon.com</p>" +
            "            <p style='margin: 0;'>+523335550057</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";

        return template;
    }

    /**
     * Implementación del método genérico de la interfaz (requerido para compilar).
     * Este método NO se usa en el flujo de /enviar-factura.
     */
    @Override
    public boolean sendEmail(EmailRequest request) {
        // Lógica de depuración simple
        log.warn("Método 'sendEmail' genérico fue llamado, pero no está implementado.");
        // TODO: Implementar si se necesita un envío de correo genérico.
        return false;
    }
}