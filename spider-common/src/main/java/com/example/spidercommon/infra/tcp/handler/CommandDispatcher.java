package com.example.spidercommon.infra.tcp.handler;

import com.example.spidercommon.infra.tcp.model.HasCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 커맨드 디스패처 (전략 패턴).
 *
 * <p>주입된 {@link CommandHandler} 목록을 순회하여
 * 커맨드를 처리할 수 있는 핸들러에 위임한다.</p>
 *
 * <p>Spring Bean이 아닌 일반 클래스이므로, AP 서버의 {@code @Configuration}에서
 * 핸들러 목록을 직접 주입하여 인스턴스를 생성한다.</p>
 *
 * @param <REQ> 요청 타입 — {@link HasCommand} 구현 필요
 * @param <RES> 응답 타입
 */
@Slf4j
@RequiredArgsConstructor
public class CommandDispatcher<REQ extends HasCommand, RES> {

    private final List<CommandHandler<REQ, RES>> handlers;

    /**
     * 요청에서 커맨드를 추출하여 적합한 핸들러에 위임한다.
     *
     * @param request 요청 객체
     * @return 핸들러의 처리 결과
     * @throws IllegalArgumentException 지원하지 않는 커맨드인 경우
     */
    public RES dispatch(REQ request) {
        String command = request.getCommand();
        log.info("[CommandDispatcher] dispatch: command={}", command);
        return handlers.stream()
                .filter(h -> h.supports(command))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 커맨드: " + command))
                .handle(command, request);
    }
}
