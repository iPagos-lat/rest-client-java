package com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Data genera getters, setters, toString, equals y hashCode
// @AllArgsConstructor y @NoArgsConstructor generan constructores
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDTO {
    private Long id;
    private String descripcion;
    private Double monto;
    private String cliente;
}
