package com.example.spider_admin.domain.article.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 파일 다운로드 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDownloadResponse {

    /** DB에 저장된 파일 경로 */
    private String filePath;

    /** 원본 파일명 */
    private String originalFileName;
}
