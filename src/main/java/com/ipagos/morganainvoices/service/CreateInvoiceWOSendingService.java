package com.ipagos.morganainvoices.service;

import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingRequest;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingResponse;
// No longer need Consulta import
// import com.ipagos.morganainvoices.model.Consulta; 

public interface CreateInvoiceWOSendingService {

    /**
     * Creates an invoice by calling Cybersource using the provided request data.
     */
    CreateInvoiceWOSendingResponse createInvoice(CreateInvoiceWOSendingRequest request);
    
    // Remove the processExistingConsulta method declaration
    // CreateInvoiceWOSendingResponse processExistingConsulta(Consulta consulta); // <-- DELETE THIS LINE
    
}