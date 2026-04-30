package com.example.spideradmin.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 표준전문조회복사 검색 결과 Row DTO.
 * FWK_TRX_MESSAGE + FWK_TRX + FWK_MESSAGE를 JOIN하여 거래-전문 매핑 정보를 반환합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StdMessageSearchResponse {

    private String trxId;
    private String trxName;
    private String orgId;
    private String messageId;
    private String ioType;
    private String messageName;
}
