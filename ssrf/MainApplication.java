// MainApplication.java
package com.hackathon.ssrf;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main entry point for the vulnerable application.
 * Sets up a simple HTTP server.
 */
public class MainApplication {

    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        try {
            // Initialize services
            ConfigService configService = new ConfigService();
            ResourceRequestHandler requestHandler = new ResourceRequestHandler(configService);

            // Setup HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            server.createContext("/fetch", requestHandler); // Endpoint is "/fetch"
            server.setExecutor(Executors.newFixedThreadPool(10)); // Basic thread pool

            // Start the server
            server.start();
            System.out.println("----------------------------------------------------");
            System.out.println(" Tricky SSRF Hackathon App Started on port " + SERVER_PORT);
            System.out.println(" Oakland, California - " + java.time.ZonedDateTime.now()); // Optional: Add timestamp/location
            System.out.println(" Access via: http://localhost:" + SERVER_PORT + "/fetch?dataRef=<identifier>"); // Updated param name
            System.out.println(" Example (intended internal relative): http://localhost:" + SERVER_PORT + "/fetch?dataRef=some_internal_doc.txt");
            System.out.println(" Example (intended external allowed): http://localhost:" + SERVER_PORT + "/fetch?dataRef=http://api.partner.com/status"); // Assumes api.partner.com is in allowed prefixes
            System.out.println(" Example (exploit via flawed validation): http://localhost:" + SERVER_PORT + "/fetch?dataRef=http://internal-service.local.evil.com/admin"); // Bypass via startsWith
            System.out.println(" Example (exploit - metadata attempt): http://localhost:" + SERVER_PORT + "/fetch?dataRef=http://169.254.169.254.evil.com/latest/meta-data/"); // Bypass via startsWith if IP not blocked
            System.out.println(" Example (blocked external): http://localhost:" + SERVER_PORT + "/fetch?dataRef=http://example.com"); // Should be blocked by validation
            System.out.println("----------------------------------------------------");


        } catch (IOException e) {
            System.err.println("Failed to start the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}