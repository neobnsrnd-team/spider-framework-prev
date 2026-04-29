package com.example.admin_demo.domain.loglevel.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.example.admin_demo.domain.loglevel.dto.AdditivityUpdateRequest;
import com.example.admin_demo.domain.loglevel.dto.LogLevelResponse;
import com.example.admin_demo.domain.loglevel.dto.LogLevelUpdateRequest;
import com.example.admin_demo.global.exception.InvalidInputException;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <h3>LogLevel 서비스</h3>
 * <p>Logback {@link LoggerContext}를 직접 조작하여 로거 목록 조회 및 레벨·Additivity 변경을 처리합니다.</p>
 * <p>변경사항은 런타임에만 적용되며, 애플리케이션 재시작 시 logback-spring.xml 기준으로 초기화됩니다.</p>
 */
@Slf4j
@Service
public class LogLevelService {

    /**
     * 전체 로거 목록을 Logback LoggerContext에서 조회합니다.
     *
     * @return 로거 이름·레벨·Additivity·Appender 목록
     */
    public List<LogLevelResponse> findAll() {
        LoggerContext ctx = getLoggerContext();
        return ctx.getLoggerList().stream().map(this::toResponse).toList();
    }

    /**
     * 특정 로거의 로그 레벨을 변경합니다.
     *
     * @param request 로거 이름 + 변경할 레벨
     */
    public void updateLevel(LogLevelUpdateRequest request) {
        Logger logger = getOrCreateLogger(request.getLogName());
        if (request.getLevel() == null) {
            // null 설정 = 명시적 레벨 제거 → 부모 로거 레벨 상속
            logger.setLevel(null);
            log.info("로그 레벨 변경: {} → 상속 (null)", request.getLogName());
            return;
        }
        // Level.toLevel(str, null): @Pattern으로 1차 검증 후 서비스 레이어에서 방어적 재검증
        Level level = Level.toLevel(request.getLevel(), null);
        if (level == null) {
            throw new InvalidInputException("유효하지 않은 로그 레벨: " + request.getLevel());
        }
        logger.setLevel(level);
        log.info("로그 레벨 변경: {} → {}", request.getLogName(), request.getLevel());
    }

    /**
     * 특정 로거의 Additivity를 변경합니다.
     *
     * @param request 로거 이름 + Y/N
     */
    public void updateAdditivity(AdditivityUpdateRequest request) {
        Logger logger = getOrCreateLogger(request.getLogName());
        logger.setAdditive("Y".equals(request.getAdditivity()));
        log.info("Additivity 변경: {} → {}", request.getLogName(), request.getAdditivity());
    }

    /**
     * LoggerContext에서 기존 로거를 찾거나, 없으면 Logback이 새로 생성하여 반환합니다.
     *
     * @param logName 로거 이름
     * @return Logback Logger 인스턴스
     */
    private Logger getOrCreateLogger(String logName) {
        LoggerContext ctx = getLoggerContext();
        Logger existing = ctx.exists(logName);
        // ctx.exists()는 이미 생성된 로거만 반환 → 없으면 getLogger()로 Logback에 생성 요청
        if (existing != null) {
            return existing;
        }
        return (Logger) LoggerFactory.getLogger(logName);
    }

    private LogLevelResponse toResponse(Logger logger) {
        return LogLevelResponse.builder()
                .logName(logger.getName())
                .logLevel(logger.getLevel() != null ? logger.getLevel().toString() : null)
                .effectiveLevel(logger.getEffectiveLevel().toString())
                .parentEffectiveLevel(resolveParentEffectiveLevel(logger))
                .additivity(logger.isAdditive() ? "Y" : "N")
                .appender(collectAppenderNames(logger))
                .build();
    }

    /**
     * 부모 로거의 effective level을 반환합니다.
     * ROOT는 부모가 없으므로 null을 반환합니다.
     */
    private String resolveParentEffectiveLevel(Logger logger) {
        if (Logger.ROOT_LOGGER_NAME.equals(logger.getName())) {
            return null;
        }
        return logger.getParent().getEffectiveLevel().toString();
    }

    private String collectAppenderNames(Logger logger) {
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        StringJoiner joiner = new StringJoiner(", ");
        while (it.hasNext()) {
            joiner.add(it.next().getClass().getName());
        }
        return joiner.toString();
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }
}
