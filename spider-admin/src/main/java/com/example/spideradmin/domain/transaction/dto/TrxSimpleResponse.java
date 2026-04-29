package com.example.spideradmin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 간단한 TRX 정보 DTO
 * select box용 목록 조회에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxSimpleResponse {

    private String trxId;
    private String trxName;
    private String trxStopYn;
}
