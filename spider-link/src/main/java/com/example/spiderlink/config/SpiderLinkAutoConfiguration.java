package com.example.spiderlink.config;

import com.example.spiderlink.domain.management.executor.MessageStructureExecutor;
import com.example.spiderlink.domain.management.executor.RequestAppMappingExecutor;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.domain.messageinstance.MessageLogQueue;
import com.example.spiderlink.infra.tcp.client.pool.SocketPoolRegistry;
import com.example.spiderlink.infra.tcp.handler.MetaDrivenServiceOrchestrator;
import com.example.spiderlink.infra.tcp.parser.MessageStructureCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

/**
 * spider-link 공통 자동 설정.
 *
 * <p>소비 모듈에 {@link JdbcTemplate} 빈이 존재할 때만 활성화되며,
 * {@link SocketPoolRegistry}, {@link MessageLogQueue}, {@link MessageInstanceRecorder}를
 * 자동으로 빈으로 등록한다.</p>
 *
 * <p>{@code MessageLogQueue}와 {@link SocketPoolRegistry}는
 * {@link org.springframework.context.SmartLifecycle}을 구현하므로
 * Spring Context 시작 시 자동 기동되고, 종료 시 정상 종료된다.</p>
 *
 * <p>이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}에
 * 등록되어 Spring Boot 자동 설정 메커니즘으로 로드되며, spider-link를 의존하는 모든 WAS에 자동 적용된다.</p>
 *
 * <p>관리/인프라 빈(LogLevelApplier, ManagementReloadCommandHandler 등)은
 * spider-common의 {@code SpiderCommonAutoConfiguration}에서 등록된다.</p>
 *
 * <p>등록되는 빈 목록:</p>
 * <ul>
 *   <li>{@link MessageInstanceRecorder} — JdbcTemplate이 존재하는 경우에만 등록</li>
 *   <li>{@link RequestAppMappingExecutor} — MetaDrivenServiceOrchestrator가 존재하는 경우에만 등록</li>
 *   <li>{@link MessageStructureExecutor} — 항상 등록 (MessageStructureCache 없으면 supports=false)</li>
 * </ul>
 */
// JdbcTemplateAutoConfiguration 이후에 실행해야 JdbcTemplate 빈이 이미 등록된 상태에서
// @ConditionalOnBean(JdbcTemplate.class) 조건을 올바르게 평가할 수 있다
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass(JdbcTemplate.class)
@Import(SpiderLinkMessageConfig.class)
public class SpiderLinkAutoConfiguration {

    /**
     * 소켓 커넥션 풀 레지스트리 빈.
     *
     * <p>(host:port) 단위로 소켓을 재사용하여 TCP 연결 오버헤드를 줄인다.
     * SmartLifecycle 구현체로 Context 종료 시 모든 유휴 소켓이 자동 닫힌다.</p>
     *
     * @return SocketPoolRegistry 빈
     */
    @Bean
    public SocketPoolRegistry socketPoolRegistry() {
        return new SocketPoolRegistry();
    }

    /**
     * 전문 이력 비동기 큐 빈.
     *
     * <p>JdbcTemplate 빈이 존재하는 경우에만 생성된다.
     * SmartLifecycle 구현체로 Context 시작·종료 시 컨슈머 스레드가 자동 관리된다.</p>
     *
     * @param jdbcTemplate DB 연결 (소비 모듈의 datasource 사용)
     * @return MessageLogQueue 빈
     */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public MessageLogQueue messageLogQueue(JdbcTemplate jdbcTemplate) {
        return new MessageLogQueue(jdbcTemplate);
    }

    /**
     * 전문 거래 이력 기록기 빈.
     *
     * <p>JdbcTemplate 빈이 존재하는 경우에만 생성된다.
     * {@link MessageLogQueue}에 이력을 적재하고 실제 INSERT는 큐 컨슈머가 비동기로 처리한다.
     * {@code spring.application.name}을 ORG_ID·INSTANCE_ID에 활용한다.</p>
     *
     * @param queue        비동기 기록 큐
     * @param objectMapper JSON 직렬화용 ObjectMapper
     * @param appName      spring.application.name
     * @return MessageInstanceRecorder 빈
     */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public MessageInstanceRecorder messageInstanceRecorder(
            MessageLogQueue queue,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown}") String appName) {
        return new MessageInstanceRecorder(queue, objectMapper, appName);
    }

    /**
     * FWK_LISTENER_TRX_MESSAGE 커맨드 캐시 갱신 실행기.
     *
     * <p>MetaDrivenServiceOrchestrator 빈이 등록된 WAS에서만 활성화된다.
     * ManagementReloadHttpController(spider-common)가 이 실행기를 자동으로 주입받아
     * {@code gubun=request_app_mapping} 명령을 처리한다.</p>
     */
    @Bean
    @ConditionalOnBean(MetaDrivenServiceOrchestrator.class)
    public RequestAppMappingExecutor requestAppMappingExecutor(MetaDrivenServiceOrchestrator handler) {
        return new RequestAppMappingExecutor(handler);
    }

    /**
     * FWK_MESSAGE 전문 구조 캐시 초기화 실행기.
     *
     * <p>MessageStructureCache가 없으면(고정길이 전문 미사용) supports()가 false를 반환하여
     * ManagementReloadHttpController에서 자동으로 건너뛴다.</p>
     */
    @Bean
    public MessageStructureExecutor messageStructureExecutor(
            @Nullable MessageStructureCache messageStructureCache) {
        return new MessageStructureExecutor(messageStructureCache);
    }
}
