package com.example.spideradmin.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 생성 요청 DTO
 * POST /api/messages 요청에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageCreateRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    @Size(max = 20, message = "기관 ID는 20자 이내여야 합니다")
    private String orgId;

    @NotBlank(message = "전문 ID는 필수입니다")
    @Size(max = 50, message = "전문 ID는 50자 이내여야 합니다")
    private String messageId;

    @NotBlank(message = "전문명은 필수입니다")
    @Size(max = 100, message = "전문명은 100자 이내여야 합니다")
    private String messageName;

    @Size(max = 1000, message = "전문 설명은 1000자 이내여야 합니다")
    private String messageDesc;

    @Size(max = 10, message = "전문 타입은 10자 이내여야 합니다")
    private String messageType;

    @Size(max = 50, message = "상위 전문 ID는 50자 이내여야 합니다")
    private String parentMessageId;

    @Size(max = 1, message = "헤더 여부는 1자 이내여야 합니다")
    private String headerYn;

    @Size(max = 1, message = "요청 여부는 1자 이내여야 합니다")
    private String requestYn;

    @Size(max = 10, message = "거래 타입은 10자 이내여야 합니다")
    private String trxType;

    @Size(max = 1, message = "사전 로드 여부는 1자 이내여야 합니다")
    private String preLoadYn;

    @Size(max = 10, message = "로그 레벨은 10자 이내여야 합니다")
    private String logLevel;

    @Size(max = 50, message = "비즈니스 도메인은 50자 이내여야 합니다")
    private String bizDomain;

    @Size(max = 1, message = "검증 사용 여부는 1자 이내여야 합니다")
    private String validationUseYn;

    @Size(max = 1, message = "잠금 여부는 1자 이내여야 합니다")
    private String lockYn;

    private Integer curVersion;
}
