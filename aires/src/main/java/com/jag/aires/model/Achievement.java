package com.jag.aires.model;

import lombok.Data;

@Data
public class Achievement {
    private String title;
    private String description;
    private String date; // Format: YYYY-MM or YYYY
    private String issuer;
    
    // You can add more fields as needed
}
