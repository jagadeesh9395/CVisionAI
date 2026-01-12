// src/main/java/com/jag/aires/extractor/EducationExtractor.java
package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Education;
import com.jag.aires.service.ai.GroqPromptService;
import com.jag.aires.util.ExperiencePeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EducationExtractor implements ResumeSectionExtractor<List<Education>> {

    private final GroqPromptService groqPromptService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

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
                      "start_date": "string (format: MM/YYYY, required)",
                      "end_date": "string (format: MM/YYYY or 'Present', required)",
                      "is_current": "boolean",
                      "grade": "string",
                      "achievements": ["string"]
                    }
                  ]
                }
                """;

        try {
            String systemPrompt = "Extract education details from the given text. Return the response in the following JSON format:\n" + schema;
            String response = groqPromptService.generateResponse(systemPrompt, sectionText);
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode educationArray = rootNode.get("education");

            if (educationArray == null || !educationArray.isArray()) {
                return Collections.emptyList();
            }

            List<Education> educations = new ArrayList<>();
            for (JsonNode eduNode : educationArray) {
                try {
                    Education education = new Education();
                    education.setInstitution(eduNode.path("institution").asText());
                    education.setDegree(eduNode.path("degree").asText());
                    education.setLocation(eduNode.path("location").asText());
                    education.setGrade(eduNode.path("grade").asText());
                    education.setIsCurrent(eduNode.path("is_current").asBoolean(false));

                    // Set achievements
                    List<String> achievements = new ArrayList<>();
                    eduNode.path("achievements").forEach(ach -> achievements.add(ach.asText()));
                    education.setAchievements(achievements);

                    // Set period
                    ExperiencePeriod period = new ExperiencePeriod();
                    period.setStartDate(YearMonth.parse(eduNode.path("start_date").asText(), DATE_FORMATTER));
                    if (!"Present".equalsIgnoreCase(eduNode.path("end_date").asText())) {
                        period.setEndDate(YearMonth.parse(eduNode.path("end_date").asText(), DATE_FORMATTER));
                    }
                    education.setPeriod(period);

                    educations.add(education);
                } catch (Exception e) {
                    log.warn("Failed to parse education entry: " + eduNode, e);
                }
            }
            return educations;
        } catch (Exception e) {
            log.error("Error parsing education", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Extract education details including degree (e.g., MCA, B.Tech), institution, location, start_date (MM/YYYY), end_date (MM/YYYY or 'Present'), and other relevant information.";
    }
}