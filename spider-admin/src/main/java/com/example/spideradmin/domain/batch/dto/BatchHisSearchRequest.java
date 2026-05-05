package com.example.spideradmin.domain.batch.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배치 수행 이력 검색 요청 DTO
 * <p>페이징 및 검색 조건을 포함합니다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHisSearchRequest {

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
     * 배치 APP ID (LIKE 검색)
     */
    private String batchAppId;

    /**
     * 인스턴스 ID (필터)
     */
    private String instanceId;

    /**
     * 상태 코드 (0: 시작됨, 1: 정상 종료, -1: 대상건수 알수없음)
     */
    private String resRtCode;

    /**
     * 배치 실행 일자 (YYYYMMDD)
     */
    private String batchDate;

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
