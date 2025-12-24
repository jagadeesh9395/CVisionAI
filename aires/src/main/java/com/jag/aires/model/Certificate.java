package com.jag.aires.model;

import lombok.Data;

@Data
public class Certificate {
    private String name;
    private String issuingOrganization;
    private String issueDate; // Format: YYYY-MM or YYYY
    private String expirationDate; // Optional: Format: YYYY-MM or YYYY
    private String credentialId; // Optional: Certificate ID or URL
    private String credentialUrl; // Optional: URL to verify the certificate
    
    // You can add more fields as needed
}
