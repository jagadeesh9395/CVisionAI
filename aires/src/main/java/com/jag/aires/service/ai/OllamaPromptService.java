package com.jag.aires.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OllamaPromptService implements AIPromptService {
    private final ChatClient chatClient;

    public OllamaPromptService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateResponse(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * Analyzes the input text and returns the result in the specified JSON format.
     *
     * @param inputText The text to analyze
     * @param jsonSchema The target JSON schema to structure the response
     * @return String containing the analysis result in the requested JSON format
     */
    public String analyzeText(String inputText, String jsonSchema) {
        String systemPrompt = """
                You are a helpful AI assistant that analyzes text and provides structured responses.
                Your task is to analyze the provided text and return the results in the exact JSON format specified.
                Only include the JSON response, with no additional text, explanations, or markdown formatting.
                """;

        String userPrompt = String.format("""
                Analyze the following text and provide the results in this exact JSON format:
                %s
                
                Text to analyze:
                %s
                
                Return only the JSON response with no additional text or formatting.
                """, jsonSchema, inputText);

        String response = generateResponse(systemPrompt, userPrompt);
        
        // Validate and format the JSON response
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process JSON response", e);
        }
    }
}
