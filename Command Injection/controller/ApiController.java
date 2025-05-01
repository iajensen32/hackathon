package com.example.accountservice.controller; // Updated package name

import com.example.accountservice.dto.AccountInfoRequest; // Updated import
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for handling account information requests.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * Processes account information requests.
     * Performs validation on the incoming request body.
     * If validation passes, returns a success message.
     * If validation fails, the GlobalExceptionHandler will intercept the exception.
     *
     * @param request The validated account information request.
     * @return ResponseEntity indicating success.
     */
    @PostMapping("/account-info")
    public ResponseEntity<Map<String, String>> processAccountInfo(@Valid @RequestBody AccountInfoRequest request) {
        // If @Valid passes, execution reaches here.
        System.out.println("[CONTROLLER] Received valid request for application: " + request.getApplication());

        // Simulate successful processing
        Map<String, String> response = Map.of(
            "status", "Success",
            "message", "Account information received for application: " + request.getApplication()
        );
        return ResponseEntity.ok(response);
    }
}
