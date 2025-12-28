package com.jag.aires.repository;

import com.jag.aires.model.Summary;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummaryRepository extends MongoRepository<Summary, String> {
    
    Optional<Summary> findByResumeId(String resumeId);
    
    @Query("{ 'resume._id': ?0 }")
    List<Summary> findAllByResumeId(String resumeId);
    
    @Query(value = "{ 'resume._id': ?0 }", delete = true)
    void deleteByResumeId(String resumeId);
    
    boolean existsByResumeId(String resumeId);
}