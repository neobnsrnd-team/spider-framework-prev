package com.example.spideradmin.domain.cmsapproval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 승인 관리 목록 응답 DTO (SPW_CMS_PAGE 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsApprovalPageResponse {

    /** 페이지 ID (PAGE_ID) */
    private String pageId;

    /** 페이지명 (PAGE_NAME) */
    private String pageName;

    /** 뷰 모드 (VIEW_MODE: mobile / PC) */
    private String viewMode;

    /** 작성자명 (CREATE_USER_NAME) */
    private String createUserName;

    /** 승인 상태 (APPROVE_STATE: WORK / PENDING / APPROVED / REJECTED) */
    private String approveState;

    /** 공개 여부 (IS_PUBLIC: Y / N) */
    private String isPublic;

    /** 노출 시작일 (BEGINNING_DATE, YYYY-MM-DD) */
    private String beginningDate;

    /** 노출 종료일 (EXPIRED_DATE, YYYY-MM-DD) */
    private String expiredDate;

    /** 최종 수정일시 (LAST_MODIFIED_DTIME, YYYY-MM-DD HH24:MI:SS) */
    private String lastModifiedDtime;

    /** 이력 존재 여부 (0: 없음, 1: 있음) — 롤백 버튼 노출 기준 */
    private int hasApproveHistory;

    /**
     * 화면 표시용 상태 (DISPLAY_STATE)
     * IS_PUBLIC=N + EXPIRED_DATE < SYSDATE → 만료
     * IS_PUBLIC=N                           → 비공개
     * 그 외                                 → APPROVE_STATE 값
     */
    private String displayState;

    /** 배포 여부 (0: 미배포, 1: 배포완료) — FILE_CRC_VALUE IS NOT NULL (#308) */
    private int isDeployed;
}
