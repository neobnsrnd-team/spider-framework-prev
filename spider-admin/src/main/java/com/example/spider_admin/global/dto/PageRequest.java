package com.example.spider_admin.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pagination request (0-based page index)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequest {

    @Builder.Default
    private Integer page = 0; // 현재 페이지 (0부터 시작, 0-based index)

    @Builder.Default
    private Integer size = 10; // 페이지당 항목 수

    private String sortBy; // 정렬 필드
    private String sortDirection; // ASC, DESC

    // 검색 조건
    private String searchField; // 검색 타입 (예: codeGroupName, codeGroupId)
    private String searchValue; // 검색 키워드

    /**
     * Get page number (0-based)
     * Returns minimum of 0 to prevent negative page numbers
     */
    public int getPage() {
        return Math.max(0, page);
    }

    /**
     * Set sort direction (normalizes to uppercase)
     * Accepts: 'asc', 'ASC', 'desc', 'DESC', null
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = (sortDirection != null) ? sortDirection.trim().toUpperCase() : null;
    }

    /**
     * Get sort direction (always uppercase: ASC or DESC)
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * Get page size
     */
    public int getSize() {
        return Math.max(1, size);
    }

    /**
     * Oracle ROWNUM offset (0-based). page=0,size=10 → offset=0
     */
    public int getOffset() {
        return getPage() * getSize();
    }

    /**
     * Oracle ROWNUM endRow. page=0,size=10 → endRow=10
     */
    public int getEndRow() {
        return getOffset() + getSize();
    }
}
