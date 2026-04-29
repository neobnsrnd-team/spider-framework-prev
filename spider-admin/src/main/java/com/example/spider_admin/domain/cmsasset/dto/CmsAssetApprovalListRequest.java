package com.example.spider_admin.domain.cmsasset.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결재자 "이미지 승인 관리 화면" 목록 조회 요청 DTO.
 *
 * <p>{@code assetState}가 비어있는 경우 기본적으로 PENDING 만 조회한다(XML 분기).
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsAssetApprovalListRequest {

    /** 승인 상태 필터. 비어있으면 PENDING 기본 */
    private String assetState;

    /** 업로더 ID 필터 */
    private String uploaderId;

    /** 업무 카테고리 필터 (BUSINESS_CATEGORY) */
    private String businessCategory;

    /** 노출 여부 필터 (USE_YN) */
    private String useYn;

    /** 검색어 — CREATE_USER_NAME / CREATE_USER_ID / ASSET_NAME LIKE */
    private String search;

    /** 조회 시작일 (YYYY-MM-DD). CREATE_DATE 기준 */
    private String startDate;

    /** 조회 종료일 (YYYY-MM-DD). 종료일 포함 범위 */
    private String endDate;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
