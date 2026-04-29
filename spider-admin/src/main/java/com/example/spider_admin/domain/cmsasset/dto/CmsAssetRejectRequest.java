package com.example.spider_admin.domain.cmsasset.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 이미지 반려 요청 DTO — ASSET_STATE: PENDING → REJECTED 전용.
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsAssetRejectRequest {

    /** 반려 사유 (선택). REJECTED_REASON 컬럼에 저장된다. */
    private String rejectedReason;
}
