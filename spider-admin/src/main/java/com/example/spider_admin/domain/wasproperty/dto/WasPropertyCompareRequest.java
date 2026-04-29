package com.example.spider_admin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;

/**
 * WAS 프로퍼티 비교 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WasPropertyCompareRequest {

    /**
     * 비교할 첫 번째 인스턴스 ID
     */
    @NotBlank(message = "첫 번째 인스턴스 ID는 필수입니다")
    private String instanceId1;

    /**
     * 비교할 두 번째 인스턴스 ID
     */
    @NotBlank(message = "두 번째 인스턴스 ID는 필수입니다")
    private String instanceId2;

    /**
     * 프로퍼티 그룹 필터 (null이면 전체 비교)
     */
    private List<String> propertyGroupIds;
}
