package com.example.spideradmin.domain.message.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 수정 요청 DTO
 * PUT /api/messages/{messageId} 요청에 사용
 * orgId와 messageId는 path variable로 전달되므로 DTO에는 포함하지 않음
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageUpdateRequest {

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
