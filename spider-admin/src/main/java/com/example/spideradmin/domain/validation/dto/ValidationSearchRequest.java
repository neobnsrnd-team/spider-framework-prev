package com.example.spideradmin.domain.validation.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Validation 검색 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationSearchRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 20;

    private String sortBy;
    private String sortDirection;

    private String validationId;
    private String validationDesc;

    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
