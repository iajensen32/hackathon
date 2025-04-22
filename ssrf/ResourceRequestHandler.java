// ResourceRequestHandler.java
package com.hackathon.ssrf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException; // Keep this import
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles incoming HTTP requests for resources.
 */
public class ResourceRequestHandler implements HttpHandler {

    private final InternalResourceFetcher resourceFetcher;

    public ResourceRequestHandler(ConfigService config) {
        this.resourceFetcher = new InternalResourceFetcher(config);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String responseBody;
        int statusCode = 200;

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responseBody = "Method Not Allowed";
            statusCode = 405;
        } else {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            // Change parameter name for slight obfuscation
            String dataRef = params.get("dataRef"); // Changed from resourceId

            if (dataRef == null || dataRef.trim().isEmpty()) {
                // Update error message
                responseBody = "Error: Missing 'dataRef' query parameter.";
                statusCode = 400;
            } else {
                try {
                    // Update logging message
                    System.out.println("Processing request for dataRef: " + dataRef);
                    // Delegate fetching to the other class
                    responseBody = resourceFetcher.retrieveResourceContent(dataRef);
                    System.out.println("Successfully retrieved content.");
                } catch (SecurityException e) { // <-- Add specific catch for SecurityException
                    System.err.println("POLICY VIOLATION for dataRef '" + dataRef + "': " + e.getMessage());
                    responseBody = "Access Denied: " + e.getMessage();
                    statusCode = 403; // Forbidden status for security policy violation
                } catch (IllegalArgumentException | MalformedURLException e) { // Group client errors
                    System.err.println("BAD REQUEST for dataRef '" + dataRef + "': " + e.getMessage());
                    responseBody = "Bad Request: " + e.getMessage();
                    statusCode = 400;
                } catch (IOException e) { // Handle IO errors (fetching, connection)
                     System.err.println("FAILED TO FETCH for dataRef '" + dataRef + "': " + e.getMessage());
                     responseBody = "Error Fetching Resource: " + e.getMessage();
                     statusCode = 502; // Bad Gateway might be appropriate
                } catch (Exception e) {
                     // Catch unexpected errors
                     System.err.println("Unexpected error handling request for dataRef '" + dataRef + "': " + e.getClass().getName() + " - " + e.getMessage());
                     responseBody = "An unexpected server error occurred.";
                     statusCode = 500;
                }
            }
        }

        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        // Use -1 for content length if 0 to indicate no body, per HttpExchange docs suggestion
        exchange.sendResponseHeaders(statusCode, responseBytes.length > 0 ? responseBytes.length : -1);
        if (responseBytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        System.out.println("Response sent with status code: " + statusCode);
    }

    /**
     * Basic query string parser.
     */
    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2); // Split into key and value
            if (entry.length > 0) {
                try {
                    String key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8.name());
                    String value = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8.name()) : "";
                    result.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // Should not happen with UTF-8
                    System.err.println("Error decoding query parameter: " + e.getMessage());
                }
            }
        }
        return result;
    }
}