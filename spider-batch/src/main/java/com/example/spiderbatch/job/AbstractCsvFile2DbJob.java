package com.example.spiderbatch.job;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * CSV 파일 → DB 배치 Job의 추상 기반 클래스.
 *
 * <p>CSV FlatFileItemReader + faultTolerant Step 패턴의 공통 설정을 템플릿 메서드로 제공한다.
 * 내장 프로젝트의 {@code @Configuration}에서 이 클래스를 상속하여
 * 도메인 특화 Reader·Writer·Processor와 Job Bean을 정의한다.</p>
 *
 * <pre>{@code
 * @Configuration
 * @RequiredArgsConstructor
 * public class File2DbJobConfig extends AbstractCsvFile2DbJob<PocUser> {
 *
 *     private final DataSource dataSource;
 *     private final BatchNotificationListener notificationListener;
 *
 *     @Override
 *     protected String getJobName() { return "file2db"; }
 *
 *     @Bean(name = "file2db")
 *     public Job file2DbJob(JobRepository jobRepository, Step file2DbStep) {
 *         return buildJobBuilder(jobRepository)
 *                 .listener(notificationListener)
 *                 .start(file2DbStep)
 *                 .build();
 *     }
 *
 *     @Bean
 *     public Step file2DbStep(JobRepository jobRepository, PlatformTransactionManager txMgr) {
 *         return buildStep(jobRepository, txMgr, reader(), processor(), writer());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 읽기/쓰기 대상 도메인 타입
 */
public abstract class AbstractCsvFile2DbJob<T> {

    /** Job 이름. FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다. */
    protected abstract String getJobName();

    /** Chunk 크기(건당 읽기/쓰기 단위). 기본값 5. */
    protected int getChunkSize() {
        return 5;
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
     * faultTolerant chunk Step을 생성한다.
     * {@code skip(Exception.class)}로 개별 아이템 오류 시 해당 건만 건너뛴다.
     *
     * @param stepName Step 이름 (JobBuilder의 Step 참조 키)
     * @param reader   아이템 리더
     * @param processor 아이템 프로세서
     * @param writer   아이템 라이터
     */
    protected Step buildStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             String stepName,
                             ItemReader<T> reader,
                             ItemProcessor<T, T> processor,
                             ItemWriter<T> writer) {
        return new StepBuilder(stepName, jobRepository)
                .<T, T>chunk(getChunkSize(), transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(getSkipLimit())
                // 완료된 Step은 재시작 시 skip — RETRYABLE_YN='N'이면 Job에 preventRestart() 추가 권장
                .allowStartIfComplete(false)
                .build();
    }
}
