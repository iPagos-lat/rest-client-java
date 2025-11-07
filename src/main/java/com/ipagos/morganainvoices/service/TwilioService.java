package com.ipagos.morganainvoices.service;

import java.util.Map;

public interface TwilioService {
    
    void sendTemplateNotification(String toPhoneNumber, String contentSid, Map<String, String> variables);
}