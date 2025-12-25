package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "educations")
public class Education {
    @Id
    private String id;

    @NotBlank(message = "Institution name is required")
    private String institution;

    private String location;

    @NotBlank(message = "Degree is required")
    private String degree;

    private String fieldOfStudy;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    @Field("start_date")
    private Date startDate;

    @DateTimeFormat(pattern = "yyyy-MM")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    @Field("end_date")
    private Date endDate;

    @Field("is_current")
    private Boolean isCurrent = false;  // Changed from boolean to Boolean

    private String grade;

    private List<String> achievements = new ArrayList<>();

    @Field("resume_id")
    private String resumeId;
}
