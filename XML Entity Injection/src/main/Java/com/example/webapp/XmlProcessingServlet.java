package com.example.webapp; // Updated package

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
 */
// Map to the new action path used in index.html
@WebServlet("/process") // Updated mapping
@MultipartConfig
public class XmlProcessingServlet extends HttpServlet { // Renamed class

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

            // --- XML Parsing Section ---
            out.println("<h2>Parsed Content:</h2>");
            try {
                // Create a DocumentBuilderFactory
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                // Default settings are used here for the parser configuration.

                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse the XML content from the string
                InputSource is = new InputSource(new StringReader(xmlContent));
                Document doc = db.parse(is);
                doc.getDocumentElement().normalize();

                // Attempt to extract some data
                // Example: Get content of the first element named 'item'
                NodeList itemNodes = doc.getElementsByTagName("item"); // Changed example tag
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
                // Log the full error server-side
                System.err.println("XML Parsing Error: " + e.getMessage()); // Log to console/server logs
                // e.printStackTrace(); // Optionally print stack trace to server logs
            } catch (Exception e) {
                 out.println("<p style='color:red;'>An unexpected error occurred during XML processing: " + escapeHtml(e.getMessage()) + "</p>");
                 System.err.println("Unexpected XML Processing Error: " + e.getMessage());
                 // e.printStackTrace();
            }
            // --- End XML Parsing Section ---

        } catch (Exception e) {
            out.println("<p style='color:red;'>An error occurred during file upload processing: " + escapeHtml(e.getMessage()) + "</p>");
            // Log the full error server-side
            System.err.println("File Upload Processing Error: " + e.getMessage());
            // e.printStackTrace();
        } finally {
            out.println("</body></html>");
            out.close();
        }
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
