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

    // In SkillsExtractor.java

    private static final String SKILLS_SCHEMA = """
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

    @Override
    public Skills extract(String sectionText) {
        log.info("Extracting skills from text: {}", sectionText);

        try {
            String response = ollamaPromptService.analyzeText(sectionText, SKILLS_SCHEMA);
            log.debug("Raw AI response: {}", response);

            var node = objectMapper.readTree(response);
            if (!node.has("skills")) {
                log.warn("AI response missing 'skills' field. Response: {}", response);
                return createEmptySkills();
            }

            Skills skills = objectMapper.convertValue(node.get("skills"), Skills.class);
            if (skills == null) {
                log.warn("Failed to convert AI response to Skills object. Response: {}", response);
                return createEmptySkills();
            }

            // Ensure allSkills is populated
            skills.updateAllSkills();
            log.info("Successfully extracted {} skills", skills.getAllSkills().size());
            return skills;

        } catch (Exception e) {
            log.error("Error extracting skills. Section text: {}. Error: {}",
                    sectionText, e.getMessage(), e);
            return createEmptySkills();
        }
    }

    private Skills createEmptySkills() {
        Skills skills = new Skills();
        skills.updateAllSkills();
        return skills;
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Categorize the following skills into Java Technologies, Web Technologies, Distributed Technologies, Frameworks, Databases, Application Servers, Web Servers, Tools, Unit Testing, Design Patterns, and IDE.";
    }
}
