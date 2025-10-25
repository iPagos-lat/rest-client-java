package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateInvoiceWOSendingRequest {
	private CustomerInformation customerInformation;
	private InvoiceInformation invoiceInformation;
	private MerchantInformation merchantInformation; // Cambiado Ifnormation a Information
	private OrderInformation orderInformation;
	private List<MerchantDefinedFieldValues> merchantDefinedFieldValuesList;
}