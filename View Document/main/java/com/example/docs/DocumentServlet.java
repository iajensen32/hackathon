package com.example.docs.servlet;

import com.example.docs.model.DocumentRecord;
import com.example.docs.service.DocumentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet to display document details based on ID provided in the URL parameter.
 * Assumes user is authenticated.
 */
@WebServlet("/viewDocument") // Maps requests to /viewDocument
public class DocumentServlet extends HttpServlet {

    private DocumentService documentService = new DocumentService(); // Instantiate the service

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // --- Assume Authentication Handled ---
        HttpSession session = request.getSession(false);
        String currentUserId = null;

        if (session != null && session.getAttribute("userId") != null) {
            currentUserId = (String) session.getAttribute("userId");
        } else {
            response.sendRedirect("/login");
            return;
        }
        // --- End Assumption ---

        // Get the document ID requested by the user from the URL parameter
        String requestedDocId = request.getParameter("docId");

        out.println("<!DOCTYPE html><html><head><title>Document Viewer</title></head><body>");
        out.println("<h1>Document Viewer</h1>");
        out.println("<p><i>Current User ID: " + escapeHtml(currentUserId) + "</i></p>");
        out.println("<p><i>Requested Document ID: " + escapeHtml(requestedDocId) + "</i></p>");
        out.println("<hr>");

        if (requestedDocId == null || requestedDocId.trim().isEmpty()) {
            out.println("<p style='color:red;'>Error: Please provide a document ID (e.g., /viewDocument?docId=DOC001).</p>");
        } else {
            // Call the new method to retrieve and display the document
            displayDocument(out, requestedDocId.trim(), currentUserId);
        }

        out.println("<hr>");
        out.println("<p>Test Links (assuming logged in as user1):</p>");
        out.println("<ul>");
        out.println("<li><a href='?docId=DOC001'>View DOC001 (Owned by user1)</a></li>");
        out.println("<li><a href='?docId=DOC002'>View DOC002 (Owned by user1)</a></li>");
        out.println("<li><a href='?docId=DOC004'>View DOC004 (Owned by user2)</a> <-- Try this</li>");
        out.println("<li><a href='?docId=DOC005'>View DOC005 (Owned by user2)</a> <-- Try this</li>");
        out.println("<li><a href='?docId=DOC999'>View DOC999 (Not Found)</a></li>");
        out.println("</ul>");

        out.println("</body></html>");
        out.close();
    }

    /**
     * Retrieves and displays the document record using the service.
     */
    private void displayDocument(PrintWriter out, String requestedDocId, String currentUserId) {
        DocumentRecord document = documentService.getDocumentById(requestedDocId);
        if (document != null) {
                // Display the document data
                out.println("<h2>Document: " + escapeHtml(document.getTitle()) + " (ID: " + escapeHtml(document.getDocumentId()) + ")</h2>");
                out.println("<p><b>Owner:</b> " + escapeHtml(document.getOwnerUserId()) + "</p>");
                out.println("<h3>Content:</h3>");
                out.println("<pre style='border: 1px solid #eee; padding: 10px; background-color: #f8f8f8;'>");
                out.println(escapeHtml(document.getContent()));
                out.println("</pre>");
            }
        } else {
            out.println("<p style='color:orange;'>Document not found for ID: " + escapeHtml(requestedDocId) + "</p>");
        }
    }

    /** Basic HTML escaping utility. */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
