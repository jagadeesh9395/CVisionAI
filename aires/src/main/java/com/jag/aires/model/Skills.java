package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class Skills {
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
