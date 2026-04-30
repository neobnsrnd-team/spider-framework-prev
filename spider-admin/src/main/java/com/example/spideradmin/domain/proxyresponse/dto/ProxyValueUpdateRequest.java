package com.example.spideradmin.domain.proxyresponse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대응답 값(PROXY_VALUE) 업데이트 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyValueUpdateRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    @NotBlank(message = "거래 ID는 필수입니다")
    private String trxId;

    @NotNull(message = "테스트 일련번호는 필수입니다")
    private Long testSno;

    private String proxyValue;

    /**
     * 테스트 그룹 ID (기본값: "DEFAULT")
     */
    @Builder.Default
    private String testGroupId = "DEFAULT";

    /**
     * 검색 조건 - 등록자 (선택)
     */
    private String userId;

    /**
     * 검색 조건 - 테스트명 (선택)
     */
    private String testName;
}
