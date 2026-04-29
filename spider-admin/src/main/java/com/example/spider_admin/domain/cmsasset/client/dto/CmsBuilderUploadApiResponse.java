package com.example.spider_admin.domain.cmsasset.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS Builder /cms/api/builder/upload 응답 스키마 — Issue #65.
 *
 * <p>CMS 는 실패 시에도 HTTP 200 을 반환하며 {@code ok:false} 로 실패를 표현한다.
 * 따라서 HTTP status 가 아닌 {@code ok} 필드로 성공/실패를 판단해야 한다.
 *
 * <p>성공 시 body: {@code {"url": "/static/xxx.png", "assetId": "uuid"}}
 * (성공 응답에는 {@code ok} 필드가 없으므로 기본값 null → 아래 isSuccess 로직 참고)
 * <p>실패 시 body: {@code {"ok": false, "error": "메시지"}}
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsBuilderUploadApiResponse {

    /** 성공 여부 플래그 (실패 시에만 명시). 성공 응답에서는 null 일 수 있음 */
    private Boolean ok;

    /** 실패 메시지 (ok=false 일 때만 존재) */
    private String error;

    /** CMS 가 저장한 이미지 URL (성공 시, 예: "/static/xxx.png") */
    private String url;

    /** CMS 가 발급한 Asset UUID (성공 시) */
    private String assetId;

    /**
     * 성공 여부 판단.
     * <ul>
     *   <li>{@code ok == Boolean.FALSE} 이면 실패</li>
     *   <li>그 외에는 assetId 와 url 이 모두 존재해야 성공으로 본다</li>
     * </ul>
     */
    public boolean isSuccess() {
        if (Boolean.FALSE.equals(ok)) {
            return false;
        }
        return assetId != null && !assetId.isBlank() && url != null && !url.isBlank();
    }
}
