package com.ipagos.morganainvoices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan; 

@SpringBootApplication
@ComponentScan(basePackages = "com.ipagos.morganainvoices")
public class MorganainvoicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MorganainvoicesApplication.class, args);
    }
}