package com.example.spider_admin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trx 목록 조회용 응답 DTO
 * FWKI0060 거래관리 화면 리스트용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxListResponse {

    // 1. 거래ID
    private String trxId;

    // 2. 거래명
    private String trxName;

    // 3. 기관명
    private String orgId;
    private String orgName;

    // 4. 기동/수동 (retryTrxYn: Y=자동, N=수동)
    private String autoManualType;

    // 5. 요청/응답 전문명 (클릭 시 전문관리 모달)
    private String reqMessageId;
    private String reqMessageName;
    private String resMessageId;
    private String resMessageName;

    // 6. 기본값 설정전문 (클릭 시 전문관리 모달)
    private String stdReqMessageId;
    private String stdReqMessageName;
    private String stdResMessageId;
    private String stdResMessageName;

    // 7. 현재 상태 (trxStopYn: Y=중지, N=정상)
    private String trxStopYn;

    // 8. 대응답 관리 (클릭 시 대응답 관리 모달) - proxyResYn
    private String proxyResYn;

    // 9. 다수 응답 (multiResYn: Y/N)
    private String multiResYn;
}
