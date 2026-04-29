package com.example.spider_admin.infra.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * TCP 커맨드 요청 공통 추상 모델.
 * 구간별 구현체(ManagementContext, JsonCommandRequest)가 이를 참고한다.
 *
 * @param <T> 페이로드 타입
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CommandRequest<T> {

    /** 실행할 커맨드 이름 (예: BATCH_EXEC, NOTICE_SYNC, PING) */
    private String command;

    /** 요청 추적용 ID (UUID) */
    private String requestId;

    /** 커맨드 페이로드 (구간별 타입 상이) */
    private T payload;
}
