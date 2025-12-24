package com.jag.aires.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Skills;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillsExtractor implements ResumeSectionExtractor<Skills> {

    private final OllamaPromptService ollamaPromptService;
    private final ObjectMapper objectMapper;

    @Override
    public Skills extract(String sectionText) {
        String schema = """
                {
                  "skills": {
                    "javaTechnologies": ["string"],
                    "webTechnologies": ["string"],
                    "distributedTechnologies": ["string"],
                    "frameworks": ["string"],
                    "databases": ["string"],
                    "applicationServers": ["string"],
                    "webServers": ["string"],
                    "tools": ["string"],
                    "unitTesting": ["string"],
                    "designPatterns": ["string"],
                    "ide": ["string"]
                  }
                }
                """;
        String response = ollamaPromptService.analyzeText(sectionText, schema);
        try {
            var node = objectMapper.readTree(response);
            return objectMapper.convertValue(node.get("skills"), Skills.class);
        } catch (Exception e) {
            log.error("Error parsing skills", e);
            return new Skills();
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Categorize the following skills into Java Technologies, Web Technologies, Distributed Technologies, Frameworks, Databases, Application Servers, Web Servers, Tools, Unit Testing, Design Patterns, and IDE.";
    }
}
