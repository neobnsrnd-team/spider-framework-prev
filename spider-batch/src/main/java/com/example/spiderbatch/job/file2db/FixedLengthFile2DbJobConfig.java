package com.example.spiderbatch.job.file2db;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.FixedLengthRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.BufferedReaderFactory;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 고정 길이 파일 → Oracle DB 적재 Job 설정.
 *
 * <p>금융 거래내역 전문(61자 고정 길이)을 읽어 {@code POC_고정길이거래} 테이블에 MERGE(중복 skip)한다.</p>
 *
 * <ul>
 *   <li>헤더(HDR) 1줄 skip</li>
 *   <li>트레일러(TLR) {@link LineCallbackHandler}로 감지 후 로그만 기록</li>
 *   <li>BOM(EF BB BF) 자동 제거 — {@link BomStrippingBufferedReaderFactory}</li>
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
public class FixedLengthFile2DbJobConfig {

    /** Chunk 크기: 100건씩 읽어서 DB에 배치 INSERT */
    private static final int CHUNK_SIZE = 100;

    /** UTF-8 BOM 시퀀스 (EF BB BF) */
    private static final int BOM_BYTE_1 = 0xEF;
    private static final int BOM_BYTE_2 = 0xBB;
    private static final int BOM_BYTE_3 = 0xBF;

    /** 아카이브 루트 디렉터리 (application.yml: batch.file.archive-dir) */
    @Value("${batch.file.archive-dir}")
    private String archiveDir;

    /** 오류 파일 루트 디렉터리 (application.yml: batch.file.error-dir) */
    @Value("${batch.file.error-dir}")
    private String errorDir;

    private final DataSource dataSource;

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

        return new JobBuilder("fixedFile2db", jobRepository)
                .validator(new BatchJobParametersValidator(true))
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

    /**
     * 고정 길이 파일 읽기 → Processor → DB 적재 Step.
     */
    @Bean
    public TaskletStep fixedLengthFile2DbStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<FixedLengthRecord> fixedLengthFileReader,
            ItemProcessor<FixedLengthRecord, FixedLengthRecord> fixedLengthRecordProcessor,
            JdbcBatchItemWriter<FixedLengthRecord> fixedLengthRecordWriter) {

        return new StepBuilder("fixedLengthFile2DbStep", jobRepository)
                .<FixedLengthRecord, FixedLengthRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(fixedLengthFileReader)
                .processor(fixedLengthRecordProcessor)
                .writer(fixedLengthRecordWriter)
                // 개별 아이템 오류 시 해당 건만 건너뛰고 계속 처리
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                // 완료된 Step은 재시작 시 skip — RETRYABLE_YN='N'이면 Job에 preventRestart() 추가 권장
                // TODO: FWK_BATCH_APP.RETRYABLE_YN 값에 따라 JobBuilder.preventRestart() 연동 필요
                .allowStartIfComplete(false)
                .build();
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
     *
     * <ul>
     *   <li>파일 경로: Job Parameter {@code inputFilePath}에서 주입</li>
     *   <li>BOM(EF BB BF) 제거: {@link BomStrippingBufferedReaderFactory}</li>
     *   <li>헤더 1줄 skip: {@code linesToSkip(1)}</li>
     *   <li>트레일러 감지: "TLR"로 시작하는 줄 → 로그 기록 후 무시</li>
     *   <li>컬럼 분리: {@link FixedLengthTokenizer} + {@link Range}</li>
     * </ul>
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemReader<FixedLengthRecord> fixedLengthFileReader(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {

        // FixedLengthTokenizer: 컬럼별 Range 설정 (1-based index)
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
        // 레코드 길이 불일치 시 오류 발생 — 트레일러(TLR) 처리를 위해 strict 모드 해제
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<FixedLengthRecord> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(FixedLengthRecord.class);

        return new FlatFileItemReaderBuilder<FixedLengthRecord>()
                .name("fixedLengthFileReader")
                .resource(new FileSystemResource(inputFilePath))
                // BOM(EF BB BF) 제거: 커스텀 BufferedReaderFactory 적용
                .bufferedReaderFactory(new BomStrippingBufferedReaderFactory())
                // HDR 헤더 1줄 skip
                .linesToSkip(1)
                // TLR 트레일러: "TLR"로 시작하는 줄 감지 후 레코드 수 로그만 기록
                .skippedLinesCallback(trailerLineCallbackHandler())
                .lineTokenizer(tokenizer)
                .fieldSetMapper(fieldSetMapper)
                .build();
    }

    /**
     * 트레일러 라인 콜백 핸들러.
     * "TLR"로 시작하는 줄을 감지하여 레코드 수 정보를 로그로 기록한다.
     * 실제 데이터로 처리되지 않도록 Reader의 {@code skippedLinesCallback}에 등록.
     */
    @Bean
    public LineCallbackHandler trailerLineCallbackHandler() {
        return line -> {
            if (line.startsWith("TLR")) {
                log.info("[FixedLengthFile2Db] 트레일러 감지 — 내용: {}", line.trim());
            }
        };
    }

    // -------------------------------------------------------------------------
    // Processor
    // -------------------------------------------------------------------------

    /**
     * 레코드 전처리 Processor.
     * <ul>
     *   <li>amount 필드 앞뒤 공백 및 선두 0패딩 trim</li>
     *   <li>accountNo가 공백인 빈 레코드 skip (null 반환)</li>
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
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 판단 불가 —
     * 성공 여부를 생성자에서 고정값(true)으로 주입한다.
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FileArchiveTasklet fileArchiveSuccessTasklet(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return new FileArchiveTasklet(inputFilePath, archiveDir, errorDir, true);
    }

    /**
     * 오류 아카이브 Tasklet Bean.
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 판단 불가 —
     * 성공 여부를 생성자에서 고정값(false)으로 주입한다.
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FileArchiveTasklet fileArchiveErrorTasklet(
            @Value("#{jobParameters['inputFilePath']}") String inputFilePath) {
        return new FileArchiveTasklet(inputFilePath, archiveDir, errorDir, false);
    }

    // -------------------------------------------------------------------------
    // Inner class: BOM 제거 BufferedReaderFactory
    // -------------------------------------------------------------------------

    /**
     * UTF-8 BOM(EF BB BF)을 제거하는 {@link BufferedReaderFactory} 구현.
     *
     * <p>Spring의 FlatFileItemReader는 BOM을 자동 제거하지 않으므로
     * InputStream의 첫 3바이트를 검사하여 BOM이면 건너뛰고
     * 그렇지 않으면 스트림을 되돌려 정상적으로 읽는다.</p>
     */
    private static class BomStrippingBufferedReaderFactory implements BufferedReaderFactory {

        @Override
        public BufferedReader create(org.springframework.core.io.Resource resource, String encoding)
                throws IOException {

            InputStream rawStream = resource.getInputStream();

            // BOM 여부 확인을 위해 첫 3바이트 선행 읽기
            // read()는 3바이트 미만을 반환할 수 있으므로 readNBytes()로 정확히 읽는다 (Java 9+)
            byte[] bom = new byte[3];
            int bytesRead = rawStream.readNBytes(bom, 0, 3);

            InputStream finalStream;
            if (bytesRead == 3
                    && (bom[0] & 0xFF) == BOM_BYTE_1
                    && (bom[1] & 0xFF) == BOM_BYTE_2
                    && (bom[2] & 0xFF) == BOM_BYTE_3) {
                // BOM 확인됨 — 이미 3바이트를 소비했으므로 나머지 스트림만 사용
                finalStream = rawStream;
                log.debug("[BomStrippingBufferedReaderFactory] UTF-8 BOM 감지 및 제거");
            } else {
                // BOM 없음 — 읽은 바이트를 스트림 앞에 되돌려 전체 내용을 정상 처리
                byte[] prefix = (bytesRead > 0) ? java.util.Arrays.copyOf(bom, bytesRead) : new byte[0];
                finalStream = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(prefix), rawStream);
            }

            return new BufferedReader(new InputStreamReader(finalStream, StandardCharsets.UTF_8));
        }
    }
}
