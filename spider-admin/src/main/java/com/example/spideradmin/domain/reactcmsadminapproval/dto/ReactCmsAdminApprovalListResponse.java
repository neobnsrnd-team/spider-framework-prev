package com.example.spideradmin.domain.reactcmsadminapproval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 승인 관리 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactCmsAdminApprovalListResponse {

    /** 페이지 ID */
    private String pageId;

    /** 페이지명 */
    private String pageName;

    /** 뷰 모드 (mobile / PC) */
    private String viewMode;

    /** 작성자명 */
    private String createUserName;

    /** 승인 상태 (WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 공개 여부 (Y / N) */
    private String isPublic;

    /** 노출 시작일 (YYYY-MM-DD) */
    private String beginningDate;

    /** 노출 종료일 (YYYY-MM-DD) */
    private String expiredDate;

    /** 최종 수정일시 (YYYY-MM-DD HH24:MI:SS) */
    private String lastModifiedDtime;

    /** 이전 승인 이력 존재 여부 (0: 없음, 1: 있음) — 롤백 버튼 노출 기준 */
    private int hasApproveHistory;

    /**
     * 화면 표시용 상태 (displayState)
     * IS_PUBLIC=N + EXPIRED_DATE &lt; SYSDATE → 만료
     * IS_PUBLIC=N                            → 비공개
     * 그 외                                  → APPROVE_STATE 값
     */
    private String displayState;
}
