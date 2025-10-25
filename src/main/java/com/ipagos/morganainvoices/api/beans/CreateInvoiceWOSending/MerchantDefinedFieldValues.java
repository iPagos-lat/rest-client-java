package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;


import lombok.Data;

@Data
public class MerchantDefinedFieldValues {
 // ✅ Ambos DEBEN ser String para que coincidan con el JSON ("1", "Juan Aviles")
 private String definitionId; 
 private String value;        
}