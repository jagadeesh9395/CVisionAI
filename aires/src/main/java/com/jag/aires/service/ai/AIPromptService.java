package com.jag.aires.service.ai;

public interface AIPromptService {
    String generateResponse(String systemPrompt, String userPrompt);
    
    default String generateResponse(String systemPrompt, String userPrompt, String context) {
        return generateResponse(systemPrompt, userPrompt + "\n\nContext:\n" + context);
    }
}
