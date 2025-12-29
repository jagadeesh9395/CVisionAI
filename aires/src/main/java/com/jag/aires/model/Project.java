package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jag.aires.util.ExperiencePeriod;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "projects")
public class Project {
    @JsonProperty("projectId")
    private String projectId;

    private String title;
    private String client;

    @JsonProperty("duration")
    private ExperiencePeriod duration;

    @JsonProperty("skillsUsed")
    private List<String> skillsUsed;

    private String server;
    private List<String> tools;
    private String role;
    private String description;

    @JsonProperty("rolesAndResponsibilities")
    private List<String> rolesAndResponsibilities;

    @JsonProperty("isCurrent")
    private boolean isCurrent;

    @JsonProperty("projectUrl")
    private String projectUrl;

    @JsonProperty("repositoryUrl")
    private String repositoryUrl;

    @JsonProperty("displayOrder")
    private int displayOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}