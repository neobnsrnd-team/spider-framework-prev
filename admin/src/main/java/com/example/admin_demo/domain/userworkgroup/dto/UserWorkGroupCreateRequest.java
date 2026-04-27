package com.example.admin_demo.domain.userworkgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file UserWorkGroupCreateRequest.java
 * @description 사용자 작업 그룹 생성 요청 DTO. GROUP_ID는 서비스에서 자동 생성됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkGroupCreateRequest {

    /** 사용자 ID (FWK_USER_WORK_GROUP PK 구성 요소) */
    @NotBlank
    @Size(max = 20)
    private String userId;

    /** 그룹명 */
    @NotBlank
    @Size(max = 100)
    private String groupName;

    /** 정렬 순서 (미입력 시 0) */
    private Integer groupOrder;
}
