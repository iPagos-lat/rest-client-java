package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object para encapsular los datos de una solicitud de correo electrónico.
 * Esto resuelve el error "EmailRequest cannot be resolved to a type".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String to;
    private String subject;
    private String body;
    // Opcional: private String from;
}
