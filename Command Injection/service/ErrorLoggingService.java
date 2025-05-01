package com.example.accountservice.service; // Updated package name

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Service containing the error logging mechanism.
 */
@Service
public class ErrorLoggingService { 

    /**
     * Logs validation errors by executing a Windows command to append to a file.
     *
     * @param failedParameterInfo Information about the failed validation.
     */
    public void logValidationError(String failedParameterInfo) {
        // Attempt to sanitize input slightly.
        String somewhatSanitizedInfo = failedParameterInfo.replace("\"", "'").replace("\n", "").replace("\r", "");

        System.out.println("[SERVICE] Logging validation error. Info: " + somewhatSanitizedInfo);
        // Define the log file path (ensure this directory exists and is writable)
        String logFilePath = "C:\\logs\\validation_errors.log"; // Adjusted log file name

        try {
            // Construct the command to echo the error message and append it to the log file.
            String command = "cmd /c echo Validation Error: " + somewhatSanitizedInfo + " >> " + logFilePath;

            System.out.println("[SERVICE] Executing command: " + command);

            // Execute the command
            Process process = Runtime.getRuntime().exec(command);

            // Capture output/error streams (optional, useful for debugging)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) { output.append(line).append("\n"); }
                while ((line = errorReader.readLine()) != null) { output.append("ERROR: ").append(line).append("\n"); }
            }

            int exitCode = process.waitFor();
            System.out.println("[SERVICE] Command executed. Exit Code: " + exitCode);
            if (output.length() > 0) {
                System.out.println("[SERVICE] Command Output/Error:\n" + output);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[SERVICE] Error executing logging command: " + e.getMessage());
            // In a real application, log this error using a safe logging framework (e.g., Logback, Log4j2)
            // logger.error("Failed to execute validation error logging command", e);
        }
    }
}
