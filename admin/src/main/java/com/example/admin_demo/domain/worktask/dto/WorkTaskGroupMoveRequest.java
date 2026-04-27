package com.example.admin_demo.domain.worktask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file WorkTaskGroupMoveRequest.java
 * @description 작업함 항목 그룹 이동 요청 DTO.
 *              선택한 메뉴 항목들을 지정한 그룹으로 이동(FWK_USER_WORK_TASK UPSERT)한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTaskGroupMoveRequest {

    @NotBlank
    private String userId;

    /** 이동할 메뉴 ID 목록 */
    @NotEmpty
    private List<String> menuIds;

    /** 이동할 그룹 ID. null 이면 미분류로 이동 */
    private String workGroupId;
}
