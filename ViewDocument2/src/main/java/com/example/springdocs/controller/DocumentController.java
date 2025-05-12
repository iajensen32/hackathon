package com.example.springdocs.controller;

import com.example.springdocs.model.DocumentRecord;
import com.example.springdocs.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession; // For Spring session management

@Controller
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Handles the request to view a document.
     * It performs authentication and then delegates to prepareAndShowDocument.
     */
    @GetMapping("/viewDocument")
    public String viewDocument(@RequestParam(name = "docId", required = false) String requestedDocId,
                               Model model,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        // --- Authentication Check ---
        String currentUserId = (String) session.getAttribute("userId");
        if (currentUserId == null) {
            redirectAttributes.addFlashAttribute("error", "User not authenticated. Please login.");
            return "redirect:/login";
        }
        model.addAttribute("currentUserId", currentUserId);
        // --- End Authentication Check ---

        model.addAttribute("requestedDocId", requestedDocId);

        if (requestedDocId == null || requestedDocId.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Error: Please provide a document ID (e.g., /viewDocument?docId=DOC001).");
        } else {
            // Delegate to the method to fetch and prepare document data
            prepareAndShowDocument(requestedDocId.trim(), currentUserId, model);
        }

        // Add test links data for the view
        model.addAttribute("allDocumentIds", documentService.getAllDocuments().keySet());

        return "view-document"; // Refers to src/main/resources/templates/view-document.html
    }

    /**
     * Fetches a document and prepares it for viewing.
     * This method retrieves the document based on the provided ID.
     * @param requestedDocId The ID of the document to retrieve.
     * @param currentUserId The ID of the currently authenticated user.
     * @param model The Spring MVC model to add attributes to.
     */
    private void prepareAndShowDocument(String requestedDocId, String currentUserId, Model model) {
        DocumentRecord document = documentService.getDocumentById(requestedDocId);

        if (document != null) {
            model.addAttribute("document", document);
        } else {
            model.addAttribute("notFoundMessage", "Document not found for ID: " + requestedDocId);
        }
    }

    // Example login endpoint to set the session attribute for testing
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Assumes a login.html exists
    }

    @GetMapping("/perform-login") // Simulate login
    public String performLogin(@RequestParam("userId") String userId, HttpSession session) {
        session.setAttribute("userId", userId);
        return "redirect:/viewDocument?docId=DOC001"; // Redirect after "login"
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        return "redirect:/viewDocument?docId=DOC001";
    }
}
