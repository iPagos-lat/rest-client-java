package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Builder;
import lombok.Data;
import java.util.Map; // Import Map

@Data
@Builder
public class CreateInvoiceWOSendingResponse {
    private Map<String, Object> result; // Usa Map<String, Object>
    private String paymentLink;
}