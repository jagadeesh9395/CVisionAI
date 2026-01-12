package com.jag.aires.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.dto.TextAnalysisRequest;
import com.jag.aires.service.ai.GroqPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@Controller
public class TextAnalysisController {

    @GetMapping("/text-analyzer")
    public String showAnalyzerPage() {
        return "text-analyzer";
    }

    @RestController
    @RequestMapping("/api")
    @RequiredArgsConstructor
    public static class TextAnalysisApiController {

        private final GroqPromptService groqPromptService;
        private final ObjectMapper objectMapper;

        @PostMapping("/analyze")
        public ResponseEntity<String> analyzeText(@RequestBody TextAnalysisRequest request) {
            try {
                // Parse the schema to validate it's valid JSON
                objectMapper.readTree(request.getSchema());

                // Use the Groq service to analyze the text
                String systemPrompt = "Analyze the following text and return the response in the specified JSON format:\n" +
                        request.getSchema();
                return ResponseEntity.ok(groqPromptService.generateResponse(systemPrompt, request.getText()));

            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Error analyzing text: " + e.getMessage(), e);
            }
        }
    }
}
