package com.jag.aires.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.jag.aires.model.PersonalInfo;

@Controller
public class CVisionController {

    @GetMapping("/")
    public String indexPage() {
        return "index";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @GetMapping("/greet")
    public String greetPage(Model model) {
        model.addAttribute("userName", "Jagadeeshwar");
        return "greet";
    }

    @GetMapping("/analysis")
    public String analysisPage() {
        return "analysis";
    }

    @GetMapping("/builder-basics")
    public String builderBasicsPage(Model model, @RequestParam(value = "firstname", required = false) String firstname,
                                  @RequestParam(value = "lastname", required = false) String lastname) {
        PersonalInfo personalInfo = new PersonalInfo();
        
        if (firstname != null) {
            personalInfo.setFirstName(firstname);
            model.addAttribute("firstname", firstname);
        }
        if (lastname != null) {
            personalInfo.setLastName(lastname);
            model.addAttribute("lastname", lastname);
        }
        
        model.addAttribute("personalInfo", personalInfo);
        return "builder-basics";
    }

    @GetMapping("/builder-experience")
    public String builderExperiencePage() {
        return "builder-experience";
    }

    @GetMapping("/builder-experience-list")
    public String builderExperienceListPage() {
        return "builder-experience-list";
    }

    @GetMapping("/builder-education")
    public String builderEducationPage() {
        return "builder-education";
    }

    @GetMapping("/builder-education-list")
    public String builderEducationListPage() {
        return "builder-education-list";
    }

    @GetMapping("/builder-skills")
    public String builderSkillsPage() {
        return "builder-skills";
    }

    @GetMapping("/builder-skills-list")
    public String builderSkillsListPage() {
        return "builder-skills-list";
    }

    @GetMapping("/builder-summary")
    public String builderSummaryPage() {
        return "builder-summary";
    }

    @GetMapping("/builder-summary-edit")
    public String builderSummaryEditPage() {
        return "builder-summary-edit";
    }

    @GetMapping("/builder-finalize")
    public String builderFinalizePage() {
        return "builder-finalize";
    }
}
