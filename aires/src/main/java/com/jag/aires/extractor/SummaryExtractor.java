package com.jag.aires.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Summary;
import com.jag.aires.service.ai.GroqPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryExtractor implements ResumeSectionExtractor<Summary> {

  private final GroqPromptService groqPromptService;
  private final ObjectMapper objectMapper;

  @Override
  public Summary extract(String sectionText) {
    log.debug("Extracting summary from text of length: {}", sectionText.length());
    log.info("Extracting summary from text : {}", sectionText);

    String schema = """
        {
          "professional_summary": {
            "bulletPoints": [
              "string"
            ],
            "version": "string"
          }
        }
        """;

    try {
      log.trace("Sending text to AI for summary extraction");
      String systemPrompt = "Extract a professional summary from the given text. Return the response in the following JSON format:\n" + schema;
      String response = groqPromptService.generateResponse(systemPrompt, sectionText);
      log.debug("Raw AI response: {}", response);

      var node = objectMapper.readTree(response).get("professional_summary");
      if (node == null) {
        throw new IllegalStateException("Invalid response format from AI service");
      }

      Summary summary = new Summary();
      summary.setBulletPoints(objectMapper.convertValue(
          node.get("bulletPoints"),
          new TypeReference<List<String>>() {
          }));
      summary.setVersion(node.get("version").asText("v1"));

      log.info("Successfully extracted summary with {} bullet points",
          summary.getBulletPoints().size());
      return summary;

    } catch (Exception e) {
      log.error("Error parsing summary from AI response: {}", e.getMessage(), e);
      // Fallback to a basic summary if AI extraction fails
      Summary fallback = new Summary();
      fallback.setBulletPoints(List.of(sectionText));
      fallback.setVersion("fallback");
      return fallback;
    }
  }

  @Override
  public String getPrompt(String sectionText) {
    return """
        Extract a professional summary with the following structure:
        1. Create 3-5 concise bullet points highlighting:
           - Years of experience
           - Core technical skills
           - Major achievements
           - Industry expertise
        2. Each bullet should be 1-2 sentences max
        3. Focus on measurable achievements and specific technologies

        Input text to analyze:
        """ + sectionText;
  }
}