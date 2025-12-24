package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class WorkExperience {
    private String company;
    private String location;
    private String role;
    private String startDate;
    private String endDate;
    private boolean current;
    private List<String> responsibilities;
    private List<String> achievements;
}
