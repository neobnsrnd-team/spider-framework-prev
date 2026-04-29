package com.example.spider_admin.domain.cmsapproval.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS 공개 상태 변경 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CmsPublicStateRequest {

    /** 공개 여부 (IS_PUBLIC: Y / N) */
    private String isPublic;
}
