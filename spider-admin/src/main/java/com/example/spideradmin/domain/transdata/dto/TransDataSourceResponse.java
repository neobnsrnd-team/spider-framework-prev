package com.example.spideradmin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 생성 - 탭별 소스 데이터 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataSourceResponse {

    private String id;

    private String name;

    private String col1;

    private String col2;

    private String col3;

    private String col4;
}
