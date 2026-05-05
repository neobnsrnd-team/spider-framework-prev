package com.example.spideradmin.domain.cmsasset.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 현업 관리자 "이미지 승인 요청 화면" 목록 조회 요청 DTO.
 *
 * <p>{@code createUserId}는 서버(Controller)가 인증 주체(`@AuthenticationPrincipal`)에서
 * 추출해 덮어쓰므로, 클라이언트가 보낸 값은 신뢰하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsAssetRequestListRequest {

    /** 승인 상태 필터 (WORK / PENDING / APPROVED / REJECTED). null이면 전체 */
    private String assetState;

    /** 업무 카테고리 필터 (BUSINESS_CATEGORY) */
    private String businessCategory;

    /** 검색어 — ASSET_NAME, ASSET_DESC LIKE */
    private String search;

    /** 업로더 ID — 서버에서 덮어쓰므로 클라이언트 입력은 무시된다 */
    private String createUserId;

    /** 정렬 기준 */
    private String sortBy;

    /** 정렬 방향 (ASC / DESC) */
    private String sortDirection;
}
