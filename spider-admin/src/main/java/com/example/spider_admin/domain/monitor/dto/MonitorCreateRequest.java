package com.example.spider_admin.domain.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Monitor 생성 요청 DTO</h3>
 * <p>모니터 현황판 생성 시 사용되는 DTO</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitorCreateRequest {

    /**
     * 모니터 ID (PK)
     */
    @NotBlank(message = "모니터 ID는 필수입니다")
    @Size(max = 20, message = "모니터 ID는 20자 이내여야 합니다")
    private String monitorId;

    /**
     * 모니터명
     */
    @NotBlank(message = "모니터명은 필수입니다")
    @Size(max = 50, message = "모니터명은 50자 이내여야 합니다")
    private String monitorName;

    /**
     * 모니터 쿼리 (목록 조회용 SQL)
     */
    @Size(max = 2000, message = "모니터 쿼리는 2000자 이내여야 합니다")
    private String monitorQuery;

    /**
     * 경고 조건
     */
    @Size(max = 100, message = "경고 조건은 100자 이내여야 합니다")
    private String alertCondition;

    /**
     * 경고 메시지
     */
    @Size(max = 100, message = "경고 메시지는 100자 이내여야 합니다")
    private String alertMessage;

    /**
     * 새로고침 주기 (분 단위 문자열)
     */
    @NotBlank(message = "새로고침 주기는 필수입니다")
    @Pattern(regexp = "^(1|5|10|30|60|180|360)$", message = "유효한 새로고침 주기를 선택하세요 (1, 5, 10, 30, 60, 180, 360)")
    private String refreshTerm;

    /**
     * 상세 쿼리 (상세 조회용 SQL)
     */
    @Size(max = 2000, message = "상세 쿼리는 2000자 이내여야 합니다")
    private String detailQuery;

    /**
     * 사용 여부 (Y/N)
     */
    @Size(max = 1, message = "사용 여부는 1자여야 합니다")
    private String useYn;
}
