package com.example.spider_admin.domain.transdata.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * SQL 파일 업로드 결과 DTO
 */
@Getter
@Builder
public class TransDataFileUploadResponse {

    private String fileName;
    private Long size;
    private String status; // "SUCCESS" | "FAIL"
    private String message;
}
