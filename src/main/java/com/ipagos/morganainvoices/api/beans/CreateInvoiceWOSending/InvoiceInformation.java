package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InvoiceInformation {
	private String invoiceNumber;
	private String description;
	private String dueDate; // Mantenido como String
	private boolean sendImmediately;
	private boolean allowPartialPayments;
	private String deliveryMode;
}