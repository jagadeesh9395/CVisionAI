package com.jag.aires.repository;

import com.jag.aires.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    // Custom query methods can be added here if needed
}
