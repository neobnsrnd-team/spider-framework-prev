package com.example.admin_demo.domain.userworkgroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file UserWorkGroupResponse.java
 * @description 사용자 작업 그룹 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkGroupResponse {

    private String userId;
    private String groupId;
    private String groupName;
    private Integer groupOrder;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}
