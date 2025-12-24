package com.jag.aires.dto;

import lombok.Data;

@Data
public class TextAnalysisRequest {
    private String text;
    private String schema;
}
