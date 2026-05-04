package com.example.spiderbatch.job.db2db;

import com.example.spiderbatch.job.AbstractDb2DbJob;
import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.CardUsage;
import com.example.spiderbatch.job.common.CardUsageQuery;
import com.example.spiderbatch.job.listener.BatchNotificationListener;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
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
 * <p>Oracle → Oracle 복사(아카이브) 패턴을 시연한다.
 * {@link AbstractDb2DbJob}이 제공하는 파티셔닝·병렬 처리 골격을 재사용하고,
 * 이 클래스는 POC_카드사용내역 도메인에 특화된 SQL과 RowMapper만 제공한다.</p>
 *
 * <p>Job Bean 이름 "db2db"가 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Db2DbJobConfig extends AbstractDb2DbJob<CardUsage> {

    private final DataSource dataSource;
    private final BatchNotificationListener batchNotificationListener;

    @Override
    protected String getJobName() {
        return "db2db";
    }

    /**
     * 파티션 매니저 Step 이름을 원본 camelCase(db2DbPartitionStep)로 고정.
     * 기본값 getJobName()+"PartitionStep" = "db2dbPartitionStep"(소문자)와 구분된다.
     */
    @Override
    protected String getPartitionStepName() {
        return "db2DbPartitionStep";
    }

    /** 이용일자를 첫 번째로 고정한 compound sort key — 파티션 BETWEEN 범위와 일치 */
    private static Map<String, Order> buildSortKeys() {
        return CardUsageQuery.buildSortKeys();
    }

    @Bean(name = "db2db")
    public Job db2DbJob(JobRepository jobRepository, Step db2DbPartitionStep) {
        return new JobBuilder("db2db", jobRepository)
                .validator(new BatchJobParametersValidator())
                .listener(batchNotificationListener)
                .start(db2DbPartitionStep)
                .build();
    }

    /**
     * 매니저 Step: ColumnRangePartitioner로 이용일자 범위를 분할하고 병렬 실행.
     * TaskExecutor를 @Bean으로 주입받아 Spring이 lifecycle(initialize/destroy)을 관리한다.
     */
    @Bean
    public Step db2DbPartitionStep(JobRepository jobRepository,
                                   Step db2DbWorkerStep,
                                   JdbcTemplate jdbcTemplate,
                                   TaskExecutor db2DbTaskExecutor) {
        return buildPartitionStep(jobRepository, "db2DbWorkerStep",
                new ColumnRangePartitioner(jdbcTemplate), db2DbWorkerStep, db2DbTaskExecutor);
    }

    /**
     * 파티션 병렬 실행용 스레드 풀 Bean.
     * Spring이 afterPropertiesSet()(초기화)·destroy()(graceful shutdown)를 자동 호출한다.
     */
    @Bean
    public ThreadPoolTaskExecutor db2DbTaskExecutor() {
        return buildTaskExecutor();
    }

    /**
     * 워커 Step: 파티션(이용일자 범위)에서 페이징 읽기 → 백업 테이블에 쓰기.
     */
    @Bean
    public Step db2DbWorkerStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return buildWorkerStep(jobRepository, transactionManager,
                "db2DbWorkerStep", db2DbReader(null, null), db2DbWriter());
    }

    /**
     * JdbcPagingItemReader: 파티션의 이용일자 범위에서 페이징 조회.
     * alias 없이 원본 한글 컬럼명 그대로 SELECT — JdbcPagingItemReader 내부 PagingRowMapper가
     * sort key(이용일자)를 rs.getObject("이용일자")로 추출하므로 alias하면 ORA-17006 발생.
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
                .selectClause("""
                        SELECT 이용자, 카드번호, 이용일자, 이용가맹점, 이용금액,
                               할부개월, 회차, 할부구분코드, 승인여부, 카드명, 승인시각, 결제예정일,
                               승인번호, 결제잔액, 누적결제금액, 결제상태코드, 최종결제일자
                        """)
                .fromClause("FROM POC_카드사용내역")
                .whereClause("WHERE TO_NUMBER(이용일자) BETWEEN :minValue AND :maxValue")
                // PK(이용일자+이용자+카드번호+승인시각) 순서로 compound sort key 설정 —
                // Map.of()는 순서 비보장 → CardUsageQuery.buildSortKeys()로 명시적 순서 지정
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
                .pageSize(getPageSize())
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
