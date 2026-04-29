package com.example.spideradmin.global.util;

import com.example.spideradmin.global.dto.DateRangeResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateRangeUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateRangeUtil() {}

    public static DateRangeResponse calculateRange(ScreenDateType type) {
        return calculateRange(type, LocalDate.now());
    }

    static DateRangeResponse calculateRange(ScreenDateType type, LocalDate baseDate) {
        LocalDate startDate =
                switch (type) {
                    case TODAY -> baseDate;
                    case WEEK -> baseDate.minusWeeks(1);
                    case MONTH_1 -> baseDate.minusMonths(1);
                    case MONTH_3 -> baseDate.minusMonths(3);
                    case MONTH_6 -> baseDate.minusMonths(6);
                    case YEAR -> baseDate.minusYears(1);
                    case CUSTOM -> throw new IllegalArgumentException("CUSTOM type requires explicit date range");
                };
        return new DateRangeResponse(startDate, baseDate);
    }

    public static boolean isValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMAT);
    }

    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMAT);
    }
}
