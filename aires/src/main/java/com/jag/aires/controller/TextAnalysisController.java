package com.jag.aires.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.dto.TextAnalysisRequest;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TextAnalysisController {

    private final OllamaPromptService ollamaPromptService;
    private final ObjectMapper objectMapper;

    @GetMapping("/text-analyzer")
    public String showAnalyzerPage() {
        return "text-analyzer";
    }

    @RestController
    @RequestMapping("/api")
    @RequiredArgsConstructor
    public static class TextAnalysisApiController {
        
        private final OllamaPromptService ollamaPromptService;
        private final ObjectMapper objectMapper;

        @PostMapping("/analyze")
        public String analyzeText(@RequestBody TextAnalysisRequest request) {
            try {
                // Parse the schema to validate it's valid JSON
                objectMapper.readTree(request.getSchema());
                
                // Use the Ollama service to analyze the text
                return ollamaPromptService.analyzeText(request.getText(), request.getSchema());
                
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Error analyzing text: " + e.getMessage(), e);
            }
        }
    }
}
