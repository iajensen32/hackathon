package com.example.accountservice.exception; // Updated package name

import com.example.accountservice.service.ErrorLoggingService; // Updated import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler to catch validation errors and trigger error logging.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorLoggingService errorLoggingService; // Updated service name

    // Inject the error logging service
    @Autowired
    public GlobalExceptionHandler(ErrorLoggingService errorLoggingService) { // Updated service name
        this.errorLoggingService = errorLoggingService;
    }

    /**
     * Handles validation exceptions (MethodArgumentNotValidException).
     * Extracts error details, triggers the error logging mechanism,
     * and returns a user-friendly error response.
     *
     * @param ex The exception caught.
     * @return ResponseEntity containing validation error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        System.out.println("[HANDLER] Validation failed. Triggering error logging...");

        Map<String, String> errors = new HashMap<>();
        String firstInvalidValue = "N/A"; // Default value if no field errors found
        String firstFieldName = "N/A";

        // Extract field errors
        if (ex.getBindingResult().hasFieldErrors()) {
             FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
             firstFieldName = firstError.getField();
             // Get the actual invalid value submitted by the user for the first error field
             Object rejectedValue = firstError.getRejectedValue();
             firstInvalidValue = (rejectedValue != null) ? rejectedValue.toString() : "null";

             // Populate error map for response body
             ex.getBindingResult().getFieldErrors().forEach(error -> {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
             });
        } else if (ex.getBindingResult().hasGlobalErrors()) {
            // Handle global errors if necessary
            firstFieldName = "global";
            firstInvalidValue = ex.getBindingResult().getGlobalError().toString(); // Less specific
             ex.getBindingResult().getGlobalErrors().forEach(error -> errors.put("globalError", error.getDefaultMessage()));
        }


        // --- Trigger Error Logging ---
        // Pass the actual invalid value submitted for the first field to the logging method.
        errorLoggingService.logValidationError("Field: " + firstFieldName + ", Value: " + firstInvalidValue);
        // -----------------------------

        // Prepare response body
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "Validation Error");
        responseBody.put("errors", errors);
        responseBody.put("message", "Input validation failed. Please check the errors.");

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic exceptions as a fallback.
     *
     * @param ex The exception caught.
     * @return ResponseEntity indicating an internal server error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericExceptions(Exception ex) {
        System.err.println("[HANDLER] An unexpected error occurred: " + ex.getMessage());
        // Log the stack trace in a real application using a proper logger
        // logger.error("Unexpected error occurred", ex);

        Map<String, String> responseBody = Map.of(
            "status", "Error",
            "message", "An internal server error occurred."
        );
        return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
