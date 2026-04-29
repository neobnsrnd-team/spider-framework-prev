package com.example.spider_admin.domain.reactcmsadmindeployment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 배포 실행 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsAdminDeployPushRequest {

    /** 배포할 페이지 ID (PAGE_TYPE='REACT', APPROVE_STATE='APPROVED' 상태여야 함) */
    @NotBlank(message = "페이지 ID는 필수입니다.")
    private String pageId;
}
