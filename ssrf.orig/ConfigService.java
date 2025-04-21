// ConfigService.java
package com.hackathon.ssrf;

import java.util.Properties;

/**
 * Simulates loading application configuration.
 * In a real app, this might read from a file or environment variables.
 */
public class ConfigService {

    private Properties config = new Properties();

    public ConfigService() {
        // Default configuration - intended for internal use
        config.setProperty("resource.base.url", "http://internal-service.local:9000/api/v1/data/");
        config.setProperty("resource.fetch.timeout.ms", "5000");
        System.out.println("ConfigService Initialized. Base URL: " + config.getProperty("resource.base.url"));
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
}