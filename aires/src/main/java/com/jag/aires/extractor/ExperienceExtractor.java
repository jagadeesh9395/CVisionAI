package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.WorkExperience;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceExtractor implements ResumeSectionExtractor<List<WorkExperience>> {

  private final OllamaPromptService ollamaPromptService;
  private final ObjectMapper objectMapper;

  @Override
  public List<WorkExperience> extract(String sectionText) {
    String schema = """
        {
          "work_experience": [
            {
              "company": "string",
              "location": "string",
              "role": "string",
              "start_date": "string",
              "end_date": "string",
              "description": "string"
            }
          ]
        }
        """;
    String response = ollamaPromptService.analyzeText(sectionText, schema);
    try {
      var node = objectMapper.readTree(response);
      return objectMapper.convertValue(node.get("work_experience"), new TypeReference<List<WorkExperience>>() {
      });
    } catch (Exception e) {
      log.error("Error parsing experience", e);
      return Collections.emptyList();
    }
  }

  @Override
  public String getPrompt(String sectionText) {
    return "Extract work experience with company, location, role, start_date (YYYY-MM format), end_date, and brief description.";
  }
}
