package com.example.spiderlink.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.domain.messageinstance.MessageLogQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * spider-link 공통 자동 설정.
 *
 * <p>소비 모듈에 {@link JdbcTemplate} 빈이 존재할 때만 활성화되며,
 * {@link MessageLogQueue}와 {@link MessageInstanceRecorder}를 자동으로 빈으로 등록한다.</p>
 *
 * <p>{@code MessageLogQueue}는 {@link org.springframework.context.SmartLifecycle}을 구현하므로
 * Spring Context 시작 시 컨슈머 스레드가 자동 기동되고, 종료 시 큐를 드레인한 뒤 정상 종료된다.</p>
 *
 * <p>이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}에
 * 등록되어 Spring Boot 자동 설정 메커니즘으로 로드된다.</p>
 */
// JdbcTemplateAutoConfiguration 이후에 실행해야 JdbcTemplate 빈이 이미 등록된 상태에서
// @ConditionalOnBean(JdbcTemplate.class) 조건을 올바르게 평가할 수 있다
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass(JdbcTemplate.class)
@Import(SpiderLinkMessageConfig.class)
public class SpiderLinkAutoConfiguration {

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
    public MessageInstanceRecorder messageInstanceRecorder(
            MessageLogQueue queue,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown}") String appName) {
        return new MessageInstanceRecorder(queue, objectMapper, appName);
    }
}
