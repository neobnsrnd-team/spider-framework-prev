package com.example.spideradmin.domain.cmsasset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 이미지 업로드 응답 DTO — Issue #65.
 *
 * <p>CMS Builder 가 반환한 {@code assetId} 와 {@code url} 을 그대로 전달한다.
 * 브라우저는 이 값을 받아 목록을 새로고침하거나 후속 승인 요청(#53)의 기준 ID 로 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsAssetUploadResponse {

    /** CMS 가 발급한 UUID */
    private String assetId;

    /** CMS 기준 이미지 URL (예: "/static/xxx.png" 상대경로) */
    private String url;
}
