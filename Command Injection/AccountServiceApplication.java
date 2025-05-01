package com.example.accountservice; // Updated package name

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Account Service.
 */
@SpringBootApplication
public class AccountServiceApplication { // Updated class name

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        System.out.println("\n--- Account Service Application Started ---");
        System.out.println("API Endpoint available at: POST /api/account-info");
        System.out.println("Expected JSON Body: {\"application\": \"...\", \"accountNumber\": \"...\", \"customerNumber\": \"...\"}");
        System.out.println("------------------------------------------\n");
    }

}

