package com.example.admin_demo.domain.workgroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업 그룹 조회 응답 DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkGroupResponse {
    /** 그룹 ID (userId + 3자리 일련번호). */
    private String groupId;

    private String groupName;
    private String groupDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}
