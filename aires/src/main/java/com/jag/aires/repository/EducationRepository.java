package com.jag.aires.repository;

import com.jag.aires.model.Education;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EducationRepository extends MongoRepository<Education, String> {
    List<Education> findByResumeId(String resumeId);
    Optional<Education> findByIdAndResumeId(String id, String resumeId);
    void deleteByIdAndResumeId(String id, String resumeId);
}
