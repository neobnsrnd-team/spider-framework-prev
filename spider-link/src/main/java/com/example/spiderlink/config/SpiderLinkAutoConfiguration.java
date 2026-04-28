package com.example.spiderlink.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * spider-link 메시지 엔진 자동 설정.
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
 * </ul>
 */
// JdbcTemplateAutoConfiguration 이후에 실행해야 JdbcTemplate 빈이 이미 등록된 상태에서
// @ConditionalOnBean(JdbcTemplate.class) 조건을 올바르게 평가할 수 있다
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
public class SpiderLinkAutoConfiguration {

    /**
     * 전문 거래 이력 기록기 빈.
     *
     * <p>JdbcTemplate 빈이 존재하는 경우에만 생성된다.
     * {@code spring.application.name}을 ORG_ID·INSTANCE_ID에 활용한다.</p>
     *
     * @param jdbcTemplate DB 연결 (소비 모듈의 datasource 사용)
     * @param objectMapper JSON 직렬화용 ObjectMapper
     * @param appName      spring.application.name
     * @return MessageInstanceRecorder 빈
     */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public MessageInstanceRecorder messageInstanceRecorder(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown}") String appName) {
        return new MessageInstanceRecorder(jdbcTemplate, objectMapper, appName);
    }
}
