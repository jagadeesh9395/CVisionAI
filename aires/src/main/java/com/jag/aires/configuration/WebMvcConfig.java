package com.jag.aires.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, YearMonth.class, source -> {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            return YearMonth.parse(source, DateTimeFormatter.ofPattern("MM/yyyy"));
        });
    }
}