package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryExtractor implements ResumeSectionExtractor<List<String>> {

    private final OllamaPromptService ollamaPromptService;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> extract(String sectionText) {
        String schema = """
                {
                  "professional_summary": [
                    "string",
                    "string",
                    "string"
                  ]
                }
                """;
        String response = ollamaPromptService.analyzeText(sectionText, schema);
        try {
            var node = objectMapper.readTree(response);
            return objectMapper.convertValue(node.get("professional_summary"), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.error("Error parsing summary", e);
            return List.of(sectionText);
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Extract the professional summary as a list of key bullet points highlighting years of experience, technical skills, and expertise.";
    }
}
