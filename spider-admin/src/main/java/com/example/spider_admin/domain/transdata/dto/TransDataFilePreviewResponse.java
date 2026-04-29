package com.example.spider_admin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 데이터 파일 미리보기 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataFilePreviewResponse {

    private String fileName; // 파일명

    private String content; // 파일 내용

    private Long fileSize; // 파일 크기

    private Boolean truncated; // 잘림 여부 (1MB 초과 시 true)
}
