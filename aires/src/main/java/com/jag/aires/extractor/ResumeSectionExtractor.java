package com.jag.aires.extractor;

public interface ResumeSectionExtractor<T> {
    T extract(String sectionText);

    String getPrompt(String sectionText);
}
