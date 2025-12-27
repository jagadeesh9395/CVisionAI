package com.jag.aires.controller;

import com.jag.aires.exception.ResourceNotFoundException;
import com.jag.aires.extractor.*;
import com.jag.aires.model.*;
import com.jag.aires.repository.*;
import com.jag.aires.service.ResumeSectionSplitterService;
import com.jag.aires.util.ExperiencePeriod;
import com.mongodb.DuplicateKeyException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private final EducationRepository educationRepository;
    private final SkillsRepository skillsRepository;

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
                    session.setAttribute("resumeId", resume.getId());  // Add this line

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

        // Add processing status
        response.put("processing", session.getAttribute("uploadedFile") != null);

        if (isComplete) {
            String error = (String) session.getAttribute("processingError");
            if (error != null) {
                response.put("error", error);
            } else {
                // Add success status
                response.put("resumeId", session.getAttribute("resumeId"));
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
                            (e.getMessage() != null ? e.getMessage() : "Unknown error"));
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
                        if (experience.getPeriod() != null && experience.getPeriod().getStartDate() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
                            duration.append(experience.getPeriod().getStartDate().format(formatter));

                            if (experience.getPeriod().getEndDate() != null) {
                                duration.append(" – ").append(experience.getPeriod().getEndDate().format(formatter));
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

    // Removed duplicate saveBasics method - consolidated with the one that has
    // validation and error handling

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
            HttpSession session) {

        if (result.hasErrors()) {
            return "builder-experience-form";
        }

        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }

            // Ensure resume is saved and has an ID
            if (resume.getId() == null) {
                resume.updateTimestamps();
                resume = resumeRepository.save(resume);
                session.setAttribute("resumeData", resume);
            }

            // Handle empty strings from form submission
            if (workExperience.getId() != null && workExperience.getId().trim().isEmpty()) {
                workExperience.setId(null);
            }
            if (workExperience.getResumeId() != null && workExperience.getResumeId().trim().isEmpty()) {
                workExperience.setResumeId(null); // Will be set below
            }

            // Ensure period is properly set
            if (workExperience.getPeriod() == null) {
                workExperience.setPeriod(new ExperiencePeriod());
            }

            // Set resume ID
            if (workExperience.getResumeId() == null) {
                workExperience.setResumeId(resume.getId());
            }

            // Save the work experience
            workExperience = workExperienceRepository.save(workExperience);

            // Update the resume's work experiences
            if (resume.getWorkExperience() == null) {
                resume.setWorkExperience(new ArrayList<>());
            }
            if (!resume.getWorkExperience().contains(workExperience)) {
                resume.getWorkExperience().add(workExperience);
            }
            resumeRepository.save(resume);

            return "redirect:/builder/experience/list?success=saved";

        } catch (Exception e) {
            log.error("Error saving work experience", e);
            return "redirect:/builder/experience?error";
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
    public String deleteExperience(
            @PathVariable String id,
            @RequestParam(required = false) String action,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        ResumeDocument resume = getOrCreateResume(session);
        if (resume == null || resume.getId() == null) {
            return "redirect:/";
        }

        try {
            workExperienceRepository.deleteByIdAndResumeId(id, resume.getId());
            redirectAttributes.addFlashAttribute("success", "Work experience deleted successfully!");

            // Handle redirection based on the action
            if (action != null && action.equals("deleteAndAddAnother")) {
                return "redirect:/builder/experience/add";
            }
            return "redirect:/builder/experience/list";
        } catch (Exception e) {
            log.error("Error deleting work experience", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting work experience: " + e.getMessage());
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

            if (experience.getPeriod() != null &&
                    experience.getPeriod().getStartDate() != null &&
                    experience.getPeriod().getEndDate() == null) {
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

            // Ensure all experiences have a valid period
            for (WorkExperience exp : experiences) {
                if (exp.getPeriod() == null) {
                    exp.setPeriod(new ExperiencePeriod());
                }
            }

            model.addAttribute("experiences", experiences);
            return "builder-experience-list";
        } catch (Exception e) {
            log.error("Error loading experience list", e);
            return "redirect:/builder/error";
        }
    }

    @PostMapping("/extract-education")
    @ResponseBody
    public ResponseEntity<?> extractEducation(HttpSession session) {
        try {
            Map<String, String> rawSections = (Map<String, String>) session.getAttribute("rawSections");
            if (rawSections != null && rawSections.containsKey("EDUCATION")) {
                String educationText = rawSections.get("EDUCATION");
                List<Education> extractedEducation = educationExtractor.extract(educationText);

                // Save the extracted education to the database
                ResumeDocument resume = getOrCreateResume(session);
                for (Education edu : extractedEducation) {
                    edu.setResumeId(resume.getId());
                    educationRepository.save(edu);
                }

                return ResponseEntity.ok().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error extracting education", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/education-list")
    public String showEducationList(HttpSession session, Model model) {
        ResumeDocument resume = getOrCreateResume(session);
        List<Education> educations = educationRepository.findByResumeId(resume.getId());

        // If no educations found in DB but we have raw data, try to extract
        if ((educations == null || educations.isEmpty()) && session.getAttribute("rawSections") != null) {
            Map<String, String> rawSections = (Map<String, String>) session.getAttribute("rawSections");
            if (rawSections.containsKey("EDUCATION")) {
                String educationText = rawSections.get("EDUCATION");
                educations = educationExtractor.extract(educationText);

                // Save the extracted education
                if (educations != null && !educations.isEmpty()) {
                    for (Education edu : educations) {
                        edu.setResumeId(resume.getId());
                        educationRepository.save(edu);
                    }
                }
            }
        }

        model.addAttribute("educations", educations != null ? educations : Collections.emptyList());
        return "builder-education-list";
    }

    @GetMapping("/education")
    public String showEducation() {
        return "builder-education";
    }

    // @GetMapping("/education-list")
    // public String showEducationList(HttpSession session, Model model) {
    // try {
    // ResumeDocument resume = getOrCreateResume(session);
    // if (resume == null) {
    // return "redirect:/";
    // }
    //
    // List<Education> educations =
    // educationRepository.findByResumeId(resume.getId());
    // if (educations == null) {
    // educations = new ArrayList<>();
    // }
    //
    // model.addAttribute("educations", educations);
    // return "builder-education-list";
    // } catch (Exception e) {
    // log.error("Error loading education list", e);
    // return "redirect:/builder/error";
    // }
    // }

    @GetMapping("/education/add")
    public String showAddEducationForm(Model model) {
        model.addAttribute("education", new Education());
        return "builder-education-form";
    }

    @PostMapping("/education/save")
    public String saveEducation(
            @Valid @ModelAttribute("education") Education education,
            BindingResult result,
            HttpSession session,
            @RequestParam(required = false) String action,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "builder-education-form";
        }

        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }

            if (resume.getId() == null) {
                resume.updateTimestamps();
                resume = resumeRepository.save(resume);
                session.setAttribute("resumeData", resume);
            }

            // Handle empty strings from form submission
            if (education.getId() != null && education.getId().trim().isEmpty()) {
                education.setId(null);
            }

            education.setResumeId(resume.getId());

            // Handle the isCurrent flag
            if (education.getIsCurrent() == null) {
                education.setIsCurrent(false);
            }
            if (education.getIsCurrent()) {
                if (education.getPeriod() == null) {
                    education.setPeriod(new ExperiencePeriod());
                }
                education.getPeriod().setEndDate(null);
            }

            // Save the education
            educationRepository.save(education);

            redirectAttributes.addFlashAttribute("success", "Education saved successfully!");

            if ("saveAndAdd".equals(action)) {
                return "redirect:/builder/education/add";
            }
            return "redirect:/builder/education-list";
        } catch (Exception e) {
            log.error("Error saving education", e);
            result.reject("error.education", "Error saving education: " + e.getMessage());
            return "builder-education-form";
        }
    }

    @GetMapping("/education/edit/{id}")
    public String showEditEducationForm(@PathVariable String id, Model model, HttpSession session) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null || resume.getId() == null) {
                return "redirect:/";
            }

            Education education = educationRepository.findByIdAndResumeId(id, resume.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Education not found with id: " + id));

            model.addAttribute("education", education);
            return "builder-education-form";
        } catch (ResourceNotFoundException e) {
            log.error("Education not found", e);
            return "redirect:/builder/education-list";
        } catch (Exception e) {
            log.error("Error loading education form", e);
            return "redirect:/builder/error";
        }
    }

    @PostMapping("/education/delete/{id}")
    public String deleteEducation(
            @PathVariable String id,
            @RequestParam(required = false) String action,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null || resume.getId() == null) {
                return "redirect:/";
            }

            educationRepository.deleteByIdAndResumeId(id, resume.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Education deleted successfully");

            if ("returnToList".equals(action)) {
                return "redirect:/builder/education-list";
            }
            return "redirect:/builder/education";
        } catch (Exception e) {
            log.error("Error deleting education", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting education: " + e.getMessage());
            return "redirect:/builder/education-list";
        }
    }

    @GetMapping("/education/suggestions/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEducationSuggestions(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        ResumeDocument resume = getOrCreateResume(session);

        if (resume == null || resume.getId() == null) {
            response.put("error", "Session expired or invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Education education = educationRepository.findByIdAndResumeId(id, resume.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Education not found with id: " + id));

            List<String> suggestions = new ArrayList<>();

            if (education.getDegree() != null) {
                suggestions.add("Consider adding your field of study if not already included");
                suggestions.add("Include your GPA if it's 3.0 or higher");
            }

            if (education.getPeriod() != null &&
                    education.getPeriod().getEndDate() == null &&
                    (education.getIsCurrent() == null || !education.getIsCurrent())) {
                suggestions.add("Mark as 'Currently Studying' if you're still studying here");
            }

            if (education.getAchievements() == null || education.getAchievements().isEmpty()) {
                suggestions.add("Add relevant coursework, honors, or achievements");
            } else if (education.getAchievements().size() < 2) {
                suggestions.add("Consider adding more achievements or relevant coursework");
            }

            response.put("suggestions", suggestions);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Education not found", e);
            response.put("error", "Education not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            response.put("error", "Error generating suggestions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/education/process")
    public String processEducation(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null) {
                return "redirect:/";
            }

            Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
            if (sections != null && sections.containsKey("EDUCATION")) {
                List<Education> extractedEducations = educationExtractor.extract(sections.get("EDUCATION"));
                if (extractedEducations != null && !extractedEducations.isEmpty()) {
                    for (Education education : extractedEducations) {
                        education.setResumeId(resume.getId());
                        educationRepository.save(education);
                    }
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Education information processed successfully");
                } else {
                    redirectAttributes.addFlashAttribute("infoMessage", "No education information found to process");
                }
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "No education section found in the uploaded document");
            }

            return "redirect:/builder/education-list";
        } catch (Exception e) {
            log.error("Error processing education information", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error processing education information: " + e.getMessage());
            return "redirect:/builder/education";
        }
    }

    @GetMapping("/skills")
    public String showSkills() {
        return "builder-skills";
    }

    // In ResumeBuilderController.java

    @GetMapping("/skills-list")
    @SuppressWarnings("unchecked")
    public String showSkillsList(HttpSession session, Model model) {
        log.info("Entering showSkillsList");
        Map<String, String> sections = (Map<String, String>) session.getAttribute("rawSections");
        ResumeDocument resume = (ResumeDocument) session.getAttribute("resumeData");

        if (sections == null || resume == null) {
            log.error("Sections or resume is null in session");
            return "redirect:/";
        }

        log.info("Resume ID: {}, Has SKILLS section: {}",
                resume.getId(), sections.containsKey("SKILLS"));

        try {
            if (resume.getSkills() == null) {
                if (sections.containsKey("SKILLS")) {
                    log.info("Extracting skills from section text");
                    Skills skills = null;
                    try {
                        skills = skillsExtractor.extract(sections.get("SKILLS"));
                        log.info("Extracted skills: {}", skills.getAllSkills());
                    } catch (Exception e) {
                        log.error("Error extracting skills, creating empty skills object", e);
                        skills = new Skills();
                    }

                    skills.setResumeId(resume.getId());
                    log.info("Saving skills with resumeId: {}", resume.getId());

                    skills = skillsRepository.save(skills);
                    log.info("Saved skills with ID: {}", skills.getId());

                    resume.setSkills(skills);
                    resume = resumeRepository.save(resume);
                    log.info("Updated resume with skills reference");
                } else {
                    log.warn("No SKILLS section found in resume");
                    resume.setSkills(new Skills());
                    resume = resumeRepository.save(resume);
                }
            } else {
                log.info("Using existing skills with ID: {}", resume.getSkills().getId());
                resume.getSkills().updateAllSkills();
            }

            model.addAttribute("skills", resume.getSkills());
            return "builder-skills-list";
        } catch (Exception e) {
            log.error("Error in showSkillsList", e);
            model.addAttribute("error", "Error processing skills: " + e.getMessage());
            return "error";
        }
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey(DuplicateKeyException ex) {
        return ResponseEntity.badRequest().body("Skills for this resume already exist");
    }

    @PostMapping("/skills/add")
    @ResponseBody
    public ResponseEntity<?> addSkill(
            @RequestParam String skill,
            HttpSession session) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null || resume.getId() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("No active resume found");
            }

            // Get or create skills
            Skills skills = resume.getSkills();
            if (skills == null) {
                skills = new Skills();
                skills.setResumeId(resume.getId());
                skills = skillsRepository.save(skills);
                resume.setSkills(skills);
                resumeRepository.save(resume);
            }

            // Add the skill
            skills.addSkill(skill);
            skillsRepository.save(skills);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error adding skill", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding skill: " + e.getMessage());
        }
    }

    @PostMapping("/skills/remove")
    @ResponseBody
    public ResponseEntity<?> removeSkill(
            @RequestParam String skill,
            HttpSession session) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null || resume.getSkills() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No skills found to remove");
            }

            Skills skills = resume.getSkills();
            skills.removeSkill(skill);
            skillsRepository.save(skills);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error removing skill", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing skill: " + e.getMessage());
        }
    }

    @GetMapping("/skills/list")
    @ResponseBody
    public ResponseEntity<?> listSkills(HttpSession session) {
        try {
            ResumeDocument resume = getOrCreateResume(session);
            if (resume == null || resume.getSkills() == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(resume.getSkills().getAllSkills());
        } catch (Exception e) {
            log.error("Error listing skills", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error listing skills: " + e.getMessage());
        }
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
