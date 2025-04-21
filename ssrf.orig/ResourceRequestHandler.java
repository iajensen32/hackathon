// ResourceRequestHandler.java
package com.hackathon.ssrf;

import java.net.MalformedURLException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
            String resourceId = params.get("resourceId"); // Parameter name doesn't scream "URL"

            if (resourceId == null || resourceId.trim().isEmpty()) {
                responseBody = "Error: Missing 'resourceId' query parameter.";
                statusCode = 400;
            } else {
                try {
                    System.out.println("Processing request for resourceId: " + resourceId);
                    // Delegate fetching to the other class
                    responseBody = resourceFetcher.retrieveResourceContent(resourceId);
                    System.out.println("Successfully retrieved content.");
                } catch (IllegalArgumentException | IOException e) {
                    System.err.println("Error handling request for resourceId '" + resourceId + "': " + e.getMessage());
                    responseBody = "Error fetching resource: " + e.getMessage();
                    // Distinguish between client error (bad input) and server error (cannot fetch)
                    if (e instanceof MalformedURLException || e instanceof IllegalArgumentException) {
                         statusCode = 400; // Bad Request
                    } else {
                         statusCode = 500; // Internal Server Error or downstream error
                    }
                } catch (Exception e) {
                     // Catch unexpected errors
                     System.err.println("Unexpected error handling request for resourceId '" + resourceId + "': " + e.getMessage());
                     responseBody = "An unexpected server error occurred.";
                     statusCode = 500;
                }
            }
        }

        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
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