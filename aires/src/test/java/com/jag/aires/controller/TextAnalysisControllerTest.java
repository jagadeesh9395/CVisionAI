package com.jag.aires.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jag.aires.dto.TextAnalysisRequest;
import com.jag.aires.service.ai.OllamaPromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class TextAnalysisControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OllamaPromptService ollamaPromptService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TextAnalysisController textAnalysisController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(textAnalysisController).build();
    }

    @Test
    void showAnalyzerPage_ShouldReturnTextAnalyzerView() throws Exception {
        mockMvc.perform(get("/text-analyzer"))
               .andExpect(status().isOk())
               .andExpect(view().name("text-analyzer"));
    }

    @Test
    void analyzeText_WithValidRequest_ShouldReturnAnalysisResult() throws Exception {
        String mockResponse = "{\"summary\":\"Test summary\",\"keyPoints\":[\"point1\",\"point2\"]}";
        when(ollamaPromptService.analyzeText(anyString(), anyString())).thenReturn(mockResponse);

        TextAnalysisRequest request = new TextAnalysisRequest();
        request.setText("Test text to analyze");
        request.setSchema("{\"summary\":\"string\",\"keyPoints\":[\"string\"]}");

        mockMvc.perform(post("/text-analyzer/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(content().json(mockResponse));
    }

    @Test
    void analyzeText_WithInvalidJsonSchema_ShouldReturnBadRequest() throws Exception {
        TextAnalysisRequest request = new TextAnalysisRequest();
        request.setText("Test text");
        request.setSchema("invalid json");

        mockMvc.perform(post("/text-analyzer/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }
}
