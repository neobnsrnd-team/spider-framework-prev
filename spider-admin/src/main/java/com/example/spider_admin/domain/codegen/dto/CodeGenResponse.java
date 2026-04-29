package com.example.spider_admin.domain.codegen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 Layout 기반 소스 코드 자동 생성 응답 DTO.
 *
 * <p>FWK_MESSAGE + FWK_MESSAGE_FIELD 정보를 바탕으로 생성된 Java DTO 소스 코드를 담아 반환한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeGenResponse {

    /** 전문 ID */
    private String messageId;

    /** 전문명 */
    private String messageName;

    /** 생성된 Java DTO 소스 코드 */
    private String dtoCode;
}
