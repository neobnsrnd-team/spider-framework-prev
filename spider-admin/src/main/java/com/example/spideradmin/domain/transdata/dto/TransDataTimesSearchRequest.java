package com.example.spideradmin.domain.transdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 실행 이력 검색 조건 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataTimesSearchRequest {

    private String userId;

    private String tranResult;
}
