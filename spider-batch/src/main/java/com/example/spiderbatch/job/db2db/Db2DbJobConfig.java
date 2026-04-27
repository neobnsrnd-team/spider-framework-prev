package com.example.spiderbatch.job.db2db;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.CardUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DB2DBJob 설정.
 *
 * <p>Oracle → Oracle 복사(아카이브) 패턴을 시연한다. 대용량 처리를 위해:
 * <ul>
 *   <li>JdbcPagingItemReader: pageSize 단위로 페이징 조회</li>
 *   <li>ColumnRangePartitioner: 이용일자(YYYYMMDD) 범위로 분할하여 병렬 처리</li>
 *   <li>TaskExecutorPartitionHandler: 멀티스레드로 파티션 병렬 실행</li>
 * </ul>
 * </p>
 *
 * <p>POC_카드사용내역 → POC_카드사용내역_백업 으로 아카이브한다.</p>
 *
 * <p>Job Bean 이름 "db2db"가 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Db2DbJobConfig {

    /** 페이지당 읽을 건수 */
    private static final int PAGE_SIZE = 5;

    /** 병렬 처리할 파티션(스레드) 수 */
    private static final int GRID_SIZE = 4;

    private final DataSource dataSource;

    /** 이용일자를 첫 번째로 고정한 compound sort key — 파티션 BETWEEN 범위와 일치 */
    private static Map<String, Order> buildSortKeys() {
        Map<String, Order> keys = new LinkedHashMap<>();
        keys.put("이용일자", Order.ASCENDING);
        keys.put("이용자", Order.ASCENDING);
        keys.put("카드번호", Order.ASCENDING);
        keys.put("승인시각", Order.ASCENDING);
        return keys;
    }

    @Bean(name = "db2db")
    public Job db2DbJob(JobRepository jobRepository, Step db2DbPartitionStep) {
        return new JobBuilder("db2db", jobRepository)
                .validator(new BatchJobParametersValidator())
                .start(db2DbPartitionStep)
                .build();
    }

    /**
     * 매니저 Step: Partitioner로 이용일자 범위를 분할하고 PartitionHandler로 병렬 실행.
     */
    @Bean
    public Step db2DbPartitionStep(JobRepository jobRepository,
                                   Step db2DbWorkerStep,
                                   JdbcTemplate jdbcTemplate) {
        return new StepBuilder("db2DbPartitionStep", jobRepository)
                .partitioner("db2DbWorkerStep", new ColumnRangePartitioner(jdbcTemplate))
                .partitionHandler(db2DbPartitionHandler(db2DbWorkerStep))
                // TODO: FWK_BATCH_APP.RETRYABLE_YN 값에 따라 JobBuilder.preventRestart() 연동 필요
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * TaskExecutorPartitionHandler: 각 파티션을 별도 스레드에서 병렬 실행.
     */
    @Bean
    public PartitionHandler db2DbPartitionHandler(Step db2DbWorkerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(db2DbWorkerStep);
        handler.setTaskExecutor(db2DbTaskExecutor());
        handler.setGridSize(GRID_SIZE);
        return handler;
    }

    /** 파티션 병렬 실행용 스레드 풀. */
    @Bean
    public TaskExecutor db2DbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(GRID_SIZE);
        executor.setMaxPoolSize(GRID_SIZE);
        executor.setThreadNamePrefix("db2db-partition-");
        executor.initialize();
        return executor;
    }

    /**
     * 워커 Step: 파티션(이용일자 minValue~maxValue 범위)에서 페이징 읽기 → 백업 테이블에 쓰기.
     */
    @Bean
    public Step db2DbWorkerStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("db2DbWorkerStep", jobRepository)
                .<CardUsage, CardUsage>chunk(PAGE_SIZE, transactionManager)
                .reader(db2DbReader(null, null))   // @StepScope로 런타임 주입
                .writer(db2DbWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * JdbcPagingItemReader: 파티션의 이용일자 범위에서 페이징 조회.
     * 한글 컬럼명을 영문 alias로 매핑하여 CardUsage Bean과 연결.
     *
     * @param minValue 파티션 시작 이용일자(숫자, ColumnRangePartitioner 주입)
     * @param maxValue 파티션 종료 이용일자(숫자)
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<CardUsage> db2DbReader(
            @Value("#{stepExecutionContext['minValue']}") Long minValue,
            @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

        log.debug("db2DbReader 생성: 이용일자 {}~{}", minValue, maxValue);

        return new JdbcPagingItemReaderBuilder<CardUsage>()
                .name("db2DbReader")
                .dataSource(dataSource)
                // alias 없이 원본 한글 컬럼명 그대로 SELECT —
                // JdbcPagingItemReader 내부 PagingRowMapper가 sort key(이용일자)를
                // rs.getObject("이용일자")로 추출하므로 alias하면 ORA-17006 발생
                .selectClause("""
                        SELECT 이용자, 카드번호, 이용일자, 이용가맹점, 이용금액,
                               할부개월, 회차, 할부구분코드, 승인여부, 카드명, 승인시각, 결제예정일,
                               승인번호, 결제잔액, 누적결제금액, 결제상태코드, 최종결제일자
                        """)
                .fromClause("FROM POC_카드사용내역")
                .whereClause("WHERE TO_NUMBER(이용일자) BETWEEN :minValue AND :maxValue")
                // PK(이용일자+이용자+카드번호+승인시각) 순서로 compound sort key 설정 —
                // Map.of()는 순서 비보장 → LinkedHashMap.put()으로 명시적 순서 지정
                // 이용일자를 첫 번째로 유지해야 partition BETWEEN 범위와 next-page 조건이 충돌하지 않음
                .sortKeys(buildSortKeys())
                .rowMapper((rs, rowNum) -> CardUsage.builder()
                        .userId(rs.getString("이용자"))
                        .cardNo(rs.getString("카드번호"))
                        .usageDt(rs.getString("이용일자"))
                        .merchant(rs.getString("이용가맹점"))
                        .amount(rs.getObject("이용금액") != null ? rs.getLong("이용금액") : null)
                        .installmentMonths(rs.getObject("할부개월") != null ? rs.getInt("할부개월") : null)
                        .installmentRound(rs.getObject("회차") != null ? rs.getInt("회차") : null)
                        .installmentTypeCode(rs.getString("할부구분코드"))
                        .approvalYn(rs.getString("승인여부"))
                        .cardName(rs.getString("카드명"))
                        .approvalTime(rs.getString("승인시각"))
                        .paymentDueDate(rs.getString("결제예정일"))
                        .approvalNo(rs.getString("승인번호"))
                        .paymentBalance(rs.getObject("결제잔액") != null ? rs.getLong("결제잔액") : null)
                        .cumulativeAmount(rs.getObject("누적결제금액") != null ? rs.getLong("누적결제금액") : null)
                        .paymentStatusCode(rs.getString("결제상태코드"))
                        .lastPaymentDt(rs.getString("최종결제일자"))
                        .build())
                .parameterValues(Map.of(
                        "minValue", minValue != null ? minValue : 0L,
                        "maxValue", maxValue != null ? maxValue : 99991231L))
                .pageSize(PAGE_SIZE)
                .build();
    }

    /**
     * POC_카드사용내역_백업 테이블에 배치 UPSERT.
     * PK(이용자, 카드번호, 이용일자, 승인시각) 기준으로 MERGE.
     */
    @Bean
    public JdbcBatchItemWriter<CardUsage> db2DbWriter() {
        return new JdbcBatchItemWriterBuilder<CardUsage>()
                .dataSource(dataSource)
                .sql("""
                        MERGE INTO POC_카드사용내역_백업 t
                        USING (SELECT :userId    AS 이용자,
                                      :cardNo    AS 카드번호,
                                      :usageDt   AS 이용일자,
                                      :approvalTime AS 승인시각
                               FROM DUAL) s
                        ON (t.이용자   = s.이용자
                        AND t.카드번호 = s.카드번호
                        AND t.이용일자 = s.이용일자
                        AND t.승인시각 = s.승인시각)
                        WHEN MATCHED THEN UPDATE SET
                            t.이용가맹점   = :merchant,
                            t.이용금액     = :amount,
                            t.할부개월     = :installmentMonths,
                            t.회차         = :installmentRound,
                            t.할부구분코드 = :installmentTypeCode,
                            t.승인여부     = :approvalYn,
                            t.카드명       = :cardName,
                            t.결제예정일   = :paymentDueDate,
                            t.승인번호     = :approvalNo,
                            t.결제잔액     = :paymentBalance,
                            t.누적결제금액 = :cumulativeAmount,
                            t.결제상태코드 = :paymentStatusCode,
                            t.최종결제일자 = :lastPaymentDt
                        WHEN NOT MATCHED THEN INSERT (
                            이용자, 카드번호, 이용일자, 이용가맹점, 이용금액, 할부개월,
                            회차, 할부구분코드, 승인여부, 카드명, 승인시각, 결제예정일, 승인번호,
                            결제잔액, 누적결제금액, 결제상태코드, 최종결제일자
                        ) VALUES (
                            :userId, :cardNo, :usageDt, :merchant, :amount, :installmentMonths,
                            :installmentRound, :installmentTypeCode, :approvalYn, :cardName, :approvalTime,
                            :paymentDueDate, :approvalNo, :paymentBalance, :cumulativeAmount,
                            :paymentStatusCode, :lastPaymentDt
                        )
                        """)
                .beanMapped()
                // MERGE INTO는 기존 행 UPDATE 시 1, 신규 INSERT 시 0을 반환하므로
                // assertUpdates(true, 기본값)면 INSERT 행에 대해 예외 발생 → false로 억제
                .assertUpdates(false)
                .build();
    }
}
