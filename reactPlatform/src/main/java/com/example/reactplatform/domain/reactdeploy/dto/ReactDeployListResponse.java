/**
 * @file ReactDeployListResponse.java
 * @description 배포 가능 목록 조회 응답 DTO.
 *     APPROVED 상태 코드에 최근 배포 이력 1건을 LEFT JOIN하여 반환한다.
 */
package com.example.reactplatform.domain.reactdeploy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactDeployListResponse {

    /** 코드 ID */
    private String codeId;

    /** 화면 제목 */
    private String title;

    /** 컴포넌트명 */
    private String componentName;

    /** 원본 Figma URL */
    private String figmaUrl;

    /** 코드 생성 요청자 ID */
    private String createUserId;

    /** 승인자 ID */
    private String approvalUserId;

    /** 승인 일시 (yyyyMMddHHmmss) */
    private String approvalDtime;

    /** 최근 배포 결과. {@code SUCCESS | FAILED | null} (미배포 시 null) */
    private String lastDeployStatus;

    /** 최근 배포 일시 (null 가능) */
    private String lastDeployDtime;

    /** 최근 배포 모드. {@code local | git-pr} (null 가능) */
    private String lastDeployMode;

    /** 최근 배포 PR URL — git-pr 모드에서만 존재 (null 가능) */
    private String lastPrUrl;
}
