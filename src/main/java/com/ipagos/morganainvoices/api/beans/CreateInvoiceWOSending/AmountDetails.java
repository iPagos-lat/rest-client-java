package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AmountDetails {
    private String totalAmount;
    private String currency;
}