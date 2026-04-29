package com.example.spideradmin.domain.cmsdashboard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 페이지 승인 요청 DTO — APPROVE_STATE: WORK / REJECTED / APPROVED → PENDING
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsDashboardApproveRequestDto {

    /** 승인자 ID (APPROVER_ID) — 이름은 서버에서 DB 조회하여 결정 (위변조 방지) */
    private String approverId;

    /** 노출 시작일 (YYYY-MM-DD, 선택) */
    private String beginningDate;

    /** 노출 종료일 (YYYY-MM-DD, 선택) */
    private String expiredDate;
}
