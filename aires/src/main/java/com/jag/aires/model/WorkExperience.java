// src/main/java/com/jag/aires/model/WorkExperience.java
package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jag.aires.util.ExperiencePeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
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

    //    @DocumentReference
    @Field("period")
    private ExperiencePeriod period;

    public ExperiencePeriod getPeriod() {
        if (period == null) {
            period = new ExperiencePeriod();
        }
        return period;
    }

    public void setPeriod(ExperiencePeriod period) {
        this.period = period;
    }

    @Field("description")
    private List<String> description = new ArrayList<>();

    @Field("resume_id")
    private String resumeId;

}