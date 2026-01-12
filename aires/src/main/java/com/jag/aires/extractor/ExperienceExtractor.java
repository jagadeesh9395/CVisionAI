// src/main/java/com/jag/aires/extractor/ExperienceExtractor.java
package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.WorkExperience;
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
public class ExperienceExtractor implements ResumeSectionExtractor<List<WorkExperience>> {

  private final GroqPromptService groqPromptService;
  private final ObjectMapper objectMapper;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

  @Override
  public List<WorkExperience> extract(String sectionText) {
    if (sectionText == null || sectionText.trim().isEmpty()) {
      return new ArrayList<>();
    }
    String schema = """
        {
          "work_experience": [
            {
              "company": "string (required)",
              "role": "string (required)",
              "startDate": "string (format: MM/YYYY, required)",
              "endDate": "string (format: MM/YYYY or 'Present', required)",
              "location": "string (optional)",
              "description": ["string (optional, array of responsibilities/achievements]"
            }
          ]
        }
        """;
    try {
      String systemPrompt = "Extract work experience details from the given text. Return the response in the following JSON format:\n" + schema;
      String response = groqPromptService.generateResponse(systemPrompt, sectionText);
      var node = objectMapper.readTree(response);
      var experiences = objectMapper.convertValue(node.get("work_experience"),
          new TypeReference<List<WorkExperience>>() {
          });

      // Convert dates to ExperiencePeriod
      for (WorkExperience exp : experiences) {
        if (exp != null) {
          JsonNode expNode = node.get("work_experience").get(experiences.indexOf(exp));
          ExperiencePeriod period = new ExperiencePeriod();
          period.setStartDate(YearMonth.parse(expNode.get("startDate").asText(), DATE_FORMATTER));
          if (!"Present".equalsIgnoreCase(expNode.get("endDate").asText())) {
            period.setEndDate(YearMonth.parse(expNode.get("endDate").asText(), DATE_FORMATTER));
          }
          exp.setPeriod(period);
        }
      }
      return experiences;
    } catch (Exception e) {
      log.error("Error parsing experience", e);
      return Collections.emptyList();
    }
  }

  @Override
  public String getPrompt(String sectionText) {
    return "Extract work experience with company, location, role, start_date (MM/YYYY format), end_date (MM/YYYY or 'Present'), and brief description.";
  }
}