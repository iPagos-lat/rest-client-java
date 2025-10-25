package com.ipagos.morganainvoices.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.FacturaDTO;

@RestController
public class FacturaController {

    // Mapea la petición POST a la URL raíz del servicio ("/")
    @PostMapping("/") 
    public ResponseEntity<String> recibirFactura(@RequestBody FacturaDTO factura) {
        
        // Simulación de procesamiento (esto se imprimirá en los logs de Cloud Run)
        System.out.println("Solicitud recibida para: " + factura.getCliente());

        String respuesta = String.format(
            "Factura #%d de %s recibida. Monto: %.2f", 
            factura.getId(), factura.getCliente(), factura.getMonto()
        );

        // Devuelve 201 Created (HTTP OK)
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }
}
