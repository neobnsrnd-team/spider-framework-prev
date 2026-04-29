package com.example.spider_admin.domain.reactcmsdashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS 페이지 승인 요청 DTO — APPROVE_STATE: WORK / REJECTED / APPROVED → PENDING
 */
@Getter
@Setter
@NoArgsConstructor
public class ReactCmsDashboardApproveRequestDto {

    /** 승인자 ID (APPROVER_ID) — 이름은 서버에서 DB 조회하여 결정 (위변조 방지) */
    private String approverId;

    /** 노출 시작일 (선택) — 잘못된 형식은 Jackson 역직렬화 단계에서 400으로 처리 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginningDate;

    /** 노출 종료일 (선택) */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiredDate;
}
