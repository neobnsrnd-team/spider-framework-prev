package com.example.spider_admin.domain.transdata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SQL 파일 실행 요청 DTO
 */
@Data
public class TransDataSqlExecuteRequest {

    @NotBlank
    private String filePath;
}
