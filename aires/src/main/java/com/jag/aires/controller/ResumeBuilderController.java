package com.jag.aires.controller;

import com.jag.aires.extractor.*;
import com.jag.aires.model.PersonalInfo;
import com.jag.aires.model.ResumeDocument;
import com.jag.aires.model.WorkExperience;
import com.jag.aires.repository.PersonalInfoRepository;
import com.jag.aires.repository.ResumeRepository;
import com.jag.aires.repository.WorkExperienceRepository;
import com.jag.aires.service.ResumeSectionSplitterService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/builder")
@RequiredArgsConstructor
@Slf4j
public class ResumeBuilderController {

    private final ResumeSectionSplitterService splitterService;
    private final PersonalInfoExtractor personalInfoExtractor;
    private final SummaryExtractor summaryExtractor;
    private final EducationExtractor educationExtractor;
    private final ExperienceExtractor experienceExtractor;
    private final SkillsExtractor skillsExtractor;
    private final ResumeRepository resumeRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final WorkExperienceRepository workExperienceRepository;
    
    private ResumeDocument getOrCreateResume(HttpSession session) {
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");
        if (resume == null) {
            resume = new ResumeDocument();
            // If you have user authentication, set the userId here
            // resume.setUserId(currentUser.getId());
            session.setAttribute("resumeData", resume);
        }
        return resume;
    }
    private final Tika tika = new Tika();

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            String rawText = tika.parseToString(file.getInputStream());
            Map<String, String> sections = splitterService.splitSections(rawText);
            
            if (sections == null || sections.isEmpty()) {
                return "redirect:/builder/upload-error";
            }
            
            session.setAttribute("rawSections", sections);
            session.setAttribute("resumeData", new ResumeDocument());
            
            // Start processing in a separate thread
            new Thread(() -> {
                try {
                    // Simulate processing time
                    Thread.sleep(5000);
                    // Set a flag when processing is complete
                    session.setAttribute("processingComplete", true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Processing was interrupted", e);
                }
            }).start();
            
            return "redirect:/builder/processing";
        } catch (Exception e) {
            log.error("Error processing uploaded file", e);
            return "redirect:/builder/upload-error";
        }
    }
    
    @GetMapping("/processing")
    public String showProcessingPage(HttpSession session) {
        if (session.getAttribute("rawSections") == null) {
            return "redirect:/";
        }
        return "processing";
    }
    
    @GetMapping("/check-status")
    @ResponseBody
    public Map<String, Object> checkProcessingStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("complete", session.getAttribute("processingComplete") != null);
        return response;
    }
    
    @GetMapping("/upload-error")
    public String showUploadError() {
        return "upload-error";
    }

    @GetMapping("/basics")
    @SuppressWarnings("unchecked")
    public String showBasicsForm(HttpSession session, Model model) {
        ResumeDocument resume = getOrCreateResume(session);
        Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");

        // If personal info is not set and we have raw sections, try to extract it
        if (resume.getPersonalInfo() == null && sections != null && sections.containsKey("PERSONAL_INFO")) {
            // First save the PersonalInfo to get an ID
            PersonalInfo extractedInfo = personalInfoExtractor.extract(sections.get("PERSONAL_INFO"));
            extractedInfo = personalInfoRepository.save(extractedInfo);
            
            // Then set it to the resume and save
            resume.setPersonalInfo(extractedInfo);
            resume.updateTimestamps();
            resume = resumeRepository.save(resume);
            session.setAttribute("resumeData", resume);
        }

        // Always ensure we have a non-null PersonalInfo in the model
        PersonalInfo personalInfo = resume.getPersonalInfo() != null ? 
            resume.getPersonalInfo() : new PersonalInfo();
            
        model.addAttribute("personalInfo", personalInfo);
        return "builder-basics";
    }

    @PostMapping("/basics")
    public String saveBasics(
            @Valid @ModelAttribute("personalInfo") PersonalInfo personalInfo,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "builder-basics";
        }

        try {
            ResumeDocument resume = getOrCreateResume(session);
            
            // Handle the PersonalInfo save/update
            PersonalInfo savedPersonalInfo;
            if (resume.getPersonalInfo() != null && resume.getPersonalInfo().getId() != null) {
                // Update existing PersonalInfo
                personalInfo.setId(resume.getPersonalInfo().getId());
                savedPersonalInfo = personalInfoRepository.save(personalInfo);
            } else {
                // Create new PersonalInfo
                savedPersonalInfo = personalInfoRepository.save(personalInfo);
            }
            
            // Set the saved PersonalInfo to the ResumeDocument
            resume.setPersonalInfo(savedPersonalInfo);
            resume.updateTimestamps();
            
            // Save the ResumeDocument
            resume = resumeRepository.save(resume);
            
            // Update session with the latest data
            session.setAttribute("resumeData", resume);
            
            redirectAttributes.addFlashAttribute("success", "Personal information saved successfully!");
            return "redirect:/builder/experience";
            
        } catch (Exception e) {
            log.error("Error saving personal info", e);
            redirectAttributes.addFlashAttribute(
                "error", 
                "Failed to save personal information: " + 
                (e.getMessage() != null ? e.getMessage() : "Unknown error")
            );
            return "redirect:/builder/basics";
        }
    }

    @GetMapping("/greet")
    @SuppressWarnings("unchecked")
    public String showGreet(HttpSession session, Model model) {
        try {
            // Safely get or create resume
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                log.error("Failed to get or create resume");
                return "redirect:/builder/upload-error";
            }

            // Safely get sections from session
            Map<String, String> sections = new HashMap<>();
            try {
                sections = (Map<String, String>) session.getAttribute("rawSections");
                if (sections == null) {
                    sections = new HashMap<>();
                }
            } catch (Exception e) {
                log.warn("Error getting rawSections from session", e);
            }

            // Handle personal info extraction
            try {
                if (resume.getPersonalInfo() == null && sections.containsKey("PERSONAL_INFO")) {
                    PersonalInfo personalInfo = personalInfoExtractor.extract(sections.get("PERSONAL_INFO"));
                    if (personalInfo != null) {
                        personalInfo = personalInfoRepository.save(personalInfo);
                        resume.setPersonalInfo(personalInfo);
                        resume.updateTimestamps();
                        resume = resumeRepository.save(resume);
                        session.setAttribute("resumeData", resume);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing personal info", e);
            }

            // Handle work experience extraction
            try {
                if ((resume.getWorkExperience() == null || resume.getWorkExperience().isEmpty()) 
                        && sections.containsKey("EXPERIENCE")) {
                    List<WorkExperience> experiences = experienceExtractor.extract(sections.get("EXPERIENCE"));
                    if (experiences != null && !experiences.isEmpty()) {
                        // Save each work experience first
                        List<WorkExperience> savedExperiences = new ArrayList<>();
                        for (WorkExperience exp : experiences) {
                            if (exp != null) {
                                exp = workExperienceRepository.save(exp);
                                savedExperiences.add(exp);
                            }
                        }
                        // Then set the saved experiences to the resume
                        resume.setWorkExperience(savedExperiences);
                        resume.updateTimestamps();
                        resume = resumeRepository.save(resume);
                        session.setAttribute("resumeData", resume);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing work experience", e);
            }

            // Set up model attributes
            String fullName = "";
            if (resume.getPersonalInfo() != null) {
                fullName = resume.getPersonalInfo().getFullName();
            }
            model.addAttribute("fullName", fullName);

            // Get latest company details
            Map<String, String> latestCompany = new HashMap<>();
            try {
                if (resume.getWorkExperience() != null && !resume.getWorkExperience().isEmpty()) {
                    var experience = resume.getWorkExperience().get(0);
                    if (experience != null) {
                        latestCompany.put("name", StringUtils.defaultIfEmpty(experience.getCompany(), ""));
                        latestCompany.put("location", StringUtils.defaultIfEmpty(experience.getLocation(), ""));
                        latestCompany.put("role", StringUtils.defaultIfEmpty(experience.getRole(), ""));

                        // Format duration
                        StringBuilder duration = new StringBuilder();
                        if (experience.getStartDate() != null) {
                            duration.append(experience.getStartDate());
                            if (experience.getEndDate() != null) {
                                duration.append(" – ").append(experience.getEndDate());
                            } else {
                                duration.append(" – Present");
                            }
                        }
                        latestCompany.put("duration", duration.toString());
                    }
                }
            } catch (Exception e) {
                log.error("Error formatting work experience", e);
            }
            model.addAttribute("latestCompany", latestCompany);

            return "greet";

        } catch (Exception e) {
            log.error("Unexpected error in showGreet", e);
            return "redirect:/builder/upload-error";
        }
    }

    // Removed duplicate saveBasics method - consolidated with the one that has validation and error handling

    @GetMapping("/experience")
    public String showExperience() {
        return "builder-experience";
    }

    @GetMapping("/experience-list")
    @SuppressWarnings("unchecked")
    public String showExperienceList(HttpSession session, Model model) {
        try {
            Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
            ResumeDocument resume = getOrCreateResume(session);

            if (resume != null && (resume.getWorkExperience() == null || resume.getWorkExperience().isEmpty()) 
                    && sections != null && sections.containsKey("EXPERIENCE")) {
                List<WorkExperience> experiences = experienceExtractor.extract(sections.get("EXPERIENCE"));
                if (experiences != null && !experiences.isEmpty()) {
                    // Save each work experience first
                    List<WorkExperience> savedExperiences = new ArrayList<>();
                    for (WorkExperience exp : experiences) {
                        if (exp != null) {
                            exp = workExperienceRepository.save(exp);
                            savedExperiences.add(exp);
                        }
                    }
                    // Then set the saved experiences to the resume
                    resume.setWorkExperience(savedExperiences);
                    resume.updateTimestamps();
                    resume = resumeRepository.save(resume);
                    session.setAttribute("resumeData", resume);
                }
            }

            model.addAttribute("experiences", resume != null ? resume.getWorkExperience() : null);
            return "builder-experience-list";
        } catch (Exception e) {
            log.error("Error loading experience list", e);
            return "redirect:/builder/error";
        }
    }

    @GetMapping("/education")
    public String showEducation() {
        return "builder-education";
    }

    @GetMapping("/education-list")
    @SuppressWarnings("unchecked")
    public String showEducationList(HttpSession session, Model model) {
        Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");

        if (resume != null && resume.getEducation() == null && sections != null && sections.containsKey("EDUCATION")) {
            resume.setEducation(educationExtractor.extract(sections.get("EDUCATION")));
        }

        model.addAttribute("educations", resume != null ? resume.getEducation() : null);
        return "builder-education-list";
    }

    @GetMapping("/skills")
    public String showSkills() {
        return "builder-skills";
    }

    @GetMapping("/skills-list")
    @SuppressWarnings("unchecked")
    public String showSkillsList(HttpSession session, Model model) {
        Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");

        if (resume != null && resume.getSkills() == null && sections != null && sections.containsKey("SKILLS")) {
            resume.setSkills(skillsExtractor.extract(sections.get("SKILLS")));
        }

        model.addAttribute("skills", resume != null ? resume.getSkills() : null);
        return "builder-skills-list";
    }

    @GetMapping("/summary")
    public String showSummary() {
        return "builder-summary";
    }

    @GetMapping("/summary-edit")
    @SuppressWarnings("unchecked")
    public String showSummaryEdit(HttpSession session, Model model) {
        Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");

        if (resume != null && resume.getProfessionalSummary() == null && sections != null
                && sections.containsKey("SUMMARY")) {
            resume.setProfessionalSummary(summaryExtractor.extract(sections.get("SUMMARY")));
        }

        model.addAttribute("summary", resume != null ? resume.getProfessionalSummary() : null);
        return "builder-summary-edit";
    }

    @PostMapping("/finalize")
    public String finalizeResume(HttpSession session) {
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");
        if (resume != null) {
            resumeRepository.save(resume);
        }
        return "redirect:/builder/finished";
    }

    @GetMapping("/finished")
    public String finished() {
        return "builder-finalize";
    }
}
