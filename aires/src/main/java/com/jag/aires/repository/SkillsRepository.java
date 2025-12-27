package com.jag.aires.repository;

import com.jag.aires.model.Skills;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillsRepository extends MongoRepository<Skills, String> {
    Optional<Skills> findByResumeId(String resumeId);

    @Query(value = "{'resumeId': ?0}", delete = true)
    void deleteByResumeId(String resumeId);

    @Query("{'resumeId': ?0, 'skills': {$exists: true, $ne: []}}")
    Optional<Skills> findSkillsByResumeId(String resumeId);
    @Query("{$set: {'allSkills': ?1}}")
    void updateSkills(String resumeId, List<String> skills);
}