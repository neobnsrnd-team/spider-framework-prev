package com.example.spideradmin.domain.service.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 목록 검색 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceSearchRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String sortBy;
    private String sortDirection;

    private String serviceId;
    private String serviceName;
    private String serviceType;
    private String useYn;
    private String bizGroupId;
    private String reqChannelCode;
    private String componentId;
    private String componentName;
    private String loginOnlyYn;
    private String secureSignYn;
    private String bankStatusCheckYn;
    private String bizdayServiceYn;
    private String saturdayServiceYn;
    private String holidayServiceYn;

    public PageRequest toPageRequest() {
        return PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
