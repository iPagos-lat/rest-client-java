package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderInformation {
    private AmountDetails amountDetails;
    private List<OrderInformationLineItems> lineItems;
}