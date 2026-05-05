package com.example.spideradmin.domain.sqlquery.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQuerySearchRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String sortBy;
    private String sortDirection;

    private String queryId;
    private String queryName;
    private String useYn;
    private String sqlGroupId;
    private String sqlGroupName;
    private String sqlType;

    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
