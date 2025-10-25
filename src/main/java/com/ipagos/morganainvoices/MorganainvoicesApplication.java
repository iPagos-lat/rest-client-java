package com.ipagos.morganainvoices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Import the classes to exclude
import org.springframework.boot.autoconfigure.EnableAutoConfiguration; 
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan; 

// Add the @EnableAutoConfiguration annotation here
@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class}) 
@ComponentScan(basePackages = "com.ipagos.morganainvoices")
public class MorganainvoicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MorganainvoicesApplication.class, args);
    }

}