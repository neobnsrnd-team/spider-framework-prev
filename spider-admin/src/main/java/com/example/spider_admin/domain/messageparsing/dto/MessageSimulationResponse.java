package com.example.spider_admin.domain.messageparsing.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 시뮬레이션 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSimulationResponse {

    /**
     * 시뮬레이션 성공 여부
     */
    private boolean success;

    /**
     * 요청 데이터 (포맷팅된 문자열 또는 맵)
     */
    private Object request;

    /**
     * 응답 데이터 (포맷팅된 문자열 또는 맵)
     */
    private Object response;

    /**
     * 오류 메시지
     */
    private String errorMessage;

    /**
     * 오류 상세 정보
     */
    private Map<String, Object> errorDetails;
}
