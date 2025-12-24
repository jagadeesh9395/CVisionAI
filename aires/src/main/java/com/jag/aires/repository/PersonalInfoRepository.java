package com.jag.aires.repository;

import com.jag.aires.model.PersonalInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalInfoRepository extends MongoRepository<PersonalInfo, String> {
}
