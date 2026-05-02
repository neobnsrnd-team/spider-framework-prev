/**
 * @file ReactApprovalResponse.java
 * @description 승인 관리 메뉴의 목록 조회용 경량 DTO.
 *     PENDING_APPROVAL 상태 코드 목록을 반환할 때 사용하며,
 *     CLOB 컬럼(REACT_CODE 등)은 포함하지 않는다.
 * @returns codeId, title, componentName, figmaUrl, status, createUserId, createDtime
 */
package com.example.reactplatform.domain.reactgenerate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactApprovalResponse {

    private String codeId;
    private String title;
    private String componentName;
    private String figmaUrl;
    private String status;
    private String createUserId;
    private String createDtime;
}
