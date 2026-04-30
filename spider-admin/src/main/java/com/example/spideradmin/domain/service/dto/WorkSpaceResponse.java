package com.example.spideradmin.domain.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** WorkSpace 팝업 조회용 응답 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSpaceResponse {

    private String workSpaceId;
    private String workSpaceName;
    private Integer threadCount;
}
