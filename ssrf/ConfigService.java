// ConfigService.java
package com.hackathon.ssrf;

import java.util.Arrays; // <-- Import Arrays
import java.util.Collections; // <-- Import Collections
import java.util.List; // <-- Import List
import java.util.Properties;

/**
 * Simulates loading application configuration.
 * In a real app, this might read from a file or environment variables.
 * Contains source for flawed "allow-list" logic.
 */
public class ConfigService {

    private Properties config = new Properties();

    public ConfigService() {
        // Default configuration - intended for internal use
        config.setProperty("resource.base.url", "http://internal-service.local:9000/api/v1/data/");
        config.setProperty("resource.fetch.timeout.ms", "5000");
        // Add allowed host prefixes - part of the flawed validation setup
        config.setProperty("allowed.host.prefixes", "internal-service.local,api.partner.com"); // Example prefixes

        System.out.println("ConfigService Initialized. Base URL: " + getResourceBaseUrl());
        System.out.println("Allowed host prefixes: " + getAllowedHostPrefixes());
    }

    public String getResourceBaseUrl() {
        // Intended to return the base path for constructing internal resource URLs
        return config.getProperty("resource.base.url");
    }

    public int getFetchTimeout() {
        try {
            return Integer.parseInt(config.getProperty("resource.fetch.timeout.ms"));
        } catch (NumberFormatException e) {
            return 5000; // Default timeout
        }
    }

    // New method to get allowed host prefixes
    public List<String> getAllowedHostPrefixes() {
        String prefixes = config.getProperty("allowed.host.prefixes");
        if (prefixes == null || prefixes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Return as a list, trimming whitespace
        return Arrays.asList(prefixes.split(",\\s*")); // Split by comma and optional whitespace
    }
}