package com.example.spiderlink.infra.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * TCP 커맨드 요청 공통 추상 모델.
 *
 * <p>Admin ↔ AP 서버 구간 전체에서 동일한 JSON 스키마를 사용한다.</p>
 *
 * @param <T> 페이로드 타입
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CommandRequest<T> implements HasCommand {

    /** 실행할 커맨드 이름 (예: NOTICE_SYNC, NOTICE_END, PING) */
    private String command;

    /** 요청 추적용 ID (UUID) */
    private String requestId;

    /** 커맨드 페이로드 */
    private T payload;
}
