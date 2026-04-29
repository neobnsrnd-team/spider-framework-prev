package com.example.spider_admin.domain.workgroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업 그룹 생성 요청 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkGroupCreateRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String groupName;

    private String groupDesc;
}
