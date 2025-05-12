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

    @GetMapping("/viewDocument")
    public String viewDocument(@RequestParam(name = "docId", required = false) String requestedDocId,
                               Model model,
                               HttpSession session, // Spring injects the HTTP session
                               RedirectAttributes redirectAttributes) {

        // --- Authentication Check (similar to servlet) ---
        String currentUserId = (String) session.getAttribute("userId");

        if (currentUserId == null) {
            // In a real Spring Security app, this would be handled by security filters.
            // For direct conversion, we redirect.
            redirectAttributes.addFlashAttribute("error", "User not authenticated. Please login.");
            return "redirect:/login"; // Assuming a /login GET mapping exists or a login page
        }
        model.addAttribute("currentUserId", currentUserId);
        // --- End Authentication Check ---

        model.addAttribute("requestedDocId", requestedDocId);

        if (requestedDocId == null || requestedDocId.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Error: Please provide a document ID (e.g., /viewDocument?docId=DOC001).");
        } else {
            DocumentRecord document = documentService.getDocumentById(requestedDocId.trim());
            if (document != null) {
                // The authorization check (can currentUserId view document.getOwnerUserId())
                // is currently missing, similar to the original servlet's displayDocument method.
                // This would be a critical addition in a real application.
                model.addAttribute("document", document);
            } else {
                model.addAttribute("notFoundMessage", "Document not found for ID: " + requestedDocId);
            }
        }

        // Add test links data for the view
        model.addAttribute("allDocumentIds", documentService.getAllDocuments().keySet());

        return "view-document"; // This refers to src/main/resources/templates/view-document.html
    }

    // Example login endpoint to set the session attribute for testing
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Assumes a login.html exists or redirects to an identity provider
    }

    @GetMapping("/perform-login") // Simulate login
    public String performLogin(@RequestParam("userId") String userId, HttpSession session) {
        session.setAttribute("userId", userId);
        return "redirect:/viewDocument?docId=DOC001"; // Redirect to a document after "login"
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        // Redirect to a default document or a dashboard
        return "redirect:/viewDocument?docId=DOC001";
    }
}
