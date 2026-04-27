package com.example.spiderbatch.job.db2foreign;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.CardUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * DB2ForeignJob 설정.
 *
 * <p>DB → 외부 시스템 HTTP 전문 연계 패턴을 시연한다.
 * JdbcPagingItemReader로 POC_카드사용내역을 페이징 조회한 후,
 * TransferItemWriter가 각 건을 Mock 외부 엔드포인트(POST /mock/external/transfer)로 전송한다.</p>
 *
 * <p>Job Bean 이름 "db2foreign"이 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Db2ForeignJobConfig {

    private static final int PAGE_SIZE = 5;

    private final DataSource dataSource;

    /** PK 전체를 이용일자 우선으로 고정한 compound sort key */
    private static Map<String, Order> buildSortKeys() {
        Map<String, Order> keys = new LinkedHashMap<>();
        keys.put("이용일자", Order.ASCENDING);
        keys.put("이용자", Order.ASCENDING);
        keys.put("카드번호", Order.ASCENDING);
        keys.put("승인시각", Order.ASCENDING);
        return keys;
    }

    /** WAS 포트를 application.yml에서 읽어 Mock URL 구성에 사용 */
    @Value("${server.port:8081}")
    private int serverPort;

    @Bean(name = "db2foreign")
    public Job db2ForeignJob(JobRepository jobRepository, Step db2ForeignStep) {
        return new JobBuilder("db2foreign", jobRepository)
                .validator(new BatchJobParametersValidator())
                .start(db2ForeignStep)
                .build();
    }

    @Bean
    public Step db2ForeignStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("db2ForeignStep", jobRepository)
                .<CardUsage, CardUsage>chunk(PAGE_SIZE, transactionManager)
                .reader(db2ForeignReader())
                .writer(transferItemWriter())
                .faultTolerant()
                // ExternalTransferException만 skip — 그 외 RuntimeException은 즉시 Step 실패
                // RuntimeException.class를 skip 대상으로 두면 NullPointer 등 프로그래밍 오류도 삼킴
                .skip(ExternalTransferException.class)
                .noSkip(RuntimeException.class)
                .skipLimit(5)
                // 완료된 Step은 재시작 시 skip — RETRYABLE_YN='N'이면 Job에 preventRestart() 추가 권장
                // TODO: FWK_BATCH_APP.RETRYABLE_YN 값에 따라 JobBuilder.preventRestart() 연동 필요
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * JdbcPagingItemReader: POC_카드사용내역 전체를 pageSize 단위로 페이징 조회.
     * alias 없이 원본 한글 컬럼명 SELECT — JdbcPagingItemReader 내부 PagingRowMapper가
     * sort key(이용일자)를 rs.getObject("이용일자")로 추출하므로 alias하면 ORA-17006 발생.
     */
    @Bean
    public JdbcPagingItemReader<CardUsage> db2ForeignReader() {
        return new JdbcPagingItemReaderBuilder<CardUsage>()
                .name("db2ForeignReader")
                .dataSource(dataSource)
                .selectClause("""
                        SELECT 이용자, 카드번호, 이용일자, 이용가맹점, 이용금액,
                               할부개월, 승인여부, 카드명, 승인시각, 결제예정일,
                               승인번호, 결제잔액, 누적결제금액, 결제상태코드, 최종결제일자
                        """)
                .fromClause("FROM POC_카드사용내역")
                // PK 전체를 compound sort key로 명시적 순서 지정 (Map.of는 순서 비보장)
                .sortKeys(buildSortKeys())
                .rowMapper((rs, rowNum) -> CardUsage.builder()
                        .userId(rs.getString("이용자"))
                        .cardNo(rs.getString("카드번호"))
                        .usageDt(rs.getString("이용일자"))
                        .merchant(rs.getString("이용가맹점"))
                        .amount(rs.getObject("이용금액") != null ? rs.getLong("이용금액") : null)
                        .installmentMonths(rs.getObject("할부개월") != null ? rs.getInt("할부개월") : null)
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
                .pageSize(PAGE_SIZE)
                .build();
    }

    /**
     * 외부 전문 연계 Writer.
     * Mock URL: http://localhost:{port}/mock/external/transfer
     * 실제 운영 시 외부 기관 URL로 교체.
     */
    @Bean
    public TransferItemWriter transferItemWriter() {
        String mockUrl = "http://localhost:" + serverPort + "/mock/external/transfer";
        // 타임아웃 미설정 시 외부 API 무응답으로 WAS 스레드가 무한 점유될 수 있음
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);   // 연결 타임아웃 5초
        factory.setReadTimeout(30_000);     // 읽기 타임아웃 30초
        log.info("DB2Foreign Mock URL: {}", mockUrl);
        return new TransferItemWriter(new RestTemplate(factory), mockUrl);
    }
}
