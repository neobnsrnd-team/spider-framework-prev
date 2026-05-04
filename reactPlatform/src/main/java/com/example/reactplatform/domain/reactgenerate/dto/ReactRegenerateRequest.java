package com.example.reactplatform.domain.reactgenerate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file ReactRegenerateRequest.java
 * @description React 코드 재생성 API 요청 DTO.
 *     기존 생성 이력(refCodeId)을 기반으로 변경 요청사항만 받아 코드를 재생성한다.
 *     figmaUrl·brand·domain 등은 원본 레코드에서 재사용하므로 전달하지 않는다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactRegenerateRequest {

    /**
     * 변경 요청사항. Claude에게 전달하며, 기존 코드와 함께 프롬프트에 포함된다.
     * 예) "버튼 색상을 primary로 변경", "입력 필드 아래 에러 메시지 추가"
     */
    @NotBlank(message = "변경 요청사항을 입력해주세요.")
    @Size(max = 4000, message = "변경 요청사항은 4000자 이내로 입력해주세요.")
    private String requirements;
}
