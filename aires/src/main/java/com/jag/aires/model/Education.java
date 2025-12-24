package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Education {
    private String degree;
    private String institution;
    private String location;
    private String startYear;
    private String endYear;
    private Double percentage;
    private String description;
}
