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
            System.out.println(" Vulnerable SSRF Application Started on port " + SERVER_PORT);
            System.out.println(" Access via: http://localhost:" + SERVER_PORT + "/fetch?resourceId=<identifier>");
            System.out.println(" Example (intended use): http://localhost:" + SERVER_PORT + "/fetch?resourceId=some_internal_doc.txt");
            System.out.println(" Example (exploit): http://localhost:" + SERVER_PORT + "/fetch?resourceId=http://example.com");
            System.out.println(" Example (exploit - cloud metadata): http://localhost:" + SERVER_PORT + "/fetch?resourceId=http://169.254.169.254/latest/meta-data/");
            System.out.println("----------------------------------------------------");


        } catch (IOException e) {
            System.err.println("Failed to start the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}