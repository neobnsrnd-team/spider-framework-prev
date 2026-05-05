package com.example.spideradmin.domain.messagetest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문테스트 화면용 필드 응답 DTO
 * 목적: 테스트 입력 화면에 필요한 최소 필드만 포함 (7개)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageFieldForTestResponse {
    private String messageId; // 메시지 ID
    private String fieldId; // 필드 ID
    private String fieldName; // 필드명
    private Long dataLength; // 길이
    private String requiredYn; // 필수여부 (Y/N)
    private String testValue; // 기본 테스트 값
    private Integer sortOrder; // 정렬 순서
    private String defaultValue; // 기본값 (default 전문 초기값)
    private Boolean isSystemKeyword; // 시스템 키워드 여부
}
