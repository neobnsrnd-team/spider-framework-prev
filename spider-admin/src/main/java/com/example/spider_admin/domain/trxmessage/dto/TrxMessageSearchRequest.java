package com.example.spider_admin.domain.trxmessage.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching TrxMessage with filters
 * GET /api/trx/messages/page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxMessageSearchRequest {

    private String searchField;
    private String searchValue;

    // 필터 조건
    private String orgIdFilter;
    private String ioTypeFilter;
    private String trxIdFilter;

    /**
     * 허용된 기관 ID 목록 (전문 테스트용)
     * <p>null이면 필터링하지 않음, 값이 있으면 해당 기관만 조회</p>
     */
    private List<String> allowedOrgIds;

    /**
     * 거래별 중복 제거 여부 (전문 테스트용)
     * <p>true일 경우 orgId+trxId별로 IO_TYPE 우선순위(O>I>Q>S)에 따라 하나만 선택</p>
     */
    private boolean deduplicate;
}
