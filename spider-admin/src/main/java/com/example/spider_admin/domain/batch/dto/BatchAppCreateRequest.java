package com.example.spider_admin.domain.batch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배치 App 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchAppCreateRequest {

    @NotBlank(message = "배치 APP ID는 필수입니다")
    @Size(max = 50, message = "배치 APP ID는 50자 이내여야 합니다")
    private String batchAppId;

    @Size(max = 50, message = "배치 APP명은 50자 이내여야 합니다")
    private String batchAppName;

    @NotBlank(message = "배치 APP FILE명은 필수입니다")
    @Size(max = 200, message = "배치 APP FILE명은 200자 이내여야 합니다")
    private String batchAppFileName;

    @Size(max = 200, message = "배치 APP 설명은 200자 이내여야 합니다")
    private String batchAppDesc;

    @Size(max = 50, message = "선행 배치 APP ID는 50자 이내여야 합니다")
    private String preBatchAppId;

    @Size(max = 1, message = "배치 주기는 1자여야 합니다")
    private String batchCycle;

    @Size(max = 20, message = "CRON 표현식은 20자 이내여야 합니다")
    private String cronText;

    @Size(max = 1, message = "재시도 가능 여부는 1자여야 합니다")
    private String retryableYn;

    @Size(max = 1, message = "다중 WAS 실행 여부는 1자여야 합니다")
    private String perWasYn;

    @Size(max = 1, message = "중요도는 1자여야 합니다")
    private String importantType;

    @Size(max = 500, message = "파라미터는 500자 이내여야 합니다")
    private String properties;

    @Size(max = 20, message = "트랜잭션 ID는 20자 이내여야 합니다")
    private String trxId;

    @Size(max = 10, message = "기관 ID는 10자 이내여야 합니다")
    private String orgId;

    @Size(max = 1, message = "입출력 구분은 1자여야 합니다")
    private String ioType;

    @Min(value = 1, message = "SLA 허용 시간은 1초 이상이어야 합니다")
    private Integer slaSeconds;

    // WAS 인스턴스 ID 목록 (생성 시 할당)
    private List<String> instanceIds;
}
