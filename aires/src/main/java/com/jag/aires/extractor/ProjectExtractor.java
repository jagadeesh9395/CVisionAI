package com.jag.aires.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Project;
import com.jag.aires.service.ai.OllamaPromptService;
import com.jag.aires.util.ExperiencePeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectExtractor implements ResumeSectionExtractor<List<Project>> {
    private final OllamaPromptService ollamaPromptService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");


    @Override
    public List<Project> extract(String text) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "projects": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "projectId": { "type": "string" },
                          "title": { "type": "string" },
                          "client": { "type": "string" },
                          "duration": {
                            "type": "object",
                            "properties": {
                              "start": { "type": "string", "format": "date" },
                              "end": { "type": "string", "format": "date" }
                            },
                            "required": ["start", "end"]
                          },
                          "skillsUsed": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "server": { "type": "string" },
                          "tools": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "role": { "type": "string" },
                          "description": { "type": "string" },
                          "rolesAndResponsibilities": {
                            "type": "array",
                            "items": { "type": "string" }
                          },
                          "isCurrent": { "type": "boolean" },
                          "projectUrl": { "type": "string" },
                          "repositoryUrl": { "type": "string" },
                          "displayOrder": { "type": "integer" },
                          "createdAt": { "type": "string", "format": "date-time" },
                          "updatedAt": { "type": "string", "format": "date-time" }
                        },
                        "required": ["title", "duration"]
                      }
                    }
                  },
                  "required": ["projects"]
                }
                """;
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = createExtractionPrompt(text);
            String response = ollamaPromptService.analyzeText(prompt, schema);
            return parseAiResponse(response);
        } catch (Exception e) {
            log.error("Error extracting projects using AI: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return createExtractionPrompt(sectionText);
    }

    private String createExtractionPrompt(String text) {
        return String.format("""
                Extract project information from the following resume text. 
                For each project, identify and extract the following details:
                - Project Title (required)
                - Client (if mentioned)
                - Duration with start and end dates (required, in YYYY-MM-DD format)
                - Role (if mentioned)
                - Skills used (as an array of strings)
                - Server/Environment (if mentioned)
                - Tools/Technologies used (as an array of strings)
                - Project description
                - Roles and Responsibilities (as an array of strings)
                - Project URL (if available)
                - Repository URL (if available)
                - isCurrent (boolean, true if project is currently ongoing)
                
                Important notes:
                - Duration must be an object with 'start' and 'end' dates in YYYY-MM-DD format
                - For ongoing projects, set isCurrent to true and end date can be empty
                - skillsUsed, tools, and rolesAndResponsibilities should be arrays, not comma-separated strings
                - Generate a unique projectId for each project in the format 'P1', 'P2', etc.
                - For dates, use MM/YYYY format (e.g., '06/2023' for June 2023)
                - Set displayOrder based on the project order in the resume (1 for first project, etc.)
                - Include current timestamp for createdAt and updatedAt
                
                Resume text:
                %s
                
                Return a JSON object with a 'projects' array containing the extracted project objects.
                """, text);
    }

    private List<Project> parseAiResponse(String response) {
        List<Project> projects = new ArrayList<>();
        try {
            var rootNode = objectMapper.readTree(response).get("projects");
            if (rootNode == null) {
                throw new IllegalStateException("Invalid response format from AI service");
            }
            if (rootNode.isArray()) {
                for (JsonNode projectNode : rootNode) {
                    Project project = new Project();
                    project.setTitle(projectNode.path("title").asText());
                    project.setClient(projectNode.path("client").asText(null));
                    if (projectNode.has("duration")) {
                        JsonNode durationNode = projectNode.path("duration");
                        if (durationNode.isObject()) {
                            ExperiencePeriod period = new ExperiencePeriod();
                            if (durationNode.has("startDate")) {
                                period.setStartDate(YearMonth.parse(durationNode.get("startDate").asText(), DATE_FORMATTER));
                            }
                            if (durationNode.has("endDate")) {
                                String endDateStr = durationNode.get("endDate").asText();
                                if (!"Present".equalsIgnoreCase(endDateStr)) {
                                    period.setEndDate(YearMonth.parse(endDateStr, DATE_FORMATTER));
                                }
                            }
                            project.setDuration(period);
                        }
                    }
                    project.setRole(projectNode.path("role").asText(null));

                    if (projectNode.has("skills")) {
                        List<String> skills = new ArrayList<>();
                        projectNode.path("skills").forEach(skill -> skills.add(skill.asText()));
                        project.setSkillsUsed(skills);
                    }

                    if (projectNode.has("responsibilities")) {
                        List<String> responsibilities = new ArrayList<>();
                        projectNode.path("responsibilities").forEach(resp -> responsibilities.add(resp.asText()));
                        project.setRolesAndResponsibilities(responsibilities);
                    }

                    projects.add(project);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing AI response for projects: {}", e.getMessage(), e);
        }
        return projects;
    }
}
