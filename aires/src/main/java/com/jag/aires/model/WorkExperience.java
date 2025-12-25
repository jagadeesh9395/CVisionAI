// src/main/java/com/jag/aires/model/WorkExperience.java
package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "work_experiences")
public class WorkExperience {
    @Id
    private String id;

    @NotBlank(message = "Company name is required")
    private String company;

    private String location;

    @NotBlank(message = "Job title is required")
    private String role;

    @NotNull(message = "Start date is required")
    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    @Field("description")
    private List<String> description = new ArrayList<>();

    @Field("resume_id")
    private String resumeId;

    public boolean isCurrent() {
        return endDate == null || endDate.trim().isEmpty() || "Present".equalsIgnoreCase(endDate);
    }

    public String getFormattedDateRange() {
        if (startDate == null) return "";
        String end = isCurrent() ? "Present" : endDate;
        return startDate + " - " + end;
    }
}