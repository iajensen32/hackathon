package com.example.webapp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * Servlet to handle XML file uploads and process them.
 * Includes basic validation attempt before parsing.
 */
@WebServlet("/process")
@MultipartConfig
public class XmlProcessingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html><html><head><title>XML Processing Result</title></head><body>");
        out.println("<h1>XML Processing Result</h1>");

        try {
            // Get the file part from the request
            Part filePart = request.getPart("xmlfile");
            if (filePart == null || filePart.getSize() == 0) {
                out.println("<p style='color:red;'>Error: No file uploaded or file is empty.</p>");
                out.println("</body></html>");
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            out.println("<p>Processing file: " + escapeHtml(fileName) + "</p>");

            // Read the file content into a String
            String xmlContent;
            try (InputStream fileInputStream = filePart.getInputStream()) {
                xmlContent = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            // --- Perform Basic Validation ---
            if (!performBasicValidation(xmlContent)) { // Renamed method call
                out.println("<p style='color:red;'>Error: Basic XML validation failed. Please check the content format and size.</p>"); // Generic error message
                out.println("</body></html>");
                return;
            }
            out.println("<p>Basic validation passed.</p>");
            // --- End Basic Validation ---


            // --- XML Parsing Section ---
            out.println("<h2>Parsed Content:</h2>");
            try {
                // Create a DocumentBuilderFactory
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                // Default settings are used here for the parser configuration.

                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse the XML content from the string
                InputSource is = new InputSource(new StringReader(xmlContent));
                Document doc = db.parse(is); // Parsing happens here
                doc.getDocumentElement().normalize();

                // Attempt to extract some data
                // Example: Get content of the first element named 'item'
                NodeList itemNodes = doc.getElementsByTagName("item");
                if (itemNodes.getLength() > 0) {
                    String itemContent = itemNodes.item(0).getTextContent();
                    out.println("<p>Content of first '&lt;item&gt;' element:</p>");
                    // Escape output
                    out.println("<pre>" + escapeHtml(itemContent) + "</pre>");
                } else {
                     out.println("<p>No '&lt;item&gt;' element found in the XML.</p>");
                }
                 out.println("<p style='color:green;'>XML parsed successfully.</p>");

            } catch (ParserConfigurationException | SAXException e) {
                out.println("<p style='color:red;'>Error parsing XML: " + escapeHtml(e.getMessage()) + "</p>");
                System.err.println("XML Parsing Error: " + e.getMessage());
            } catch (Exception e) {
                 out.println("<p style='color:red;'>An unexpected error occurred during XML processing: " + escapeHtml(e.getMessage()) + "</p>");
                 System.err.println("Unexpected XML Processing Error: " + e.getMessage());
            }
            // --- End XML Parsing Section ---

        } catch (Exception e) {
            out.println("<p style='color:red;'>An error occurred during file upload processing: " + escapeHtml(e.getMessage()) + "</p>");
            System.err.println("File Upload Processing Error: " + e.getMessage());
        } finally {
            out.println("</body></html>");
            out.close();
        }
    }

    /**
     * Performs basic validation checks on the raw XML content string.
     *
     * @param xmlContent The raw XML content as a string.
     * @return true if basic checks pass, false otherwise.
     */
    private boolean performBasicValidation(String xmlContent) { // Renamed method
        System.out.println("[VALIDATOR] Performing basic validation...");

        // Check 1: Length limit
        final int MAX_LENGTH = 10000; // Example limit
        if (xmlContent.length() > MAX_LENGTH) {
            System.out.println("[VALIDATOR] Failed: Content length check."); // Generic log message
            return false;
        }

        // Check 2: Look for script tags (basic check)
        if (xmlContent.toLowerCase().contains("<script")) {
             System.out.println("[VALIDATOR] Failed: Content structure check."); // Generic log message
             return false;
        }

        // Other basic checks could go here.

        System.out.println("[VALIDATOR] Basic validation passed.");
        return true;
    }


    /**
     * Basic HTML escaping utility.
     * @param input The string to escape.
     * @return The HTML-escaped string.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        // Basic escaping for demonstration
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
