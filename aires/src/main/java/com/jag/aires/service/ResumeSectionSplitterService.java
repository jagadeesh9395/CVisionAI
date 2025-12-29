package com.jag.aires.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ResumeSectionSplitterService {

    private static final Map<String, Pattern> SECTION_PATTERNS = new HashMap<>();

    // Update the static block in ResumeSectionSplitterService.java
    static {
        SECTION_PATTERNS.put("SUMMARY", Pattern.compile("(?i)(summary|professional summary|objective|profile)"));
        SECTION_PATTERNS.put("EXPERIENCE",
                Pattern.compile("(?i)(experience|work experience|employment history|professional experience)"));
        SECTION_PATTERNS.put("EDUCATION",
                Pattern.compile("(?i)(education|academic background|educational qualification)"));
        SECTION_PATTERNS.put("SKILLS", Pattern.compile("(?i)(skills|technical skills|expertise|competencies)"));
        SECTION_PATTERNS.put("PROJECTS",
                Pattern.compile("(?i)(projects|personal projects|academic projects|project experience)"));
        SECTION_PATTERNS.put("ACHIEVEMENTS",
                Pattern.compile("(?i)(achievements|awards|honors|accomplishments)"));
        SECTION_PATTERNS.put("CERTIFICATIONS",
                Pattern.compile("(?i)(certifications|certificates|professional development|training)"));
    }

    public Map<String, String> splitSections(String rawText) {
        Map<String, String> sections = new HashMap<>();
        String[] lines = rawText.split("\\r?\\n");
        String currentSection = "PERSONAL_INFO";
        StringBuilder sectionContent = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty())
                continue;

            boolean foundHeader = false;
            for (Map.Entry<String, Pattern> entry : SECTION_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(trimmedLine).matches()
                        || (trimmedLine.length() < 30 && entry.getValue().matcher(trimmedLine).find())) {
                    // Save previous section
                    sections.put(currentSection, sectionContent.toString().trim());
                    // Start new section
                    currentSection = entry.getKey();
                    sectionContent = new StringBuilder();
                    foundHeader = true;
                    break;
                }
            }

            if (!foundHeader) {
                sectionContent.append(line).append("\n");
            }
        }

        // Save the last section
        sections.put(currentSection, sectionContent.toString().trim());

        return sections;
    }
}
