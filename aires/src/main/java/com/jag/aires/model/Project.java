package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class Project {
    private String title;
    private String client;
    private String duration;

    @JsonProperty("skills_used")
    private List<String> skillsUsed;

    private String server;
    private List<String> tools;
    private String role;
    private String description;
    private List<String> responsibilities;
}
