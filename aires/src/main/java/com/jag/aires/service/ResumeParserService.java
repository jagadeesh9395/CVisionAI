package com.jag.aires.service;

import com.jag.aires.extractor.EducationExtractor;
import com.jag.aires.extractor.ExperienceExtractor;
import com.jag.aires.extractor.SkillsExtractor;
import com.jag.aires.extractor.SummaryExtractor;
import com.jag.aires.model.ResumeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final ResumeSectionSplitterService splitterService;
    private final SummaryExtractor summaryExtractor;
    private final EducationExtractor educationExtractor;
    private final ExperienceExtractor experienceExtractor;
    private final SkillsExtractor skillsExtractor;

    public ResumeDocument parseResume(String rawText) {
        Map<String, String> sections = splitterService.splitSections(rawText);
        ResumeDocument document = new ResumeDocument();

        if (sections.containsKey("SUMMARY")) {
            document.setProfessionalSummary(summaryExtractor.extract(sections.get("SUMMARY")));
        }

        if (sections.containsKey("EDUCATION")) {
            document.setEducation(educationExtractor.extract(sections.get("EDUCATION")));
        }

        if (sections.containsKey("EXPERIENCE")) {
            document.setWorkExperience(experienceExtractor.extract(sections.get("EXPERIENCE")));
        }

        if (sections.containsKey("SKILLS")) {
            document.setSkills(skillsExtractor.extract(sections.get("SKILLS")));
        }

        return document;
    }
}
