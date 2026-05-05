package com.example.spideradmin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 데이터 파일 검색 조건 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataFileSearchRequest {

    private String fileName; // 파일명 검색 (부분 일치)

    private String fileType; // 파일 유형 필터

    private String dateFrom; // 시작일 (yyyy-MM-dd)

    private String dateTo; // 종료일 (yyyy-MM-dd)
}
