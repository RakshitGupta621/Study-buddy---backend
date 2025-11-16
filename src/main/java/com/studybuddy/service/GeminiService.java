package com.studybuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.model:text-bison-001}")
    private String model;
    
    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }
    
  public String callGemini(String prompt) {
    try {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            )
        );

        String response = webClient.post()
            .uri("/v1beta/models/gemini-2.0-flash:generateContent")
            .header("Content-Type", "application/json")
            .header("X-goog-api-key", apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        logger.debug("Gemini API raw response: {}", response); // Add this line

        JsonNode jsonNode = objectMapper.readTree(response);
        if (jsonNode.get("candidates") == null) {
            logger.error("No candidates in Gemini response: {}", response);
            throw new RuntimeException("Invalid Gemini API response");
        }
        return jsonNode.get("candidates").get(0)
                .get("content").get("parts").get(0)
                .get("text").asText();

    } catch (Exception e) {
        logger.error("Error calling Gemini API: ", e);
        throw new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
    }
}
    public String generateSummary(String content) {
        String prompt = String.format(
            """
            Please provide a comprehensive summary of the following educational content.
            Focus on the main concepts, key points, and important details that a student should understand.
            Keep the summary clear, concise, and well-structured.
            
            Content:
            %s
            """, content
        );
        
        return callGemini(prompt);
    }
    
    public String generateFlashcards(String content) {
        String prompt = String.format(
            """
            Based on the following content, create 10 educational flashcards in JSON format.
            Each flashcard should have a "question" and an "answer".
            Focus on key concepts, definitions, and important facts.
            
            Return ONLY valid JSON in this exact format (no additional text or markdown):
            [
              {"question": "What is...", "answer": "It is..."},
              {"question": "Define...", "answer": "..."}
            ]
            
            Content:
            %s
            """, content
        );
        
        return callGemini(prompt);
    }
    
    public String answerQuestion(String content, String question) {
        String prompt = String.format(
            """
            Based on the following document content, please answer this question:
            
            Question: %s
            
            Document Content:
            %s
            
            Provide a clear and helpful answer based on the document.
            If the answer is not in the document, say so.
            """, question, content
        );
        
        return callGemini(prompt);
    }
}