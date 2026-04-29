package com.example.spider_admin.infra.tcp.model;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Admin ↔ biz-channel 간 TCP 통신 요청 모델.
 *
 * <p>JSON 직렬화 방식을 사용한다.
 * 4바이트 길이 프리픽스(int) + UTF-8 JSON 바이트열로 전송된다.</p>
 *
 * <p>공통 요청 스키마는 {@link CommandRequest}에서 상속한다
 * (command, requestId, payload 필드).</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JsonCommandRequest extends CommandRequest<Map<String, Object>> {
    // 부모 CommandRequest<Map<String, Object>>에서 command / requestId / payload 필드를 상속받는다.
    // 구간 고유 필드가 필요해지면 이 클래스에 추가한다.
}
