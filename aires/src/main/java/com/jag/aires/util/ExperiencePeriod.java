package com.jag.aires.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Data
public class ExperiencePeriod {
    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "MM/yyyy")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/yyyy")
    @JsonDeserialize(using = YearMonthDeserializer.class)
    @JsonSerialize(using = YearMonthSerializer.class)
    private YearMonth startDate;

    @DateTimeFormat(pattern = "MM/yyyy")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/yyyy")
    @JsonDeserialize(using = YearMonthDeserializer.class)
    @JsonSerialize(using = YearMonthSerializer.class)
    private YearMonth endDate;

    public String getDisplayPeriod() {
        if (startDate == null) return "";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        String start = startDate.format(fmt);
        String end = (endDate != null) ? endDate.format(fmt) : "Present";
        return start + " - " + end;
    }
}