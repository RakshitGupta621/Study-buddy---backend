package com.studybuddy.controller;

import com.studybuddy.model.Document;
import com.studybuddy.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "${cors.allowed.origins}")
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Document document = documentService.uploadDocument(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", document.getId());
            response.put("filename", document.getFilename());
            response.put("uploadedAt", document.getUploadedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload document: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/summary")
    public ResponseEntity<Map<String, String>> generateSummary(@PathVariable Long id) {
        try {
            String summary = documentService.generateSummary(id);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate summary: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/flashcards")
    public ResponseEntity<Map<String, Object>> generateFlashcards(@PathVariable Long id) {
        try {
            String flashcardsJson = documentService.generateFlashcards(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("flashcards", flashcardsJson);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate flashcards: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/chat")
    public ResponseEntity<Map<String, String>> chatWithDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            String answer = documentService.chatWithDocument(id, question);
            
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to answer question: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete document: " + e.getMessage()));
        }
    }
}