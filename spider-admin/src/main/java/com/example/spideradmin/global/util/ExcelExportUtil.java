package com.example.spideradmin.global.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public final class ExcelExportUtil {

    public static final int MAX_ROW_LIMIT = 50_000;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ExcelExportUtil() {}

    public static String generateFileName(String screenName, LocalDate date) {
        return screenName + "_" + date.format(FILE_DATE_FORMAT) + ".xlsx";
    }

    public static boolean isWithinLimit(int count) {
        return count <= MAX_ROW_LIMIT;
    }

    public static byte[] createWorkbook(
            String sheetName, List<ExcelColumnDefinition> columns, List<Map<String, Object>> dataList)
            throws IOException {

        if (dataList.size() > MAX_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Row count " + dataList.size() + " exceeds MAX_ROW_LIMIT " + MAX_ROW_LIMIT);
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                ExcelColumnDefinition col = columns.get(i);
                var cell = headerRow.createCell(i);
                cell.setCellValue(col.getHeader());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, col.getWidth() * 256);
            }

            // Data rows
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Map<String, Object> rowData = dataList.get(rowIdx);
                Row row = sheet.createRow(rowIdx + 1);

                for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                    ExcelColumnDefinition col = columns.get(colIdx);
                    Object value = rowData.get(col.getFieldName());
                    var cell = row.createCell(colIdx);
                    if (value instanceof Number num) {
                        cell.setCellValue(num.doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private static CellStyle createDataStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
