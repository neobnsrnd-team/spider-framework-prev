package com.example.spider_admin.domain.transdata.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * SQL 파일 실행 결과 DTO
 */
@Getter
@Builder
public class TransDataSqlExecuteResponse {

    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> errors;
}
