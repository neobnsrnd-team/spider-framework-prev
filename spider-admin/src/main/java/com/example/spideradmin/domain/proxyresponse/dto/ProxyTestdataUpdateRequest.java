package com.example.spideradmin.domain.proxyresponse.dto;

import com.example.spideradmin.domain.messagefield.dto.FieldValueRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 당발 대응답 테스트 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataUpdateRequest {

    private Long testSno;

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
