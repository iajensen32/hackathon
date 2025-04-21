// InternalResourceFetcher.java
package com.hackathon.ssrf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Fetches data based on a resource identifier, supposedly internal.
 */
public class InternalResourceFetcher {

    private final ConfigService config;

    public InternalResourceFetcher(ConfigService config) {
        this.config = config;
    }

    /**
     * Fetches content identified by the user-provided identifier, relative to the configured base URL.
     *
     * @param userProvidedIdentifier The identifier provided by the user (e.g., "document_abc.json")
     * @return The fetched content as a String.
     * @throws IOException If fetching fails.
     */
    public String retrieveResourceContent(String userProvidedIdentifier) throws IOException {
        if (userProvidedIdentifier == null || userProvidedIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource identifier cannot be empty.");
        }

        // Basic input check - might look like sanitization but doesn't prevent the core issue.
        if (userProvidedIdentifier.contains("..")) {
             throw new MalformedURLException("Invalid characters in resource identifier.");
        }
        
        URL baseUrl = null;
        try {
            baseUrl = new URL(config.getResourceBaseUrl());
            System.out.println("Using base URL: " + baseUrl);
        } catch (MalformedURLException e) {
            System.err.println("Configuration error: Invalid base URL provided.");
            throw new IOException("Internal configuration error", e);
        }

        URL finalUrl;
        try {
            // *** THE VULNERABILITY IS HERE ***
            // The URL(URL context, String spec) constructor is used.
            // If 'userProvidedIdentifier' is an absolute URL (e.g., "http://evil.com"),
            // it *completely ignores* the 'baseUrl' context.
            // This allows the user to specify any URL they want the server to request.
            finalUrl = new URL(baseUrl, userProvidedIdentifier);

            // A less experienced dev might *think* this safely combines a base path and a relative identifier.
             System.out.println("Constructed final URL for fetching: " + finalUrl);

        } catch (MalformedURLException e) {
            System.err.println("Error constructing final URL from base '" + baseUrl + "' and identifier '" + userProvidedIdentifier + "'");
            throw new IOException("Invalid resource identifier provided: " + e.getMessage(), e);
        }


        // Prevent requests to unexpected protocols, slight mitigation but doesn't fix SSRF fully
        if (!finalUrl.getProtocol().equals("http") && !finalUrl.getProtocol().equals("https")) {
             throw new MalformedURLException("Protocol not allowed: " + finalUrl.getProtocol());
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) finalUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.getFetchTimeout());
            connection.setReadTimeout(config.getFetchTimeout());
            connection.setInstanceFollowRedirects(false); // Good practice, but doesn't prevent SSRF

            int responseCode = connection.getResponseCode();
            System.out.println("Request sent to " + finalUrl + ". Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            } else {
                // Read error stream for more details if needed
                 try (InputStream errorStream = connection.getErrorStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                     String errorDetails = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                      throw new IOException("Failed to fetch resource from " + finalUrl + ". Server responded with code: " + responseCode + ". Details: " + errorDetails );
                 } catch (Exception e) {
                     // Ignore if error stream cannot be read
                      throw new IOException("Failed to fetch resource from " + finalUrl + ". Server responded with code: " + responseCode);
                 }
            }
        } catch (IOException e) {
            System.err.println("Error fetching resource: " + e.getMessage());
            throw new IOException("Failed to fetch resource.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}