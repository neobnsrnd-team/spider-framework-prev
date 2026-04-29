package com.example.spidercommon.infra.tcp.model;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Admin ↔ spider-link ↔ demo/backend 구간 TCP 통신 요청 모델.
 *
 * <p>4바이트 길이 프리픽스(int, big-endian) + UTF-8 JSON 바이트열로 전송된다.
 * Admin의 JsonCommandRequest와 동일한 JSON 스키마를 사용하므로
 * spider-link는 역/직렬화 없이 바이트를 그대로 프록시할 수 있다.</p>
 *
 * <p>공통 필드(command, requestId, payload)는 {@link CommandRequest}에서 상속한다.</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JsonCommandRequest extends CommandRequest<Map<String, Object>> {
    // command / requestId / payload 필드는 부모 CommandRequest에서 상속
}
