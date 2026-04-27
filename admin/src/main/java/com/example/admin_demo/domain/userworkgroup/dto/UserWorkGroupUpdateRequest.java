package com.example.admin_demo.domain.userworkgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file UserWorkGroupUpdateRequest.java
 * @description 사용자 작업 그룹 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkGroupUpdateRequest {

    @NotBlank
    @Size(max = 20)
    private String userId;

    @NotBlank
    @Size(max = 20)
    private String groupId;

    @NotBlank
    @Size(max = 100)
    private String groupName;

    private Integer groupOrder;
}
