package com.example.spiderlink.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
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
 * {@link MessageInstanceRecorder}를 자동으로 빈으로 등록한다.</p>
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
    public MessageInstanceRecorder messageInstanceRecorder(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown}") String appName) {
        return new MessageInstanceRecorder(jdbcTemplate, objectMapper, appName);
    }
}
