package com.example.spider_admin.domain.messageparsing.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 시뮬레이션 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSimulationRequest {

    /**
     * 기관 ID
     */
    @NotBlank(message = "기관 ID는 필수입니다.")
    private String orgId;

    /**
     * 거래 ID
     */
    @NotBlank(message = "거래 ID는 필수입니다.")
    private String trxId;

    /**
     * 인스턴스 ID
     */
    @NotBlank(message = "인스턴스 ID는 필수입니다.")
    private String instanceId;

    /**
     * 필드 데이터
     * Map<메시지ID, Map<필드ID, 값>>
     * 값은 String(단일) 또는 List<String>(루프 필드)
     * 예: { "로그인A": { "거래ID": "로그인", "ITEM_ID": ["val1","val2","val3"] } }
     */
    private Map<String, Map<String, Object>> fieldData;
}
