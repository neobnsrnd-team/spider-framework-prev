package com.example.spideradmin.domain.batch.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배치 앱 검색 요청 DTO
 * <p>페이징 및 검색 조건을 포함합니다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAppSearchRequest {

    // ==================== 페이징 파라미터 ====================

    /**
     * 현재 페이지 (1-based, 클라이언트 기준)
     */
    @Builder.Default
    private Integer page = 1;

    /**
     * 페이지당 항목 수
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * 정렬 필드
     */
    private String sortBy;

    /**
     * 정렬 방향 (ASC, DESC)
     */
    private String sortDirection;

    // ==================== 검색 파라미터 ====================

    /**
     * 검색 필드: "batchAppName" or "batchAppId"
     */
    private String searchField;

    /**
     * 검색 키워드
     */
    private String searchValue;

    /**
     * 인스턴스 ID 필터
     */
    private String instanceIdFilter;

    /**
     * 배치 주기 필터
     */
    private String batchCycleFilter;

    // ==================== 변환 메서드 ====================

    /**
     * PageRequest로 변환 (0-based 인덱스)
     */
    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1)) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
