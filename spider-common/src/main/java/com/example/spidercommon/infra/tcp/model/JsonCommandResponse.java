package com.example.spidercommon.infra.tcp.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin ↔ AP 서버 구간 TCP 통신 응답 모델.
 *
 * <p>4바이트 길이 프리픽스(int, big-endian) + UTF-8 JSON 바이트열로 전송된다.</p>
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

    /** 커맨드별 응답 데이터 */
    private Map<String, Object> payload;
}
