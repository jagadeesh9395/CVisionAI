package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "resumes")
public class ResumeDocument {
    @Id
    private String id;
    
    @Field("user_id")
    private String userId; // Link to user account if you have authentication
    
    @DBRef
    @Field("personal_info")
    private PersonalInfo personalInfo;
    
    private String objective;
    private List<String> professionalSummary;
    @DBRef
    @Field("skills")
    private Skills skills;
    
    @DBRef
    @Field("work_experience")
    private List<WorkExperience> workExperience;

    @DBRef
    @Field("education")
    private List<Education> education;
    private List<Project> projects;
    private List<Achievement> achievements;
    private List<Certificate> certificates;
    private String declaration;
    
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * Updates the updatedAt timestamp to current time
     */
    public void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }
}
