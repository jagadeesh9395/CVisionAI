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

    @Query(value = "{'resumeId' : ?0}", delete = true)
    void deleteByResumeId(String resumeId);

    @Query(value = "{'resumeId' : ?0}", fields = "{'allSkills' : 1}")
    Optional<Skills> findSkillsByResumeId(String resumeId);

    @Query("{$addToSet: {'allSkills': ?1}}")
    void addSkill(String resumeId, String skill);

    @Query("{$pull: {'allSkills': ?1}}")
    void removeSkill(String resumeId, String skill);
}