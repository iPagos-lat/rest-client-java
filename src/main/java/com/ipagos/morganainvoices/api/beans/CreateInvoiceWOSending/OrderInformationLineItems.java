package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderInformationLineItems {
    private String productSku;
    private String productName;
    private Integer quantity; // Usar Integer en lugar de int para permitir null
    private String unitPrice;
}