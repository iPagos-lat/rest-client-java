package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerInformation {
    private String name;
    private String email;
}