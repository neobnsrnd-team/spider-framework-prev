package com.example.admin_demo.domain.workgroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업 그룹 수정 요청 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkGroupUpdateRequest {
    @NotBlank
    private String groupId;

    @NotBlank
    private String groupName;

    private String groupDesc;
}
