package com.jag.aires.service.ai;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroqPromptService implements AIPromptService {
    private final ChatClient chatClient;

    public GroqPromptService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String generateResponse(String systemPrompt, String userPrompt) {
        SystemMessage systemMessage = new SystemMessage(systemPrompt + "\n\nIMPORTANT: Return ONLY valid JSON. Do not include any markdown code blocks or additional text.");
        UserMessage userMessage = new UserMessage(userPrompt);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        String response = chatClient.call(prompt).getResult().getOutput().getContent();
        
        // Clean up the response by removing markdown code blocks if present
        return cleanJsonResponse(response);
    }
    
    /**
     * Cleans up the JSON response by removing markdown code blocks and any non-JSON content.
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        // Remove markdown code blocks
        String cleaned = response.replaceAll("```(json)?\\s*([\\s\\S]*?)\\s*```", "$2");
        
        // Remove any non-JSON content before the first {
        int startBrace = cleaned.indexOf('{');
        if (startBrace > 0) {
            cleaned = cleaned.substring(startBrace);
        }
        
        // Remove any non-JSON content after the last }
        int endBrace = cleaned.lastIndexOf('}');
        if (endBrace >= 0 && endBrace < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, endBrace + 1);
        }
        
        return cleaned.trim();
    }
}