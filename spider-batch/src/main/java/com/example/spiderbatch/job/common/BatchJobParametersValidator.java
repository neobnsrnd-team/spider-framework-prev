package com.example.spiderbatch.job.common;

import java.io.File;
import java.util.regex.Pattern;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

/**
 * 배치 Job 공통 JobParameters 유효성 검증기.
 *
 * <p>모든 Job에 공통으로 적용되는 필수 파라미터(batchAppId, batchDate)를 검증하고,
 * 파일 처리 Job의 경우 inputFilePath 존재 여부까지 추가로 검증한다.</p>
 *
 * <p>Spring Bean이 아닌 일반 클래스로 구현되며, Job 빌더에서 {@code new}로 생성한다.</p>
 *
 * <pre>{@code
 * // 기본 사용 (파일 경로 검증 없음)
 * new JobBuilder("myJob", jobRepository)
 *     .validator(new BatchJobParametersValidator())
 *     ...
 *
 * // 파일 처리 Job (inputFilePath 존재 여부까지 검증)
 * new JobBuilder("myFileJob", jobRepository)
 *     .validator(new BatchJobParametersValidator(true))
 *     ...
 * }</pre>
 */
public class BatchJobParametersValidator implements JobParametersValidator {

    /** batchDate 파라미터 형식 검증용 패턴 (YYYYMMDD 8자리 숫자) */
    private static final Pattern BATCH_DATE_PATTERN = Pattern.compile("^\\d{8}$");

    /**
     * 파일 처리 Job 여부.
     * true이면 inputFilePath 파라미터의 존재 및 파일 가독성까지 추가 검증한다.
     */
    private final boolean requireInputFilePath;

    /**
     * 파일 경로 검증 없이 공통 파라미터(batchAppId, batchDate)만 검증하는 기본 생성자.
     */
    public BatchJobParametersValidator() {
        this(false);
    }

    /**
     * 파일 처리 Job 여부를 지정하는 생성자.
     *
     * @param requireInputFilePath true이면 inputFilePath 존재 및 읽기 가능 여부까지 검증
     */
    public BatchJobParametersValidator(boolean requireInputFilePath) {
        this.requireInputFilePath = requireInputFilePath;
    }

    /**
     * JobParameters 유효성을 검증한다.
     *
     * <p>검증 순서:
     * <ol>
     *   <li>batchAppId: null 또는 빈 문자열이면 예외</li>
     *   <li>batchDate: null 또는 YYYYMMDD 8자리 숫자 패턴 불일치 시 예외</li>
     *   <li>requireInputFilePath == true인 경우: inputFilePath 미존재 또는 파일 읽기 불가 시 예외</li>
     * </ol>
     * </p>
     *
     * @param parameters 검증할 JobParameters
     * @throws JobParametersInvalidException 필수 파라미터 누락 또는 형식 불일치 시
     */
    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        // 1. batchAppId 검증: 모든 Job의 필수 파라미터
        String batchAppId = parameters.getString("batchAppId");
        if (batchAppId == null || batchAppId.isBlank()) {
            throw new JobParametersInvalidException("batchAppId는 필수입니다");
        }

        // 2. batchDate 검증: YYYYMMDD 8자리 숫자 형식이어야 함
        String batchDate = parameters.getString("batchDate");
        if (batchDate == null || !BATCH_DATE_PATTERN.matcher(batchDate).matches()) {
            throw new JobParametersInvalidException(
                    "batchDate는 YYYYMMDD 8자리 숫자여야 합니다: " + batchDate);
        }

        // 3. inputFilePath 검증: 파일 처리 Job에서만 수행
        if (requireInputFilePath) {
            String inputFilePath = parameters.getString("inputFilePath");

            // 3-1. inputFilePath 파라미터 자체가 없으면 예외
            if (inputFilePath == null || inputFilePath.isBlank()) {
                throw new JobParametersInvalidException(
                        "inputFilePath는 파일 처리 Job의 필수 파라미터입니다");
            }

            // 3-2. 파일이 실제로 존재하고 읽을 수 있는지 확인
            File file = new File(inputFilePath);
            if (!file.exists() || !file.canRead()) {
                throw new JobParametersInvalidException(
                        "inputFilePath 파일을 찾을 수 없거나 읽을 수 없습니다: " + inputFilePath);
            }
        }
    }
}
