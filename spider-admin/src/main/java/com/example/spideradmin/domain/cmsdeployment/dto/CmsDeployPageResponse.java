package com.example.spideradmin.domain.cmsdeployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 배포 대상 페이지 응답 DTO (SPW_CMS_PAGE APPROVE_STATE='APPROVED')
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsDeployPageResponse {

    /** 페이지 ID */
    private String pageId;

    /** 페이지명 */
    private String pageName;

    /**
     * 뷰모드 — 'mobile' / 'responsive' / 'web'(=PC).
     * 배포 미리보기 창 크기 결정에 사용. (#278)
     */
    private String viewMode;

    /** 작성자명 */
    private String createUserName;

    /** 최근 배포된 파일 URL (배포 이력 없으면 null) */
    private String deployedUrl;

    /** 노출 시작일 (yyyy-MM-dd, null 가능) */
    private String beginningDate;

    /** 노출 종료일 (yyyy-MM-dd, null 가능) */
    private String expiredDate;

    /**
     * 만료 여부 — "Y": EXPIRED_DATE &lt; SYSDATE AND IS_PUBLIC='Y', 그 외 "N"
     * 만료수동처리 버튼 표시 조건으로 사용
     */
    private String isExpired;

    /** 공개 여부 — "Y"/"N" (SPW_CMS_PAGE.IS_PUBLIC) */
    private String isPublic;
}
