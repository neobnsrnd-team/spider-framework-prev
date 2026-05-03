/**
 * @file ReactDeployHistoryResponse.java
 * @description 배포 이력 응답 DTO.
 *     전체 이력 테이블 및 코드별 이력 모달에서 공용으로 사용한다.
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
public class ReactDeployHistoryResponse {

    /** 배포 이력 UUID */
    private String deployId;

    /** 코드 ID */
    private String codeId;

    /** 화면 제목 */
    private String title;

    /** 컴포넌트명 */
    private String componentName;

    /** 배포 모드. {@code local | git-pr} */
    private String deployMode;

    /** 배포 결과. {@code SUCCESS | FAILED} */
    private String deployStatus;

    /** 실패 사유 (null 가능) */
    private String failReason;

    /** PR URL — git-pr 모드에서만 존재 (null 가능) */
    private String prUrl;

    /** 배포 실행자 ID */
    private String lastUpdateUserId;

    /** 배포 일시 (yyyyMMddHHmmss) */
    private String lastUpdateDtime;
}
