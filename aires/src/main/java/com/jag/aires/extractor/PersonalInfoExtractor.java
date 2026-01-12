package com.jag.aires.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.model.PersonalInfo;
import com.jag.aires.service.ai.GroqPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalInfoExtractor implements ResumeSectionExtractor<PersonalInfo> {

    private final GroqPromptService groqPromptService;
    private final ObjectMapper objectMapper;

    @Override
    public PersonalInfo extract(String sectionText) {
        String schema = """
                {
                  "personal_info": {
                    "firstName": "string",
                    "lastName": "string",
                    "email": "string",
                    "phoneNumber": "string",
                    "gender": "string",
                    "nationality": "string",
                    "languagesKnown": ["string", "string"],
                    "city": "string",
                    "country": "string",
                    "pinCode": "string"
                  }
                }
                """;

        try {
            String systemPrompt = "Extract personal information from the given text. Return the response in the following JSON format:\n" + schema;
            String response = groqPromptService.generateResponse(systemPrompt, sectionText);
            var node = objectMapper.readTree(response);
            var personalInfoNode = node.get("personal_info");

            if (personalInfoNode != null) {
                PersonalInfo personalInfo = new PersonalInfo();

                // Set fields one by one to ensure proper mapping
                if (personalInfoNode.has("firstName")) {
                    personalInfo.setFirstName(personalInfoNode.get("firstName").asText());
                }
                if (personalInfoNode.has("lastName")) {
                    personalInfo.setLastName(personalInfoNode.get("lastName").asText());
                }
                if (personalInfoNode.has("email")) {
                    personalInfo.setEmail(personalInfoNode.get("email").asText());
                }
                if (personalInfoNode.has("phoneNumber")) {
                    personalInfo.setPhoneNumber(personalInfoNode.get("phoneNumber").asText());
                }
                if (personalInfoNode.has("gender")) {
                    personalInfo.setGender(personalInfoNode.get("gender").asText());
                }
                if (personalInfoNode.has("nationality")) {
                    personalInfo.setNationality(personalInfoNode.get("nationality").asText());
                }
                if (personalInfoNode.has("languagesKnown")) {
                    List<String> languages = new ArrayList<>();
                    personalInfoNode.get("languagesKnown").forEach(lang -> languages.add(lang.asText()));
                    personalInfo.setLanguagesKnown(languages);
                }
                if (personalInfoNode.has("city")) {
                    personalInfo.setCity(personalInfoNode.get("city").asText());
                }
                if (personalInfoNode.has("country")) {
                    personalInfo.setCountry(personalInfoNode.get("country").asText());
                }
                if (personalInfoNode.has("pinCode")) {
                    personalInfo.setPinCode(personalInfoNode.get("pinCode").asText());
                }

                return personalInfo;
            }
            return new PersonalInfo();
        } catch (Exception e) {
            log.error("Error parsing personal info", e);
            return new PersonalInfo();
        }
    }

    @Override
    public String getPrompt(String sectionText) {
        return "Extract personal information including name, mobile, email, gender, nationality, and languages known.";
    }
}
