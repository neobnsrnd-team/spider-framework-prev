package com.example.admin_demo.domain.worktask.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file WorkTaskResponse.java
 * @description 작업함 목록 응답 DTO.
 *              FWK_USER_MENU + FWK_MENU + FWK_USER_WORK_TASK + FWK_USER_WORK_GROUP 조인 결과.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTaskResponse {

    private String userId;
    private String menuId;
    private String menuName;
    private String menuUrl;
    private String authCode;

    /** 소속 그룹 ID. null 이면 미분류 */
    private String workGroupId;
    /** 소속 그룹명. null 이면 미분류 */
    private String groupName;

    private Integer taskOrder;
}
