package com.example.spider_admin.domain.transaction.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 트랜잭션 이력 Command Mapper
 * - 이력 생성 및 버전 관리
 */
public interface TrxHistoryMapper {

    /**
     * 다음 버전 번호 조회 (현재 최대 버전 + 1)
     * @param trxId 트랜잭션 ID
     * @return 다음 버전 번호 (이력이 없으면 1)
     */
    Integer getNextVersion(@Param("trxId") String trxId);

    /**
     * FWK_TRX에서 직접 이력 레코드 생성 (INSERT ... SELECT)
     * @param trxId 트랜잭션 ID
     * @param version 버전 번호
     * @param historyReason 이력 생성 사유
     */
    void insertHistoryFromTrx(
            @Param("trxId") String trxId,
            @Param("version") Integer version,
            @Param("historyReason") String historyReason);
}
