package com.example.spider_admin.domain.proxyresponse.dto;

import com.example.spider_admin.domain.messagefield.dto.FieldValueRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 당발 대응답 테스트 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataCreateRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    @NotBlank(message = "거래 ID는 필수입니다")
    private String trxId;

    @NotBlank(message = "전문 ID는 필수입니다")
    private String messageId;

    @NotBlank(message = "테스트명은 필수입니다")
    @Size(max = 100, message = "테스트명은 100자 이내여야 합니다")
    private String testName;

    @NotBlank(message = "테스트설명은 필수입니다")
    @Size(max = 200, message = "테스트설명은 200자 이내여야 합니다")
    private String testDesc;

    private String testData;

    private String testGroupId;

    /**
     * 필드별 입력값 리스트 (서버 검증용)
     */
    private List<FieldValueRequest> fieldValues;
}
