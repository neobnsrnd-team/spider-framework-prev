package com.example.spiderbatch.job.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.batch.item.database.Order;

/**
 * POC_카드사용내역 공통 쿼리 유틸리티.
 *
 * <p>DB2DB·DB2Foreign 두 Job이 동일한 PK 컬럼 및 정렬 순서를 공유한다.
 * 두 곳에서 중복 선언하면 컬럼 순서 불일치로 JdbcPagingItemReader의 페이지 커서가
 * 어긋날 수 있으므로 단일 출처(SSOT)로 관리한다.</p>
 */
public final class CardUsageQuery {

    private CardUsageQuery() {}

    /**
     * POC_카드사용내역 PK(이용일자 → 이용자 → 카드번호 → 승인시각) 기준 compound sort key.
     *
     * <p>JdbcPagingItemReader가 페이지 간 커서를 유지하려면 PK 전체를 sort key로 지정해야 한다.
     * 이용일자를 첫 번째로 유지해야 partition BETWEEN 범위와 next-page 조건이 충돌하지 않는다.
     * Map.of()는 순서 비보장이므로 LinkedHashMap으로 명시적 삽입 순서를 지킨다.</p>
     */
    public static Map<String, Order> buildSortKeys() {
        Map<String, Order> keys = new LinkedHashMap<>();
        keys.put("이용일자", Order.ASCENDING);
        keys.put("이용자", Order.ASCENDING);
        keys.put("카드번호", Order.ASCENDING);
        keys.put("승인시각", Order.ASCENDING);
        return keys;
    }
}
