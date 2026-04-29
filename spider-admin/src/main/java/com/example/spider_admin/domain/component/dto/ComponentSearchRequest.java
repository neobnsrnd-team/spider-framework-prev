package com.example.spider_admin.domain.component.dto;

import com.example.spider_admin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 컴포넌트 검색 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSearchRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String sortBy;
    private String sortDirection;

    private String componentId;
    private String componentName;
    private String componentType;
    private String useYn;

    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
