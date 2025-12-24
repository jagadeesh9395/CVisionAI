package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Education {
    private String degree;
    private String institution;
    private String location;

    @JsonProperty("year_of_completion")
    private String yearOfCompletion;
}
