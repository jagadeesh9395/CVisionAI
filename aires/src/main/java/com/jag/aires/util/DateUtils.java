package com.jag.aires.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    private static final String DATE_FORMAT = "MMM yyyy";
    
    public static String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
    
    public static String formatDateRange(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) return "";
        
        String start = startDate != null ? formatDate(startDate) : "";
        String end = endDate != null ? formatDate(endDate) : "Present";
        
        if (start.isEmpty()) return end;
        if (end.isEmpty()) return start;
        
        return start + " - " + end;
    }
}