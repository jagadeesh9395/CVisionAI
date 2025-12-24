package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "resumes")
public class ResumeDocument {
    @Id
    private String id;
    
    private PersonalInfo personalInfo;
    private String objective;
    private List<String> professionalSummary;
    private Skills skills;
    private List<WorkExperience> workExperience;
    private List<Education> education;
    private List<Project> projects;
    private List<Achievement> achievements;
    private List<Certificate> certificates;
    private String declaration;
}
