package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Education;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    String schema = """
        {
          "education": [
            {
              "degree": "string",
              "institution": "string",
              "location": "string",
              "year_of_completion": "string"
            }
          ]
        }
        """;
    String response = ollamaPromptService.analyzeText(sectionText, schema);
    try {
      var node = objectMapper.readTree(response);
      return objectMapper.convertValue(node.get("education"), new TypeReference<List<Education>>() {
      });
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
