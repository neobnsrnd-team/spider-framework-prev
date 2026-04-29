package com.example.spider_admin.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.spider_admin.global.dto.DateRangeResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateRangeUtilTest {

    private static final LocalDate BASE = LocalDate.of(2026, 3, 3);

    @Test
    void calculateRange_TODAY() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.TODAY, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE);
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_WEEK() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.WEEK, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE.minusWeeks(1));
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_MONTH_1() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.MONTH_1, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE.minusMonths(1));
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_MONTH_3() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.MONTH_3, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE.minusMonths(3));
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_MONTH_6() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.MONTH_6, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE.minusMonths(6));
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_YEAR() {
        DateRangeResponse result = DateRangeUtil.calculateRange(ScreenDateType.YEAR, BASE);
        assertThat(result.getStartDate()).isEqualTo(BASE.minusYears(1));
        assertThat(result.getEndDate()).isEqualTo(BASE);
    }

    @Test
    void calculateRange_CUSTOM_예외발생() {
        assertThatThrownBy(() -> DateRangeUtil.calculateRange(ScreenDateType.CUSTOM, BASE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValidRange_정상범위() {
        assertThat(DateRangeUtil.isValidRange(BASE.minusDays(7), BASE)).isTrue();
        assertThat(DateRangeUtil.isValidRange(BASE, BASE)).isTrue();
    }

    @Test
    void isValidRange_역전된범위() {
        assertThat(DateRangeUtil.isValidRange(BASE, BASE.minusDays(1))).isFalse();
    }

    @Test
    void isValidRange_null() {
        assertThat(DateRangeUtil.isValidRange(null, BASE)).isFalse();
        assertThat(DateRangeUtil.isValidRange(BASE, null)).isFalse();
        assertThat(DateRangeUtil.isValidRange(null, null)).isFalse();
    }

    @Test
    void formatDate_형식검증() {
        assertThat(DateRangeUtil.formatDate(LocalDate.of(2026, 1, 5))).isEqualTo("2026-01-05");
    }

    @Test
    void formatDateTime_형식검증() {
        LocalDateTime dt = LocalDateTime.of(2026, 1, 5, 14, 30, 0);
        assertThat(DateRangeUtil.formatDateTime(dt)).isEqualTo("2026-01-05 14:30:00");
    }

    @Test
    void parseDate_변환() {
        assertThat(DateRangeUtil.parseDate("2026-01-01")).isEqualTo(LocalDate.of(2026, 1, 1));
    }
}
