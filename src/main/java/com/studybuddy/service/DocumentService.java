package com.studybuddy.service;

import com.studybuddy.model.Document;
import com.studybuddy.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final GeminiService geminiService;

    public DocumentService(DocumentRepository documentRepository, GeminiService geminiService) {
        this.documentRepository = documentRepository;
        this.geminiService = geminiService;
    }

    public Document uploadDocument(MultipartFile file) throws IOException {
        logger.info("Uploading document: {}", file.getOriginalFilename());

        String content = extractContent(file);
        logger.info("Extracted content length: {} characters", content.length());

        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setContent(content);

        Document saved = documentRepository.save(document);
        logger.info("Document saved with ID: {}", saved.getId());
        return saved;
    }

    private String extractContent(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if ("application/pdf".equals(contentType)) {
            return extractPdfContent(file);
        } else if ("text/plain".equals(contentType)) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private String extractPdfContent(MultipartFile file) throws IOException {
    byte[] bytes = file.getBytes();
    try (PDDocument document = PDDocument.load(new java.io.ByteArrayInputStream(bytes))) {
        if (document.isEncrypted()) {
            try {
                document.setAllSecurityToBeRemoved(true);
            } catch (Exception ignored) { }
        }
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        return text != null ? text.trim() : "";
    } catch (IOException e) {
        logger.error("Failed to extract PDF content: {}", e.getMessage(), e);
        throw e;
    }
}

    public String generateSummary(Long documentId) {
        try {
            logger.info("Generating summary for document: {}", documentId);

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

            if (document.getSummary() != null && !document.getSummary().isEmpty()) {
                logger.info("Returning cached summary");
                return document.getSummary();
            }

            logger.info("Calling Gemini API for summary...");
            String summary = geminiService.generateSummary(document.getContent());
            logger.info("Summary generated successfully, length: {}", summary.length());

            document.setSummary(summary);
            documentRepository.save(document);
            logger.info("Summary saved to database");

            return summary;
        } catch (Exception e) {
            logger.error("Error generating summary for document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate summary: " + e.getMessage(), e);
        }
    }

    public String generateFlashcards(Long documentId) {
        try {
            logger.info("Generating flashcards for document: {}", documentId);

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

            if (document.getFlashcards() != null && !document.getFlashcards().isEmpty()) {
                logger.info("Returning cached flashcards");
                return document.getFlashcards();
            }

            logger.info("Calling Gemini API for flashcards...");
            String flashcards = geminiService.generateFlashcards(document.getContent());
            logger.info("Flashcards generated successfully, length: {}", flashcards.length());

            // Clean up the response if it contains markdown code blocks
            flashcards = flashcards.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            logger.info("Flashcards after cleanup: {}", flashcards.substring(0, Math.min(100, flashcards.length())));

            document.setFlashcards(flashcards);
            documentRepository.save(document);
            logger.info("Flashcards saved to database");

            return flashcards;
        } catch (Exception e) {
            logger.error("Error generating flashcards for document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate flashcards: " + e.getMessage(), e);
        }
    }

    public String chatWithDocument(Long documentId, String question) {
        try {
            logger.info("Chat question for document {}: {}", documentId, question);

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

            String answer = geminiService.answerQuestion(document.getContent(), question);
            logger.info("Answer generated successfully");
            return answer;
        } catch (Exception e) {
            logger.error("Error answering question for document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to answer question: " + e.getMessage(), e);
        }
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public void deleteDocument(Long documentId) {
        documentRepository.deleteById(documentId);
    }
}