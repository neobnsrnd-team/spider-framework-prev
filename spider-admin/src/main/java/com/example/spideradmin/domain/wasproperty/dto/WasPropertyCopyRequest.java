package com.example.spideradmin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;

/**
 * WAS 프로퍼티 복사 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WasPropertyCopyRequest {

    /**
     * 원본 인스턴스 ID
     */
    @NotBlank(message = "원본 인스턴스 ID는 필수입니다")
    private String sourceInstanceId;

    /**
     * 대상 인스턴스 ID
     */
    @NotBlank(message = "대상 인스턴스 ID는 필수입니다")
    private String targetInstanceId;

    /**
     * 복사할 프로퍼티 그룹 목록 (null이면 전체 복사)
     */
    private List<String> propertyGroupIds;

    /**
     * 복사 사유
     */
    private String reason;

    /**
     * 덮어쓰기 여부 (기존 값이 있으면 덮어쓸지)
     */
    @Builder.Default
    private Boolean overwrite = false;
}
