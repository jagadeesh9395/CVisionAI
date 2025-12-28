package com.jag.aires.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.Skills;
import com.jag.aires.service.ai.OllamaPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
                "javaTechnologies": {
                  "items": ["string"],
                  "aliases": ["Java Technologies", "JavaTech", "Java_Technologies", "Java"]
                },
                "webTechnologies": {
                  "items": ["string"],
                  "aliases": ["Web Technologies", "WebTech", "Web_Technologies", "Web"]
                },
                "distributedTechnologies": {
                  "items": ["string"],
                  "aliases": ["Distributed Technologies", "DistributedTech", "Distributed_Technologies"]
                },
                "frameworks": {
                  "items": ["string"],
                  "aliases": ["Frameworks", "Framework", "Frameworks_Libraries"]
                },
                "databases": {
                  "items": ["string"],
                  "aliases": ["Databases", "Database", "DB", "Data_Storage"]
                },
                "applicationServers": {
                  "items": ["string"],
                  "aliases": ["Application Servers", "AppServers", "Application_Servers"]
                },
                "webServers": {
                  "items": ["string"],
                  "aliases": ["Web Servers", "WebServers", "Web_Servers"]
                },
                "tools": {
                  "items": ["string"],
                  "aliases": ["Tools", "Development Tools", "Development_Tools", "DevTools"]
                },
                "unitTesting": {
                  "items": ["string"],
                  "aliases": ["Unit Testing", "UnitTesting", "Unit_Testing", "Test Frameworks"]
                },
                "designPatterns": {
                  "items": ["string"],
                  "aliases": ["Design Patterns", "DesignPatterns", "Design_Patterns"]
                },
                "ide": {
                  "items": ["string"],
                  "aliases": ["IDE", "IDEs", "Development Environment", "Development_Environment"]
                }
              }
            }
            """;

    @Override
    public Skills extract(String sectionText) {
        log.info("Extracting skills from text: {}", sectionText);

        try {
            // Pre-process the text to escape special characters
            String processedText = sectionText
                    // Specific AWS services pattern
                    .replaceAll("AWS\\(([^)]+)\\)", "AWS \\\\$1")
                    // General character escaping
                    .replace("\\", "\\\\")  // escape backslashes first
                    .replace("\"", "\\\"")  // escape double quotes
                    .replace("'", "\\'")      // escape single quotes
                    .replace("\n", "\\n")   // escape newlines
                    .replace("\r", "\\r")   // escape carriage returns
                    .replace("\t", "\\t");  // escape tabs

            String response = ollamaPromptService.analyzeText(processedText, SKILLS_SCHEMA);
            log.debug("Raw AI response: {}", response);

            // Add additional validation for the response
            var node = objectMapper.readTree(response);
            if (!node.has("skills")) {
                log.warn("AI response missing 'skills' field. Response: {}", response);
                return createEmptySkills();
            }

            JsonNode skillsNode = node.get("skills");
            Skills skills = new Skills();

            // Map each category from the response to the Skills object
            mapCategory(skillsNode, "javaTechnologies", skills::setJavaTechnologies);
            mapCategory(skillsNode, "webTechnologies", skills::setWebTechnologies);
            mapCategory(skillsNode, "distributedTechnologies", skills::setDistributedTechnologies);
            mapCategory(skillsNode, "frameworks", skills::setFrameworks);
            mapCategory(skillsNode, "databases", skills::setDatabases);
            mapCategory(skillsNode, "applicationServers", skills::setApplicationServers);
            mapCategory(skillsNode, "webServers", skills::setWebServers);
            mapCategory(skillsNode, "tools", skills::setTools);
            mapCategory(skillsNode, "unitTesting", skills::setUnitTesting);
            mapCategory(skillsNode, "designPatterns", skills::setDesignPatterns);
            mapCategory(skillsNode, "ide", skills::setIde);

            if (skills.getAllSkills().isEmpty()) {
                log.warn("No skills were extracted from the response. Response: {}", response);
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

    /**
     * Helper method to map a category from the JSON response to a Skills setter
     */
    private void mapCategory(JsonNode skillsNode, String categoryName, java.util.function.Consumer<List<String>> setter) {
        if (skillsNode.has(categoryName)) {
            JsonNode categoryNode = skillsNode.get(categoryName);
            if (categoryNode.has("items")) {
                List<String> items = new ArrayList<>();
                categoryNode.get("items").forEach(item -> items.add(item.asText()));
                setter.accept(items);
            }
        }
    }


    private Skills createEmptySkills() {
        Skills skills = new Skills();
        skills.updateAllSkills();
        return skills;
    }

    @Override
    public String getPrompt(String sectionText) {
        return """
                You are a skill categorization assistant. Your task is to categorize the provided skills into the specified categories.
                
                IMPORTANT FORMATTING RULES:
                1. STRICT JSON FORMAT: Your response MUST be valid JSON that matches the provided schema exactly.
                
                2. CHARACTER ESCAPING:
                   - Parentheses: "AWS (S3, EC2)" → "AWS \\(S3, EC2\\)"
                   - Single quotes: "O'Reilly" → "O\\'Reilly"
                   - Double quotes: "Some \"quoted\" text" → "Some \\\"quoted\\\" text"
                   - Backslashes: "C:\\Program Files" → "C:\\\\Program Files"
                   - Newlines: "Line1\nLine2" → "Line1\\nLine2"
                
                3. NO TRAILING COMMAS: Ensure there are no trailing commas in arrays or objects.
                
                4. PROPER QUOTES: 
                   - Use double quotes for all property names and string values
                   - Single quotes are not valid in JSON
                
                5. NO COMMENTS: Do not include any comments in the JSON.
                
                6. CATEGORIZATION GUIDELINES:
                   - Be specific with technology versions when available (e.g., "Java 17" instead of just "Java")
                   - If a skill could fit multiple categories, choose the most specific one
                   - Preserve any version numbers or specifications
                
                CATEGORIES AND THEIR ACCEPTED ALIASES:
                - Java Technologies: JavaTech, Java_Technologies, Java
                - Web Technologies: WebTech, Web_Technologies, Web
                - Distributed Technologies: DistributedTech, Distributed_Technologies
                - Frameworks: Framework, Frameworks_Libraries
                - Databases: Database, DB, Data_Storage
                - Application Servers: AppServers, Application_Servers
                - Web Servers: WebServers, Web_Servers
                - Tools: Development Tools, Development_Tools, DevTools
                - Unit Testing: UnitTesting, Unit_Testing, Test Frameworks
                - Design Patterns: DesignPatterns, Design_Patterns
                - IDE: IDEs, Development Environment, Development_Environment
                
                EXAMPLE OF PROPERLY FORMATTED RESPONSE:
                {
                  "skills": {
                    "javaTechnologies": {
                      "items": ["Java 17", "Java EE 8", "Spring Boot 3.x"],
                      "aliases": ["Java Technologies", "Java"]
                    },
                    "frameworks": {
                          "items": ["Spring Framework", "Hibernate"],
                          "aliases": ["Frameworks"]
                    },
                    "distributedTechnologies": {
                      "items": ["AWS \\(S3, EC2\\)", "Kubernetes", "Docker"],
                      "aliases": ["Distributed Technologies"]
                    },
                    "databases": {
                      "items": ["PostgreSQL 14", "MongoDB 6.0"],
                      "aliases": ["Databases"]
                    }
                  }
                }
                
                Now, categorize the following skills into the appropriate categories:
                """ + sectionText;
    }
}
