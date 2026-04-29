package com.example.spider_admin.infra.tcp.handler;

import com.example.spider_admin.infra.tcp.model.JsonCommandRequest;

/**
 * TCP 커맨드 핸들러 인터페이스 (전략 패턴).
 *
 * <p>각 구현체는 특정 커맨드를 처리하는 책임을 가진다.
 * CommandDispatcher가 supports()로 적합한 핸들러를 선택하여 위임한다.</p>
 */
public interface CommandHandler {

    /**
     * 이 핸들러가 해당 커맨드를 처리할 수 있는지 판단한다.
     *
     * @param command 커맨드 이름
     * @return 처리 가능 여부
     */
    boolean supports(String command);

    /**
     * 커맨드를 처리하고 결과를 반환한다.
     *
     * @param command 실행할 커맨드
     * @param request 요청 객체
     * @return 처리 결과
     */
    Object handle(String command, JsonCommandRequest request);
}
