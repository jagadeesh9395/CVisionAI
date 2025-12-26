// src/main/java/com/jag/aires/repository/WorkExperienceRepository.java
package com.jag.aires.repository;

import com.jag.aires.model.WorkExperience;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkExperienceRepository extends MongoRepository<WorkExperience, String> {
    List<WorkExperience> findByResumeId(String resumeId);
    Optional<WorkExperience> findByIdAndResumeId(String id, String resumeId);
    void deleteByIdAndResumeId(String id, String resumeId);
}