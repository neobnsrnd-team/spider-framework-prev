package com.example.spider_admin.domain.datasource.dto;

import com.example.spider_admin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 데이터소스 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceSearchRequest {

    // ==================== 페이징 파라미터 ====================

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String sortBy;
    private String sortDirection;

    // ==================== 검색 파라미터 ====================

    /** 검색 필드 (dbId / dbName / dbUserId) */
    private String searchField;

    /** 검색 값 */
    private String searchValue;

    /** JNDI 여부 필터 (Y / N) */
    private String jndiYnFilter;

    // ==================== 변환 메서드 ====================

    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
