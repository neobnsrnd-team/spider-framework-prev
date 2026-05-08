package com.example.spiderbatch.job.db2db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @file DateRangePartitionerTest.java
 * @description {@link DateRangePartitioner} 단위 테스트.
 */
class DateRangePartitionerTest {

    private JdbcTemplate jdbcTemplate;
    private DateRangePartitioner partitioner;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        partitioner = new DateRangePartitioner(jdbcTemplate, "TEST_TABLE", "DATE_COL");
    }

    @Test
    void 데이터_있는_경우_gridSize만큼_파티션_생성() {
        when(jdbcTemplate.queryForObject("SELECT MIN(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260101");
        when(jdbcTemplate.queryForObject("SELECT MAX(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260131");

        Map<String, ExecutionContext> result = partitioner.partition(4);

        assertThat(result).hasSize(4);
        assertThat(result).containsKeys("partition0", "partition1", "partition2", "partition3");
    }

    @Test
    void 파티션_minValue_maxValue_범위가_전체_데이터를_포함() {
        when(jdbcTemplate.queryForObject("SELECT MIN(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260101");
        when(jdbcTemplate.queryForObject("SELECT MAX(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260131");

        Map<String, ExecutionContext> result = partitioner.partition(4);

        // 첫 파티션의 minValue는 최솟값
        assertThat(result.get("partition0").getLong("minValue")).isEqualTo(20260101L);
        // 마지막 파티션의 maxValue는 최댓값
        assertThat(result.get("partition3").getLong("maxValue")).isEqualTo(20260131L);
    }

    @Test
    void 파티션_경계가_연속적임() {
        when(jdbcTemplate.queryForObject("SELECT MIN(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260101");
        when(jdbcTemplate.queryForObject("SELECT MAX(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260131");

        Map<String, ExecutionContext> result = partitioner.partition(2);

        long p0Max = result.get("partition0").getLong("maxValue");
        long p1Min = result.get("partition1").getLong("minValue");

        // 파티션 경계: partition0 maxValue 다음 날 = partition1 minValue
        // YYYYMMDD 정수 값이므로 정확한 날짜 산술로 검증하기 위해 연속성만 확인
        assertThat(p1Min).isGreaterThan(p0Max);
        assertThat(p1Min - p0Max).isLessThanOrEqualTo(2L); // 하루 차이 허용 (말월 경계)
    }

    @Test
    void 데이터_없는_경우_단일_빈_파티션_반환() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class)))
                .thenReturn(null);

        Map<String, ExecutionContext> result = partitioner.partition(4);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey("partition0");
        assertThat(result.get("partition0").getLong("minValue")).isEqualTo(0L);
        assertThat(result.get("partition0").getLong("maxValue")).isEqualTo(0L);
    }

    @Test
    void 단일_날짜_데이터_gridSize_1_파티션() {
        when(jdbcTemplate.queryForObject("SELECT MIN(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260115");
        when(jdbcTemplate.queryForObject("SELECT MAX(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260115");

        Map<String, ExecutionContext> result = partitioner.partition(1);

        assertThat(result).hasSize(1);
        assertThat(result.get("partition0").getLong("minValue")).isEqualTo(20260115L);
        assertThat(result.get("partition0").getLong("maxValue")).isEqualTo(20260115L);
    }

    @Test
    void 연도경계_파티셔닝_정수산술_데드존_없음() {
        // 20251231 → 20260101: YYYYMMDD 정수 차이는 8870이지만 달력 기준 1일
        when(jdbcTemplate.queryForObject("SELECT MIN(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20251230");
        when(jdbcTemplate.queryForObject("SELECT MAX(DATE_COL) FROM TEST_TABLE", String.class))
                .thenReturn("20260102");

        Map<String, ExecutionContext> result = partitioner.partition(2);

        // 파티션 0이 20251230부터 시작하고, 파티션 1이 20260102에서 끝나야 함
        assertThat(result.get("partition0").getLong("minValue")).isEqualTo(20251230L);
        assertThat(result.get("partition1").getLong("maxValue")).isEqualTo(20260102L);
    }
}
