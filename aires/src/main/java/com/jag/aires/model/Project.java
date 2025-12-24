package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class Project {
    private String title;
    private String client;
    private String startDate;
    private String endDate;
    private List<String> skillsUsed;
    private String server;
    private List<String> tools;
    private String role;
    private String description;
    private List<String> responsibilities;
    private String teamSize;
    private String projectUrl;
    private List<String> keyAchievements;
}
