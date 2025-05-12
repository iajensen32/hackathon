package com.example.springdocs.service;

import com.example.springdocs.model.DocumentRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentService {

    // In-memory storage for documents (replace with a database in a real app)
    private final Map<String, DocumentRecord> documentStore = new HashMap<>();

    public DocumentService() {
        // Initialize with some sample data
        documentStore.put("DOC001", new DocumentRecord("DOC001", "User 1 - Report Q1", "user1", "This is the Q1 financial report for User 1. It contains sensitive data."));
        documentStore.put("DOC002", new DocumentRecord("DOC002", "User 1 - Project Alpha Plan", "user1", "Detailed plans for Project Alpha, including timelines and resources for User 1."));
        documentStore.put("DOC003", new DocumentRecord("DOC003", "Public Announcement Draft", "public", "This is a draft for a public announcement. Content is not sensitive."));
        documentStore.put("DOC004", new DocumentRecord("DOC004", "User 2 - Research Notes", "user2", "Confidential research notes for User 2."));
        documentStore.put("DOC005", new DocumentRecord("DOC005", "User 2 - Meeting Minutes", "user2", "Minutes from the last confidential meeting held by User 2."));
    }

    /**
     * Retrieves a document by its ID.
     * In a real application, this would fetch from a database.
     *
     * @param documentId The ID of the document to retrieve.
     * @return The DocumentRecord if found, otherwise null.
     */
    public DocumentRecord getDocumentById(String documentId) {
        if (documentId == null) {
            return null;
        }
        return documentStore.get(documentId.trim());
    }

    /**
     * (Helper for testing/demo) Retrieves all documents.
     * @return A map of all documents.
     */
    public Map<String, DocumentRecord> getAllDocuments() {
        return new HashMap<>(documentStore); // Return a copy
    }
}
