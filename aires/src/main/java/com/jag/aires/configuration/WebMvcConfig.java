package com.jag.aires.configuration;

import com.jag.aires.util.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // ... existing code ...
    
    @Bean
    public DateUtils dateUtils() {
        return new DateUtils();
    }
}