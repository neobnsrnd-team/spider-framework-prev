package com.example.spideradmin.domain.worklist.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/**
 * FWK_SETTLEMENT 테이블 MyBatis Mapper.
 *
 * <p>결재요청 생성 시 사용되는 결재 레코드 INSERT 및 APPROVAL_ID 채번 쿼리를 제공한다.
 */
@Mapper
public interface FwkSettlementMapper {

    /**
     * APPROVAL_ID 채번.
     *
     * <p>오늘 날짜(YYYYMMDD)로 시작하는 최대값 + 1. 오늘 데이터가 없으면 YYYYMMDD101.
     */
    String generateApprovalId();

    /** FWK_SETTLEMENT 결재 레코드 INSERT. */
    int insert(Map<String, Object> params);
}
