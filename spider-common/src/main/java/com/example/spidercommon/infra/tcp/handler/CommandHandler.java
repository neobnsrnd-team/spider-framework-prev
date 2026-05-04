package com.example.spidercommon.infra.tcp.handler;

import com.example.spidercommon.infra.tcp.model.HasCommand;

/**
 * TCP 커맨드 핸들러 인터페이스 (전략 패턴).
 *
 * <p>{@link CommandDispatcher}가 {@code supports()}로 적합한 핸들러를 선택하여 위임한다.
 * AP 서버(spider-batch 등)가 이 인터페이스를 구현하여 커맨드별 비즈니스 로직을 제공한다.</p>
 *
 * @param <REQ> 요청 타입 — {@link HasCommand} 구현 필요
 * @param <RES> 응답 타입
 */
public interface CommandHandler<REQ extends HasCommand, RES> {

    boolean supports(String command);

    RES handle(String command, REQ request);
}
