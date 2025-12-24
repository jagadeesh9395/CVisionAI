package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class Skills {
    private List<String> javaTechnologies;
    private List<String> webTechnologies;
    private List<String> distributedTechnologies;
    private List<String> frameworks;
    private List<String> databases;
    private List<String> applicationServers;
    private List<String> webServers;
    private List<String> tools;
    private List<String> unitTesting;
    private List<String> designPatterns;
    private List<String> ide;
}
