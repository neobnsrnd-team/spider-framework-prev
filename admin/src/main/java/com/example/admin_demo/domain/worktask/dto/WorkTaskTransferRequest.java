package com.example.admin_demo.domain.worktask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file WorkTaskTransferRequest.java
 * @description 작업함 권한이양 요청 DTO.
 *              fromUserId 의 선택 메뉴 권한(FWK_USER_MENU)을 toUserId 에게 복사 후 자신 삭제.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTaskTransferRequest {

    @NotBlank
    private String fromUserId;

    @NotBlank
    private String toUserId;

    /** 이양할 메뉴 ID 목록 */
    @NotEmpty
    private List<String> menuIds;
}
