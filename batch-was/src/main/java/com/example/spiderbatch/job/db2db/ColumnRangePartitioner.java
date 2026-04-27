package com.example.spiderbatch.job.db2db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 이용일자(YYYYMMDD) 범위 기반 Partitioner.
 *
 * <p>POC_카드사용내역 테이블을 이용일자 범위로 균등 분할하여 병렬 처리한다.
 * YYYYMMDD 정수 단순 산술은 월/연도 경계에서 공백이 생기므로(예: 20251231 → 20260101 사이
 * 정수 공백 8870) {@link LocalDate} 달력 산술로 실제 날짜 기반 분할한다.</p>
 *
 * <pre>
 * 예) 이용일자 20251105~20260414, gridSize=4 이면:
 *   partition0: 20251105 ~ 20251231 (약 57일)
 *   partition1: 20260101 ~ 20260126 (약 57일)
 *   partition2: 20260127 ~ 20260220 (약 57일)
 *   partition3: 20260221 ~ 20260414 (나머지)
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ColumnRangePartitioner implements Partitioner {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    /** 파티션 분할 대상 테이블 */
    private static final String TABLE = "POC_카드사용내역";

    /**
     * gridSize 수만큼 파티션을 생성한다.
     * 각 파티션의 ExecutionContext에 minValue, maxValue(YYYYMMDD 숫자)를 저장한다.
     *
     * @param gridSize 병렬 처리할 파티션 수 (스레드 수와 동일)
     * @return 파티션 이름 → ExecutionContext 맵
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        String minStr = jdbcTemplate.queryForObject(
                "SELECT MIN(이용일자) FROM " + TABLE, String.class);
        String maxStr = jdbcTemplate.queryForObject(
                "SELECT MAX(이용일자) FROM " + TABLE, String.class);

        if (minStr == null || maxStr == null) {
            log.warn("파티셔닝 대상 데이터 없음: table={}", TABLE);
            Map<String, ExecutionContext> empty = new HashMap<>();
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minValue", 0L);
            ctx.putLong("maxValue", 0L);
            empty.put("partition0", ctx);
            return empty;
        }

        LocalDate minDate = LocalDate.parse(minStr, FMT);
        LocalDate maxDate = LocalDate.parse(maxStr, FMT);

        // 실제 달력 일수로 균등 분할 — YYYYMMDD 정수 산술은 월 경계 공백으로 데드존 발생
        long totalDays = ChronoUnit.DAYS.between(minDate, maxDate);
        long daysPerPartition = totalDays / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();
        LocalDate start = minDate;

        for (int i = 0; i < gridSize; i++) {
            LocalDate end = (i == gridSize - 1)
                    ? maxDate
                    : start.plusDays(daysPerPartition - 1);

            long minValue = Long.parseLong(start.format(FMT));
            long maxValue = Long.parseLong(end.format(FMT));

            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minValue", minValue);
            ctx.putLong("maxValue", maxValue);
            result.put("partition" + i, ctx);

            log.debug("partition{}: {}~{}", i, start.format(FMT), end.format(FMT));
            start = end.plusDays(1);
        }

        log.info("파티션 생성 완료: table={}, gridSize={}, 이용일자={}~{}", TABLE, gridSize, minStr, maxStr);
        return result;
    }
}
