package com.example.spider_admin.infra.tcp.event;

import com.example.spider_admin.infra.tcp.model.CommandRequest;
import com.example.spider_admin.infra.tcp.model.CommandResponse;

/**
 * TCP 커맨드 이벤트 발행 인터페이스.
 *
 * <p>Kafka 기반 비동기 이벤트 처리를 위한 확장 포인트.
 * 실제 구현은 이슈 #92(Kafka 비동기 이벤트 처리)에서 추가된다.</p>
 *
 * <p>현재는 인터페이스만 정의하며 구현체가 없으므로
 * CommandDispatcher에서 Optional로 주입받아 사용한다.</p>
 */
public interface TcpEventPublisher {

    /**
     * TCP 커맨드 수신 이벤트를 발행한다.
     *
     * @param request 수신된 커맨드 요청
     */
    void publishCommandReceived(CommandRequest<?> request);

    /**
     * TCP 커맨드 처리 완료 이벤트를 발행한다.
     *
     * @param request  처리된 커맨드 요청
     * @param response 처리 결과 응답
     */
    void publishCommandProcessed(CommandRequest<?> request, CommandResponse<?> response);
}
