package com.example.spideradmin.domain.cmsdeployment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 배포 실행 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDeployPushRequest {

    /** 배포할 페이지 ID (APPROVE_STATE = APPROVED 상태여야 함) */
    @NotBlank(message = "페이지 ID는 필수입니다.")
    private String pageId;
}
