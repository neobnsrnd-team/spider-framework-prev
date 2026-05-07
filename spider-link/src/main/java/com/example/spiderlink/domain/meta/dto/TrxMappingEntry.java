package com.example.spiderlink.domain.meta.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FWK_LISTENER_TRX_MESSAGE 1건 — 게이트웨이·커맨드 → 기관·거래 매핑 정보.
 *
 * <p>기동 시 {@code MessageEngineContext}가 전체 행을 메모리에 적재하고,
 * 거래 수신마다 DB 조회 대신 이 객체로 TRX_ID·ORG_ID를 반환한다.</p>
 */
@Data
@NoArgsConstructor
public class TrxMappingEntry {

    private String gwId;
    /** FWK_LISTENER_TRX_MESSAGE.REQ_ID_CODE — 커맨드(전문 ID) */
    private String reqIdCode;
    /** 기관 식별자 */
    private String orgId;
    /** 거래 ID — FWK_SERVICE 조회 키 */
    private String trxId;
}
