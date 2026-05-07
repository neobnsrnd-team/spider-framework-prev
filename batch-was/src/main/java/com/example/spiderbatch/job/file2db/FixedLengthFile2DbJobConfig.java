package com.example.spiderbatch.job.file2db;

import com.example.spiderbatch.job.AbstractFixedLengthFile2DbJob;
import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.FixedLengthRecord;
import com.example.spiderbatch.job.listener.BatchNotificationListener;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 고정 길이 파일 → Oracle DB 적재 Job 설정.
 *
 * <p>금융 거래내역 전문(61자 고정 길이)을 읽어 {@code POC_고정길이거래} 테이블에 MERGE(중복 skip)한다.</p>
 *
 * <ul>
 *   <li>헤더(HDR) 1줄 skip, 트레일러(TLR) 감지·로그: {@link AbstractFixedLengthFile2DbJob#buildReader}</li>
 *   <li>BOM(EF BB BF) 자동 제거: {@link AbstractFixedLengthFile2DbJob.BomStrippingBufferedReaderFactory}</li>
 *   <li>faultTolerant: skip(Exception), skipLimit(10)</li>
 *   <li>처리 완료 후 파일 아카이브 Step 실행 (성공/실패 분기)</li>
 * </ul>
 *
 * <pre>{@code
 * FWK_BATCH_APP: BATCH_APP_ID='FIXED_FILE2DB_JOB', BATCH_APP_FILE_NAME='fixedFile2db'
 * }</pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FixedLengthFile2DbJobConfig extends AbstractFixedLengthFile2DbJob<FixedLengthRecord> {

    /** 아카이브 루트 디렉터리 (application.yml: batch.file.archive-dir) */
    @Value("${batch.file.archive-dir}")
    private String archiveDir;

    /** 오류 파일 루트 디렉터리 (application.yml: batch.file.error-dir) */
    @Value("${batch.file.error-dir}")
    private String errorDir;

    private final DataSource dataSource;
    private final BatchNotificationListener batchNotificationListener;

    @Override
    protected String getJobName() {
        return "fixedFile2db";
    }

    @Override
    protected Class<FixedLengthRecord> getTargetType() {
        return FixedLengthRecord.class;
    }

    /**
     * 컬럼별 Range 설정 (1-based index).
     * 레코드 포맷: ACCOUNT_NO(10) + TRX_DT(8) + TRX_TM(6) + AMOUNT(15) + TRX_TYPE_CODE(2) + MEMO(20) = 61자
     */
    @Override
    protected FixedLengthTokenizer buildTokenizer() {
        FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setNames("accountNo", "trxDt", "trxTm", "amount", "trxTypeCode", "memo");
        tokenizer.setColumns(
                new Range(1, 10),   // ACCOUNT_NO
                new Range(11, 18),  // TRX_DT
                new Range(19, 24),  // TRX_TM
                new Range(25, 39),  // AMOUNT
                new Range(40, 41),  // TRX_TYPE_CODE
                new Range(42, 61)   // MEMO
        );
        return tokenizer;
    }

    // -------------------------------------------------------------------------
    // Job
    // -------------------------------------------------------------------------

    /**
     * fixedFile2db Job.
     * Job 이름이 JobRegistry 키가 되므로 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치.
     */
    @Bean(name = "fixedFile2db")
    public Job fixedLengthFile2DbJob(
            JobRepository jobRepository,
            Step fixedLengthFile2DbStep,
            Step fileArchiveSuccessStep,
            Step fileArchiveErrorStep) {

        return buildJobBuilder(jobRepository)
                // inputFilePath 파라미터 필수 검증
                .validator(new BatchJobParametersValidator(true))
                .listener(batchNotificationListener)
                .start(fixedLengthFile2DbStep)
                    // 데이터 적재 Step 성공 시 → 성공 아카이브
                    .on("COMPLETED").to(fileArchiveSuccessStep)
                // 그 외 상태(FAILED 등) → 오류 아카이브
                .from(fixedLengthFile2DbStep).on("*").to(fileArchiveErrorStep)
                .end()
                .build();
    }

    // -------------------------------------------------------------------------
    // Steps
    // -------------------------------------------------------------------------

    /** 고정 길이 파일 읽기 → Processor → DB 적재 Step */
    @Bean
    public TaskletStep fixedLengthFile2DbStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<FixedLengthRecord> fixedLengthFileReader,
            ItemProcessor<FixedLengthRecord, FixedLengthRecord> fixedLengthRecordProcessor,
            JdbcBatchItemWriter<FixedLengthRecord> fixedLengthRecordWriter) {

        return (TaskletStep) buildChunkStep(jobRepository, transactionManager,
                "fixedLengthFile2DbStep",
                fixedLengthFileReader, fixedLengthRecordProcessor, fixedLengthRecordWriter);
    }

    /** 성공 아카이브 Step */
    @Bean
    public TaskletStep fileArchiveSuccessStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FileArchiveTasklet fileArchiveSuccessTasklet) {

        return new StepBuilder("fileArchiveSuccessStep", jobRepository)
                .tasklet(fileArchiveSuccessTasklet, transactionManager)
                .build();
    }

    /** 오류 아카이브 Step */
    @Bean
    public TaskletStep fileArchiveErrorStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FileArchiveTasklet fileArchiveErrorTasklet) {

        return new StepBuilder("fileArchiveErrorStep", jobRepository)
                .tasklet(fileArchiveErrorTasklet, transactionManager)
                .build();
    }

    // -------------------------------------------------------------------------
    // Reader
    // -------------------------------------------------------------------------

    /**
     * 고정 길이 전문 FlatFileItemReader.
     * BOM 제거·헤더 skip·트레일러 감지는 {@link AbstractFixedLengthFile2DbJob#buildReader}가 처리한다.
     *
     * @param inputFilePath Job Parameter {@code inputFilePath}에서 주입
     */
    @Bean
    @StepScope
    public FlatFileItemReader<FixedLengthRecord> fixedLengthFileReader(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return buildReader(inputFilePath);
    }

    // -------------------------------------------------------------------------
    // Processor
    // -------------------------------------------------------------------------

    /**
     * 레코드 전처리 Processor.
     * <ul>
     *   <li>accountNo가 공백인 빈 레코드 skip (null 반환)</li>
     *   <li>트레일러(TLR) 레코드 skip</li>
     *   <li>amount 필드 앞뒤 공백 및 선두 0패딩 trim</li>
     *   <li>memo 필드 우측 공백패딩 제거</li>
     * </ul>
     */
    @Bean
    public ItemProcessor<FixedLengthRecord, FixedLengthRecord> fixedLengthRecordProcessor() {
        return record -> {
            // accountNo가 공백이면 빈 레코드로 판단하여 skip
            if (record.getAccountNo() == null || record.getAccountNo().isBlank()) {
                log.warn("[FixedLengthFile2Db] 빈 레코드 skip — accountNo 공백");
                return null;
            }
            // skippedLinesCallback은 linesToSkip 대상(헤더)에만 동작하므로,
            // 파일 끝 TLR 트레일러는 Processor에서 직접 필터링한다
            if (record.getAccountNo().trim().startsWith("TLR")) {
                log.info("[FixedLengthFile2Db] 트레일러 레코드 skip — accountNo: {}",
                        record.getAccountNo().trim());
                return null;
            }

            // amount 필드: 선두 0패딩 포함 공백 trim (DB에는 순수 숫자 문자열로 저장)
            if (record.getAmount() != null) {
                record.setAmount(record.getAmount().trim());
            }

            // memo 필드: 우측 공백패딩 제거
            if (record.getMemo() != null) {
                record.setMemo(record.getMemo().trim());
            }

            return record;
        };
    }

    // -------------------------------------------------------------------------
    // Writer
    // -------------------------------------------------------------------------

    /**
     * POC_고정길이거래 테이블에 MERGE INSERT.
     * PK(ACCOUNT_NO, TRX_DT, TRX_TM) 중복 시 INSERT를 건너뜀 (assertUpdates=false).
     */
    @Bean
    public JdbcBatchItemWriter<FixedLengthRecord> fixedLengthRecordWriter() {
        return new JdbcBatchItemWriterBuilder<FixedLengthRecord>()
                .dataSource(dataSource)
                .sql("""
                        MERGE INTO POC_고정길이거래 t
                        USING (SELECT :accountNo AS ACCOUNT_NO,
                                      :trxDt     AS TRX_DT,
                                      :trxTm     AS TRX_TM
                               FROM DUAL) s
                        ON (t.ACCOUNT_NO = s.ACCOUNT_NO
                            AND t.TRX_DT = s.TRX_DT
                            AND t.TRX_TM = s.TRX_TM)
                        WHEN NOT MATCHED THEN
                            INSERT (ACCOUNT_NO, TRX_DT, TRX_TM, AMOUNT, TRX_TYPE_CODE, MEMO)
                            VALUES (:accountNo, :trxDt, :trxTm, :amount, :trxTypeCode, :memo)
                        """)
                .beanMapped()
                // MERGE의 NOT MATCHED가 0건이어도 예외 발생하지 않도록 false 설정
                .assertUpdates(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // Tasklet Bean
    // -------------------------------------------------------------------------

    /**
     * 성공 아카이브 Tasklet Bean.
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 성공 여부를 생성자에서 고정값(true)으로 주입.
     */
    @Bean
    @StepScope
    public FileArchiveTasklet fileArchiveSuccessTasklet(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return buildSuccessTasklet(inputFilePath, archiveDir, errorDir);
    }

    /**
     * 오류 아카이브 Tasklet Bean.
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 성공 여부를 생성자에서 고정값(false)으로 주입.
     */
    @Bean
    @StepScope
    public FileArchiveTasklet fileArchiveErrorTasklet(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return buildErrorTasklet(inputFilePath, archiveDir, errorDir);
    }
}
