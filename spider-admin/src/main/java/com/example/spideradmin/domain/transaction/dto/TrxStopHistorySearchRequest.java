package com.example.spideradmin.domain.transaction.dto;

import lombok.*;

/**
 * 거래중지이력 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxStopHistorySearchRequest {

    /**
     * 구분유형 ("": 전체, "T": 거래, "S": 서비스)
     */
    private String gubunType;

    /**
     * 거래ID
     */
    private String trxId;

    /**
     * 시작일시 (yyyyMMddHHmmss)
     */
    private String startDtime;

    /**
     * 종료일시 (yyyyMMddHHmmss)
     */
    private String endDtime;
}
