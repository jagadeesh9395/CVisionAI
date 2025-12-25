// src/main/java/com/jag/aires/model/WorkExperience.java
package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    @DateTimeFormat(pattern = "yyyy-MM")
    @JsonProperty("start_date")
    private Date startDate;

    @JsonProperty("end_date")
    @DateTimeFormat(pattern = "yyyy-MM")
    private Date endDate;

    @Field("description")
    private List<String> description = new ArrayList<>();

    @Field("resume_id")
    private String resumeId;

}