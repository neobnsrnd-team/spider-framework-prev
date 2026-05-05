package com.example.spideradmin.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelExportUtilTest {

    private static final List<ExcelColumnDefinition> COLUMNS = List.of(
            new ExcelColumnDefinition("이름", 15, "name"),
            new ExcelColumnDefinition("나이", 10, "age"),
            new ExcelColumnDefinition("이메일", 25, "email"));

    @Test
    void createWorkbook_정상생성() throws IOException {
        List<Map<String, Object>> data = List.of(
                Map.of("name", "홍길동", "age", "30", "email", "hong@example.com"),
                Map.of("name", "김철수", "age", "25", "email", "kim@example.com"),
                Map.of("name", "이영희", "age", "28", "email", "lee@example.com"));

        byte[] result = ExcelExportUtil.createWorkbook("테스트시트", COLUMNS, data);

        assertThat(result).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("테스트시트");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(3); // header + 3 data rows
        }
    }

    @Test
    void createWorkbook_50K초과_예외발생() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 50_001; i++) {
            data.add(Map.of("name", "user" + i));
        }

        assertThatThrownBy(() -> ExcelExportUtil.createWorkbook("시트", COLUMNS, data))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createWorkbook_빈데이터_헤더만생성() throws IOException {
        byte[] result = ExcelExportUtil.createWorkbook("빈시트", COLUMNS, List.of());

        assertThat(result).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("빈시트");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(0); // header row only
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("이름");
        }
    }

    @Test
    void isWithinLimit_한계값_이하_true() {
        assertThat(ExcelExportUtil.isWithinLimit(50_000)).isTrue();
    }

    @Test
    void isWithinLimit_한계값_초과_false() {
        assertThat(ExcelExportUtil.isWithinLimit(50_001)).isFalse();
    }

    @Test
    void generateFileName_형식검증() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        String fileName = ExcelExportUtil.generateFileName("UserList", date);
        assertThat(fileName).isEqualTo("UserList_20260101.xlsx");
    }
}
