// InternalResourceFetcher.java
package com.hackathon.ssrf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder; // <-- Import URLEncoder
import java.nio.charset.StandardCharsets;
import java.util.List; // <-- Import List
import java.util.stream.Collectors;

/**
 * Fetches data based on a resource identifier.
 * Contains flawed validation logic making it vulnerable to SSRF.
 */
public class InternalResourceFetcher {

    private final ConfigService config;
    private final List<String> allowedHostPrefixes; // Store allowed prefixes

    public InternalResourceFetcher(ConfigService config) {
        this.config = config;
        // Get allowed prefixes from config during initialization
        this.allowedHostPrefixes = config.getAllowedHostPrefixes();
    }

    /**
     * Fetches content identified by the user-provided identifier.
     * Constructs URL, performs flawed validation, then fetches if validation passes.
     *
     * @param userProvidedIdentifier The identifier (e.g., "document_abc.json" or "http://...")
     * @return The fetched content as a String.
     * @throws IOException If fetching fails.
     * @throws SecurityException If URL validation fails.
     */
    public String retrieveResourceContent(String userProvidedIdentifier) throws IOException, SecurityException {
        if (userProvidedIdentifier == null || userProvidedIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource identifier cannot be empty.");
        }

        // Construct the target URL string first
        String targetUrlString = buildTargetUrl(userProvidedIdentifier);
        System.out.println("Constructed potential target URL: " + targetUrlString);

        // *** Perform Flawed Validation Check ***
        if (!isHostApparentlyAllowed(targetUrlString, this.allowedHostPrefixes)) {
             System.err.println("Security check FAILED for URL: " + targetUrlString);
            throw new SecurityException("Policy violation: Requested host is not permitted.");
        }
        System.out.println("Security check PASSED for URL (potentially due to flaw): " + targetUrlString);

        // If validation passed, create the URL object from the string
        URL finalUrl;
        try {
            finalUrl = new URL(targetUrlString);
        } catch (MalformedURLException e) {
            // This might happen if the validation logic accidentally allowed an invalid URL format through
            System.err.println("Error creating URL object after validation passed: " + e.getMessage());
            throw new IOException("Internal error: Invalid URL format processed.", e);
        }

        // Protocol Check (Keep this as a basic sanity check)
        if (!finalUrl.getProtocol().equals("http") && !finalUrl.getProtocol().equals("https")) {
             throw new SecurityException("Protocol not allowed: " + finalUrl.getProtocol());
        }

        // *** Proceed with Fetching (Sink) ***
        // The code below is reached only if the flawed validation passed.
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) finalUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.getFetchTimeout());
            connection.setReadTimeout(config.getFetchTimeout());
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();
            System.out.println("Request sent to " + finalUrl + ". Response Code: " + responseCode);

            InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                                       ? connection.getInputStream()
                                       : connection.getErrorStream();
            
            if (inputStream == null) {
                 return "Response Code: " + responseCode + " (No data stream)";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                if (responseCode >= 200 && responseCode < 300) {
                    return content;
                } else {
                    throw new IOException("Server responded with code: " + responseCode + ". Details: " + content);
                }
            }
        } catch (IOException e) {
             System.err.println("Error during HTTP connection to " + finalUrl + ": " + e.getMessage());
             // Avoid leaking the full URL in the exception message to the client if possible
             throw new IOException("Failed to fetch resource due to connection error.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Constructs the target URL string based on identifier.
     * If identifier looks absolute, use it directly. Otherwise, combine with base URL.
     */
    private String buildTargetUrl(String identifier) {
         // Simple check if the identifier looks like an absolute URL.
        if (identifier.matches("^[a-zA-Z]+://.*")) {
            System.out.println("Identifier '" + identifier + "' treated as absolute URL.");
            return identifier;
        } else {
            // Assume relative, combine with base URL from config
             System.out.println("Identifier '" + identifier + "' treated as relative path.");
            String baseUrl = config.getResourceBaseUrl();
            String encodedIdentifier = URLEncoder.encode(identifier, StandardCharsets.UTF_8)
                                         .replace("+", "%20"); // Basic encoding

            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            if (encodedIdentifier.startsWith("/")) { // Avoid double slash if identifier starts with /
                 encodedIdentifier = encodedIdentifier.substring(1);
            }
            return baseUrl + encodedIdentifier;
        }
    }


    /**
     * Performs a FLAWED check if the URL's host starts with any allowed prefixes.
     * Static helper method containing the vulnerability logic.
     *
     * @param urlString The URL string to check.
     * @param allowedHostPrefixes List of allowed prefixes.
     * @return true if validation passes (potentially due to flaw), false otherwise.
     */
    private static boolean isHostApparentlyAllowed(String urlString, List<String> allowedHostPrefixes) {
        if (urlString == null || allowedHostPrefixes == null || allowedHostPrefixes.isEmpty()) {
            return false; // Nothing to check against, deny.
        }

        try {
            URL url = new URL(urlString);
            String host = url.getHost();

            if (host == null || host.trim().isEmpty()) {
                 System.out.println("Validation: URL has no host: " + urlString);
                return false; // Cannot validate host if none exists.
            }
            host = host.toLowerCase(); // Case-insensitive check

            System.out.println("Validation: Checking host '" + host + "' against prefixes: " + allowedHostPrefixes);

            // *** THE FLAW *** Checks if host *starts with* prefix.
            for (String prefix : allowedHostPrefixes) {
                if (host.startsWith(prefix.trim().toLowerCase())) {
                    System.out.println("Validation: Host '" + host + "' matches prefix '" + prefix + "'. ALLOWING.");
                    return true; // Vulnerable point
                }
            }

            System.out.println("Validation: Host '" + host + "' did not match allowed prefixes. DENYING.");
            return false;

        } catch (MalformedURLException e) {
             System.out.println("Validation: Invalid URL format: " + urlString);
            return false; // Cannot validate invalid URLs.
        }
    }
}