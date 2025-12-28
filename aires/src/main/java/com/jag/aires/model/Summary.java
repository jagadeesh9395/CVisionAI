package com.jag.aires.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "summaries")
public class Summary {
    @Id
    private String id;

    @Field("resume_id")
    private String resumeId;

    private List<String> bulletPoints;
    private String version;
}