package com.example.spider_admin.global.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pagination response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content; // 데이터 목록
    private Integer currentPage; // 현재 페이지 (1부터 시작)
    private Integer totalPages; // 전체 페이지 수
    private Long totalElements; // 전체 항목 수
    private Integer size; // 페이지당 항목 수
    private Boolean hasNext; // 다음 페이지 존재 여부
    private Boolean hasPrevious; // 이전 페이지 존재 여부

    /**
     * 네이티브 페이징용 팩토리 메서드
     *
     * @param content 현재 페이지의 데이터 목록
     * @param total   전체 데이터 건수
     * @param page    현재 페이지 (0-based)
     * @param size    페이지 크기
     */
    public static <T> PageResponse<T> of(List<T> content, long total, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        int currentPage = page + 1; // 0-based → 1-based (기존 응답 스키마 유지)
        return PageResponse.<T>builder()
                .content(content)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(total)
                .size(size)
                .hasNext(currentPage < totalPages)
                .hasPrevious(currentPage > 1)
                .build();
    }
}
