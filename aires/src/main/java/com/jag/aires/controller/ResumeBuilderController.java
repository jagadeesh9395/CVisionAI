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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jag.aires.exception.ResourceNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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
            // Clear any existing session data
            session.removeAttribute("rawSections");
            session.removeAttribute("resumeData");
            session.removeAttribute("processingComplete");
            session.removeAttribute("processingError");
            
            // Store the file in session for processing
            byte[] fileBytes = file.getBytes();
            session.setAttribute("uploadedFile", fileBytes);
            session.setAttribute("originalFilename", file.getOriginalFilename());
            
            // Start processing in a background thread
            new Thread(() -> {
                try {
                    // Process the file
                    String rawText = new Tika().parseToString(new ByteArrayInputStream(fileBytes));
                    Map<String, String> sections = splitterService.splitSections(rawText);
                    
                    if (sections == null || sections.isEmpty()) {
                        session.setAttribute("processingError", "Failed to extract content from the resume");
                        session.setAttribute("processingComplete", true);
                        return;
                    }
                    
                    // Create new resume document
                    ResumeDocument resume = new ResumeDocument();
                    
                    // Extract personal info
                    if (sections.containsKey("PERSONAL_INFO")) {
                        PersonalInfo extractedInfo = personalInfoExtractor.extract(sections.get("PERSONAL_INFO"));
                        extractedInfo = personalInfoRepository.save(extractedInfo);
                        resume.setPersonalInfo(extractedInfo);
                    }
                    
                    // Save the resume with extracted data
                    resume.updateTimestamps();
                    resume = resumeRepository.save(resume);
                    
                    // Store in session
                    session.setAttribute("resumeData", resume);
                    session.setAttribute("rawSections", sections);
                    
                } catch (Exception e) {
                    log.error("Error processing uploaded file", e);
                    session.setAttribute("processingError", "An error occurred while processing your resume");
                } finally {
                    // Mark processing as complete
                    session.setAttribute("processingComplete", true);
                    
                    // Clean up
                    session.removeAttribute("uploadedFile");
                    session.removeAttribute("originalFilename");
                }
            }).start();
            
            // Redirect to processing page
            return "redirect:/builder/processing";
        } catch (Exception e) {
            log.error("Error processing uploaded file", e);
            return "redirect:/builder/upload-error";
        }
    }
    
    @GetMapping("/processing")
    public String showProcessingPage(HttpSession session) {
        // Check if there's a file being processed or if processing is complete
        if (session.getAttribute("uploadedFile") == null && 
            session.getAttribute("processingComplete") == null) {
            return "redirect:/";
        }
        return "processing";
    }
    
    @GetMapping("/check-status")
    @ResponseBody
    public Map<String, Object> checkProcessingStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        boolean isComplete = session.getAttribute("processingComplete") != null;
        response.put("complete", isComplete);
        
        if (isComplete) {
            String error = (String) session.getAttribute("processingError");
            if (error != null) {
                response.put("error", error);
            }
        }
        
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
        
        // If we don't have a resume in session, redirect to upload
        if (resume == null || resume.getId() == null) {
            return "redirect:/";
        }

        // Get the personal info from the resume
        PersonalInfo personalInfo = resume.getPersonalInfo();
        if (personalInfo == null) {
            personalInfo = new PersonalInfo();
            resume.setPersonalInfo(personalInfo);
        }
            
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
    public String showExperience(HttpSession session, Model model) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }
            return "builder-experience";
        } catch (Exception e) {
            log.error("Error loading experience page", e);
            return "redirect:/builder/error";
        }
    }
    @PostMapping("/experience/process")
    public String processExperience(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }

            Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
            if (sections == null || !sections.containsKey("EXPERIENCE")) {
                redirectAttributes.addFlashAttribute("error", "No experience data found to process");
                return "redirect:/builder/experience";
            }

            String experienceText = sections.get("EXPERIENCE");
            if (experienceText == null || experienceText.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Experience section is empty");
                return "redirect:/builder/experience";
            }

            // Extract and save experiences
            List<WorkExperience> experiences = experienceExtractor.extract(experienceText);
            if (experiences != null && !experiences.isEmpty()) {
                List<WorkExperience> savedExperiences = new ArrayList<>();
                for (WorkExperience exp : experiences) {
                    if (exp != null) {
                        // Ensure description is never null
                        if (exp.getDescription() == null) {
                            exp.setDescription(new ArrayList<>());
                        }
                        exp.setResumeId(resume.getId());
                        exp = workExperienceRepository.save(exp);
                        savedExperiences.add(exp);
                    }
                }
                // Update resume with saved experiences
                resume.setWorkExperience(savedExperiences);
                resume.updateTimestamps();
                resumeRepository.save(resume);
            }

            return "redirect:/builder/experience/list";
        } catch (Exception e) {
            log.error("Error processing experience", e);
            redirectAttributes.addFlashAttribute("error", "Error processing experience: " + e.getMessage());
            return "redirect:/builder/experience";
        }
    }

    @GetMapping("/experience/add")
    public String showAddExperienceForm(Model model) {
        model.addAttribute("workExperience", new WorkExperience());
        return "builder-experience-form";
    }

    @PostMapping("/experience/save")
    public String saveExperience(
            @Valid @ModelAttribute("workExperience") WorkExperience workExperience,
            BindingResult result,
            HttpSession session,
            @RequestParam(required = false) String action) {

        if (result.hasErrors()) {
            return "builder-experience-form";
        }

        ResumeDocument resume = getOrCreateResume(session);
        if (resume == null || resume.getId() == null) {
            return "redirect:/";
        }

        try {
            workExperience.setResumeId(resume.getId());
            workExperienceRepository.save(workExperience);

            if ("saveAndAddAnother".equals(action)) {
                return "redirect:/builder/experience/add";
            }
            return "redirect:/builder/experience";

        } catch (DataAccessException e) {
            log.error("Database error saving work experience", e);
            result.reject("error.database", "Error saving to database. Please try again.");
            return "builder-experience-form";
        } catch (Exception e) {
            log.error("Error saving work experience", e);
            result.reject("error.experience", "An unexpected error occurred. Please try again.");
            return "builder-experience-form";
        }
    }

    @GetMapping("/experience/edit/{id}")
    public String showEditExperienceForm(@PathVariable String id, Model model, HttpSession session) {
        ResumeDocument resume = getOrCreateResume(session);
        if (resume == null || resume.getId() == null) {
            return "redirect:/";
        }

        WorkExperience experience = workExperienceRepository.findByIdAndResumeId(id, resume.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found with id: " + id));

        model.addAttribute("workExperience", experience);
        return "builder-experience-form";
    }

    @PostMapping("/experience/delete/{id}")
    public String deleteExperience(@PathVariable String id, HttpSession session) {
        ResumeDocument resume = getOrCreateResume(session);
        if (resume == null || resume.getId() == null) {
            return "redirect:/";
        }

        try {
            workExperienceRepository.deleteByIdAndResumeId(id, resume.getId());
            return "redirect:/builder/experience?success=deleted";
        } catch (Exception e) {
            log.error("Error deleting work experience", e);
            return "redirect:/builder/experience?error=delete_failed";
        }
    }

    @GetMapping("/experience/suggestions/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExperienceSuggestions(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        ResumeDocument resume = getOrCreateResume(session);

        if (resume == null || resume.getId() == null) {
            response.put("error", "Session expired or invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            WorkExperience experience = workExperienceRepository.findByIdAndResumeId(id, resume.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Experience not found with id: " + id));

            List<String> suggestions = new ArrayList<>();

            if (experience.getRole() != null) {
                suggestions.add("Consider using action verbs in your role description");
                suggestions.add("Add specific achievements and metrics if available");
            }

            if (experience.getStartDate() != null && experience.getEndDate() == null) {
                suggestions.add("Add an end date or mark as 'Present' if currently working here");
            }

            if (experience.getDescription() == null || experience.getDescription().isEmpty()) {
                suggestions.add("Add bullet points describing your responsibilities and achievements");
            } else {
                suggestions.add("Consider quantifying your achievements with numbers and metrics");
            }

            response.put("suggestions", suggestions);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Experience not found", e);
            response.put("error", "Experience not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            response.put("error", "Error generating suggestions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/experience/list")
    public String showExperienceList(HttpSession session, Model model) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }

            List<WorkExperience> experiences = workExperienceRepository.findByResumeId(resume.getId());
            if (experiences == null) {
                experiences = new ArrayList<>();
            }

            model.addAttribute("experiences", experiences);
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
