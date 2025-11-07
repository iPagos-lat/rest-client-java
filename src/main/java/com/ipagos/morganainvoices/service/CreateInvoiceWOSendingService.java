package com.ipagos.morganainvoices.service;

import com.cybersource.authsdk.core.ConfigException;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingRequest;
import com.ipagos.morganainvoices.api.beans.CreateInvoiceWOSending.CreateInvoiceWOSendingResponse;
import Invokers.ApiException; 

public interface CreateInvoiceWOSendingService {

    CreateInvoiceWOSendingResponse createInvoice(CreateInvoiceWOSendingRequest request) throws ApiException, ConfigException;

}