package com.jag.aires;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.jag.aires.repository")
public class AiresApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiresApplication.class, args);
	}

}
