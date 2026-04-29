package com.example.admin_demo.domain.loglevel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>LogLevel 조회 응답 DTO</h3>
 * <p>Logback LoggerContext에서 읽은 로거 정보를 담습니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogLevelResponse {

    /** 로거 이름 (패키지명 또는 클래스명) */
    private String logName;

    /** 로그 레벨 (ERROR/WARN/INFO/DEBUG/TRACE/OFF). null이면 상위 로거 레벨을 상속함 */
    private String logLevel;

    /** 실제 적용 중인 레벨. logLevel이 null일 때 부모로부터 상속받은 값을 나타냄 */
    private String effectiveLevel;

    /** 부모 로거의 effective level. 상속 선택 시 실제로 적용될 레벨을 미리 보여주기 위해 사용 */
    private String parentEffectiveLevel;

    /** Additivity 여부 (Y: 상위 로거에도 전파, N: 이 로거에서만 처리) */
    private String additivity;

    /** 연결된 Appender 구현 클래스 경로 목록 (쉼표 구분, 예: ch.qos.logback.core.ConsoleAppender) */
    private String appender;
}
