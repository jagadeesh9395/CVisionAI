package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Education;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EducationExtractor implements ResumeSectionExtractor<List<Education>> {

    private final OllamaPromptService ollamaPromptService;
    private final ObjectMapper objectMapper;

    @Override
    public List<Education> extract(String sectionText) {
        if (sectionText == null || sectionText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String schema = """
                {
                  "education": [
                    {
                      "degree": "string",
                      "institution": "string",
                      "location": "string",
                      "start_date": "string",
                      "end_date": "string",
                      "is_current": "boolean",
                      "grade": "string",
                      "achievements": ["string"]
                    }
                  ]
                }
                """;

        try {
            String response = ollamaPromptService.analyzeText(sectionText, schema);
            var node = objectMapper.readTree(response);
            List<Education> educations = objectMapper.convertValue(
                    node.get("education"),
                    new TypeReference<List<Education>>() {
                    }
            );

            // Set default values if needed
            if (educations != null) {
                for (Education edu : educations) {
                    if (edu.getAchievements() == null) {
                        edu.setAchievements(new ArrayList<>());
                    }
                }
                return educations;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error parsing education", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Extract education details including degree (e.g., MCA, B.Tech), institution, location, and year_of_completion.";
    }
}
