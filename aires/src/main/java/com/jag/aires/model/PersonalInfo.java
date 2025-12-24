package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class PersonalInfo {
    private String name;
    private String mobile;
    private String email;
    private String gender;
    private String nationality;
    private List<String> languagesKnown;
}
