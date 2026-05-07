package com.example.spiderbatch.job;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.file2db.FileArchiveTasklet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.BufferedReaderFactory;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 고정 길이 파일 → DB 배치 Job의 추상 기반 클래스.
 *
 * <p>다음 공통 인프라를 템플릿 메서드로 제공한다:
 * <ul>
 *   <li>UTF-8 BOM 자동 제거 ({@link BomStrippingBufferedReaderFactory})</li>
 *   <li>헤더(HDR) 1줄 skip + 트레일러(TLR) 감지·로그</li>
 *   <li>faultTolerant Chunk Step (skip(Exception), skipLimit)</li>
 *   <li>성공/오류 아카이브 Tasklet ({@link FileArchiveTasklet})</li>
 *   <li>배포 단위 Job 빌더 (성공 → 성공 아카이브, 실패 → 오류 아카이브 플로우)</li>
 * </ul>
 * </p>
 *
 * <pre>{@code
 * @Configuration
 * @RequiredArgsConstructor
 * public class FixedLengthFile2DbJobConfig extends AbstractFixedLengthFile2DbJob<FixedLengthRecord> {
 *
 *     @Value("${batch.file.archive-dir}") private String archiveDir;
 *     @Value("${batch.file.error-dir}")   private String errorDir;
 *     private final DataSource dataSource;
 *     private final BatchNotificationListener notificationListener;
 *
 *     @Override protected String getJobName()      { return "fixedFile2db"; }
 *     @Override protected Class<FixedLengthRecord> getTargetType() { return FixedLengthRecord.class; }
 *     @Override protected FixedLengthTokenizer buildTokenizer()    { ... }
 *
 *     @Bean(name = "fixedFile2db")
 *     public Job fixedFile2DbJob(JobRepository jobRepository,
 *             Step fixedLengthFile2DbStep, Step fileArchiveSuccessStep, Step fileArchiveErrorStep) {
 *         return buildJobBuilder(jobRepository)
 *                 .validator(new BatchJobParametersValidator(true))
 *                 .listener(notificationListener)
 *                 .start(fixedLengthFile2DbStep)
 *                     .on("COMPLETED").to(fileArchiveSuccessStep)
 *                 .from(fixedLengthFile2DbStep).on("*").to(fileArchiveErrorStep)
 *                 .end()
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * @param <T> 읽기/쓰기 대상 도메인 타입
 */
@Slf4j
public abstract class AbstractFixedLengthFile2DbJob<T> {

    /** UTF-8 BOM 시퀀스 (EF BB BF) */
    private static final int BOM_BYTE_1 = 0xEF;
    private static final int BOM_BYTE_2 = 0xBB;
    private static final int BOM_BYTE_3 = 0xBF;

    /** Job 이름. FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다. */
    protected abstract String getJobName();

    /** {@link FixedLengthTokenizer} 설정(컬럼명·범위). 서브 클래스에서 구현한다. */
    protected abstract FixedLengthTokenizer buildTokenizer();

    /** 도메인 레코드 타입. {@link BeanWrapperFieldSetMapper} 타입 지정에 사용한다. */
    protected abstract Class<T> getTargetType();

    /** Chunk 크기. 기본값 100. */
    protected int getChunkSize() {
        return 100;
    }

    /** faultTolerant skip 허용 최대 건수. 기본값 10. */
    protected int getSkipLimit() {
        return 10;
    }

    /**
     * {@link BatchJobParametersValidator}가 적용된 {@link JobBuilder}를 반환한다.
     * 서브 클래스에서 {@code .listener()}, {@code .start()} 등을 추가하여 Job을 완성한다.
     */
    protected JobBuilder buildJobBuilder(JobRepository jobRepository) {
        return new JobBuilder(getJobName(), jobRepository)
                .validator(new BatchJobParametersValidator());
    }

    /**
     * BOM 제거 + 헤더 skip + 트레일러 감지가 적용된 FlatFileItemReader를 생성한다.
     *
     * @param inputFilePath Job Parameter {@code inputFilePath}에서 주입된 파일 경로
     */
    protected FlatFileItemReader<T> buildReader(String inputFilePath) {
        FixedLengthTokenizer tokenizer = buildTokenizer();
        // 레코드 길이 불일치 시 오류 발생 — 트레일러(TLR) 처리를 위해 strict 모드 해제
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<T> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(getTargetType());

        return new FlatFileItemReaderBuilder<T>()
                .name(getJobName() + "FileReader")
                .resource(new FileSystemResource(inputFilePath))
                // BOM(EF BB BF) 제거: 커스텀 BufferedReaderFactory 적용
                .bufferedReaderFactory(new BomStrippingBufferedReaderFactory())
                // HDR 헤더 1줄 skip
                .linesToSkip(1)
                // TLR 트레일러: "TLR"로 시작하는 줄 감지 후 레코드 수 로그만 기록
                .skippedLinesCallback(line -> {
                    if (line.startsWith("TLR")) {
                        log.info("[{}] 트레일러 감지: {}", getJobName(), line.trim());
                    }
                })
                .lineTokenizer(tokenizer)
                .fieldSetMapper(fieldSetMapper)
                .build();
    }

    /**
     * faultTolerant Chunk Step을 생성한다.
     * {@code skip(Exception.class)}로 개별 아이템 오류 시 해당 건만 건너뛴다.
     *
     * @param stepName Step 이름
     */
    protected <I, O> Step buildChunkStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          String stepName,
                                          ItemReader<I> reader,
                                          ItemProcessor<I, O> processor,
                                          ItemWriter<O> writer) {
        return new StepBuilder(stepName, jobRepository)
                .<I, O>chunk(getChunkSize(), transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(getSkipLimit())
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * 성공 아카이브 {@link FileArchiveTasklet}을 생성한다.
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 성공 여부를 직접 전달한다.
     */
    protected FileArchiveTasklet buildSuccessTasklet(String inputFilePath,
                                                      String archiveDir,
                                                      String errorDir) {
        return new FileArchiveTasklet(inputFilePath, archiveDir, errorDir, true);
    }

    /**
     * 오류 아카이브 {@link FileArchiveTasklet}을 생성한다.
     * jobExecution.getStatus()는 Step 실행 중 STARTED이므로 성공 여부를 직접 전달한다.
     */
    protected FileArchiveTasklet buildErrorTasklet(String inputFilePath,
                                                    String archiveDir,
                                                    String errorDir) {
        return new FileArchiveTasklet(inputFilePath, archiveDir, errorDir, false);
    }

    /**
     * UTF-8 BOM(EF BB BF)을 제거하는 {@link BufferedReaderFactory} 구현.
     *
     * <p>Spring의 FlatFileItemReader는 BOM을 자동 제거하지 않으므로
     * InputStream의 첫 3바이트를 검사하여 BOM이면 건너뛰고
     * 그렇지 않으면 스트림을 되돌려 정상적으로 읽는다.</p>
     */
    protected static class BomStrippingBufferedReaderFactory implements BufferedReaderFactory {

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
                log.debug("[BomStripping] UTF-8 BOM 감지 및 제거");
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
