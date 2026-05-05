package com.example.spideradmin.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CMS 승인 요청 모달에서 사용하는 결재자 목록 응답 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsApproverResponse {

    private String userId;
    private String userName;
}
