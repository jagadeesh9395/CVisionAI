package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "skills")
public class Skills {
    @Id
    private String id;

    @Field("resume_id")
    private String resumeId;

    @JsonProperty("java_technologies")
    private List<String> javaTechnologies;

    @JsonProperty("web_technologies")
    private List<String> webTechnologies;

    @JsonProperty("distributed_technologies")
    private List<String> distributedTechnologies;

    private List<String> frameworks;
    private List<String> databases;

    @JsonProperty("application_servers")
    private List<String> applicationServers;

    @JsonProperty("web_servers")
    private List<String> webServers;

    private List<String> tools;

    @JsonProperty("unit_testing")
    private List<String> unitTesting;

    @JsonProperty("design_patterns")
    private List<String> designPatterns;

    private List<String> ide;

}
