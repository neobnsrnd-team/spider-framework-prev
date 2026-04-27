package com.example.spiderbatch.job.file2db;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import com.example.spiderbatch.job.common.PocUser;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * File2DBJob 설정.
 *
 * <p>CSV 파일 → Oracle DB 적재 패턴을 시연한다.
 * FlatFileItemReader로 sample-data/poc-users.csv를 읽어 POC_USER 테이블에 UPSERT(MERGE).</p>
 *
 * <p>Job Bean 이름 "file2db"가 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다.</p>
 *
 * <pre>{@code
 * FWK_BATCH_APP: BATCH_APP_ID='FILE2DB_JOB', BATCH_APP_FILE_NAME='file2db'
 * }</pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class File2DbJobConfig {

    /** Chunk 크기: 5건씩 읽어서 DB에 배치 INSERT */
    private static final int CHUNK_SIZE = 5;

    private final DataSource dataSource;

    /**
     * File2DBJob.
     * Job 이름이 JobRegistry의 키가 되므로 FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치.
     */
    @Bean(name = "file2db")
    public Job file2DbJob(JobRepository jobRepository, Step file2DbStep) {
        return new JobBuilder("file2db", jobRepository)
                .validator(new BatchJobParametersValidator())
                .start(file2DbStep)
                .build();
    }

    @Bean
    public Step file2DbStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("file2DbStep", jobRepository)
                .<PocUser, PocUser>chunk(CHUNK_SIZE, transactionManager)
                .reader(file2DbReader())
                .processor(item -> {
                    // 사용자명 없으면 skip
                    if (item.getUserName() == null || item.getUserName().isBlank()) {
                        log.warn("사용자명 없음 — skip: userId={}", item.getUserId());
                        return null;
                    }
                    return item;
                })
                .writer(file2DbWriter())
                // skip: 개별 아이템 오류 시 해당 아이템만 건너뜀
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                // 완료된 Step은 재시작 시 skip — RETRYABLE_YN='N'이면 Job에 preventRestart() 추가 권장
                // TODO: FWK_BATCH_APP.RETRYABLE_YN 값에 따라 JobBuilder.preventRestart() 연동 필요
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * CSV FlatFileItemReader.
     * 컬럼 순서: userId, userName, password, userGrade, logYn, lastLoginDtime
     */
    @Bean
    public FlatFileItemReader<PocUser> file2DbReader() {
        BeanWrapperFieldSetMapper<PocUser> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(PocUser.class);

        return new FlatFileItemReaderBuilder<PocUser>()
                .name("file2DbReader")
                .resource(new ClassPathResource("sample-data/poc-users.csv"))
                // CSV 헤더 없음 — 컬럼 이름 직접 지정
                .delimited()
                .names("userId", "userName", "password", "userGrade", "logYn", "lastLoginDtime")
                .fieldSetMapper(fieldSetMapper)
                .build();
    }

    /**
     * POC_USER 테이블에 배치 UPSERT.
     * 동일 USER_ID가 있으면 UPDATE, 없으면 INSERT (MERGE).
     */
    @Bean
    public JdbcBatchItemWriter<PocUser> file2DbWriter() {
        return new JdbcBatchItemWriterBuilder<PocUser>()
                .dataSource(dataSource)
                .sql("""
                        MERGE INTO POC_USER t
                        USING (SELECT :userId AS USER_ID FROM DUAL) s
                        ON (t.USER_ID = s.USER_ID)
                        WHEN MATCHED THEN UPDATE SET
                            t.USER_NAME        = :userName,
                            t.PASSWORD         = :password,
                            t.USER_GRADE       = :userGrade,
                            t.LOG_YN           = :logYn,
                            t.LAST_LOGIN_DTIME = :lastLoginDtime
                        WHEN NOT MATCHED THEN INSERT
                            (USER_ID, USER_NAME, PASSWORD, USER_GRADE, LOG_YN, LAST_LOGIN_DTIME, SSN)
                        VALUES
                            (:userId, :userName, :password, :userGrade, :logYn, :lastLoginDtime, '0000000000000')
                        """)
                .beanMapped()
                .build();
    }
}
