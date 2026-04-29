package com.example.spideradmin.domain.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배치 수동 실행 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchExecRequest {

    /**
     * 배치 APP ID
     */
    @NotBlank(message = "배치 APP ID는 필수입니다")
    private String batchAppId;

    /**
     * 배치APP 파라미터 (ex: id=홍길동;pass=1111;date=20080101)
     */
    private String parameters;

    /**
     * 기준일 (YYYYMMDD 형식)
     */
    @NotBlank(message = "기준일은 필수입니다")
    private String batchDate;

    /**
     * 실행할 WAS 인스턴스 ID 목록
     */
    @NotEmpty(message = "실행할 WAS 인스턴스를 선택하세요")
    private List<String> instanceIds;

    /**
     * 실행 요청 사용자 ID
     */
    private String userId;
}
