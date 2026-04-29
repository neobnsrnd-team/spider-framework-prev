package com.example.spider_admin.global.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelColumnDefinition {

    private String header;
    private int width;
    private String fieldName;
}
