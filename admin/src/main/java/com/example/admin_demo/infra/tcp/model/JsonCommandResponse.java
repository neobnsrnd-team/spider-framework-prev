package com.example.admin_demo.infra.tcp.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin ↔ biz-channel 간 TCP 통신 응답 모델.
 *
 * <p>JSON 직렬화 방식을 사용한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonCommandResponse {

    /** 요청과 대응되는 커맨드 이름 */
    private String command;

    /** 성공 여부 */
    private boolean success;

    /** 응답 메시지 */
    private String message;

    /** 실패 시 에러 메시지 */
    private String error;

    /** 응답 데이터 (조회 결과 등 구조화된 데이터 전달용) */
    private Map<String, Object> payload;
}
