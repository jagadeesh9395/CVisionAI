package com.jag.aires.repository;

import com.jag.aires.model.WorkExperience;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkExperienceRepository extends MongoRepository<WorkExperience, String> {
}
