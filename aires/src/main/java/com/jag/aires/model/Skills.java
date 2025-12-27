package com.jag.aires.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Document(collection = "skills")
public class Skills {
    @Id
    private String id;

    @Field("resume_id")
    @Indexed(unique = true)
    private String resumeId;

    @JsonProperty("java_technologies")
    private List<String> javaTechnologies = new ArrayList<>();

    @JsonProperty("web_technologies")
    private List<String> webTechnologies = new ArrayList<>();

    @JsonProperty("distributed_technologies")
    private List<String> distributedTechnologies = new ArrayList<>();

    private List<String> frameworks = new ArrayList<>();
    private List<String> databases = new ArrayList<>();

    @JsonProperty("application_servers")
    private List<String> applicationServers = new ArrayList<>();

    @JsonProperty("web_servers")
    private List<String> webServers = new ArrayList<>();

    private List<String> tools = new ArrayList<>();

    @JsonProperty("unit_testing")
    private List<String> unitTesting = new ArrayList<>();

    @JsonProperty("design_patterns")
    private List<String> designPatterns = new ArrayList<>();

    private List<String> ide = new ArrayList<>();

    // New field to store all skills as a single list
    private List<String> allSkills = new ArrayList<>();
    // Helper methods
    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty() && !allSkills.contains(skill.trim())) {
            allSkills.add(skill.trim());
        }
    }

    public void removeSkill(String skill) {
        if (skill != null) {
            allSkills.removeIf(s -> s.equalsIgnoreCase(skill.trim()));
        }
    }

    /**
     * Combines all skills from different categories into a single list
     * @return List of all unique skills
     */
    public List<String> getAllSkills() {
        if (allSkills == null || allSkills.isEmpty()) {
            allSkills = Stream.of(
                            javaTechnologies, webTechnologies, distributedTechnologies,
                            frameworks, databases, applicationServers, webServers,
                            tools, unitTesting, designPatterns, ide)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .filter(skill -> !skill.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return allSkills;
    }

    /**
     * Updates the allSkills list with current values from all categories
     */
    public void updateAllSkills() {
        this.allSkills = getAllSkills();
    }
}
