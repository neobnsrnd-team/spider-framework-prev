package com.example.spidercommon.domain.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

/**
 * Logback 로거 레벨·Additivity 런타임 변경 공통 로직.
 *
 * <p>HTTP({@link LogLevelReloadController})와 TCP({@link com.example.spidercommon.domain.management.executor.LogLevelExecutor})
 * 양쪽에서 공유한다. 어느 WAS JVM에서 실행되든 해당 JVM의 {@link LoggerContext}를 직접 조작한다.</p>
 */
@Slf4j
public class LogLevelApplier {

    /**
     * 로거 레벨을 변경한다.
     *
     * @param logName  변경 대상 로거 이름
     * @param levelStr 변경할 레벨 (ERROR/WARN/INFO/DEBUG/TRACE/OFF). null 또는 빈 문자열이면 부모 레벨 상속
     * @throws IllegalArgumentException 유효하지 않은 레벨 문자열인 경우
     */
    public void applyLevel(String logName, String levelStr) {
        Logger logger = getOrCreateLogger(logName);

        if (levelStr == null || levelStr.isBlank()) {
            logger.setLevel(null);
            log.info("[LogLevelApplier] 레벨 변경: {} → 상속 (null)", logName);
            return;
        }

        // Level.toLevel(str, null): null 반환 시 유효하지 않은 레벨
        Level level = Level.toLevel(levelStr, null);
        if (level == null) {
            throw new IllegalArgumentException("유효하지 않은 로그 레벨: " + levelStr);
        }
        logger.setLevel(level);
        log.info("[LogLevelApplier] 레벨 변경: {} → {}", logName, levelStr);
    }

    /**
     * 로거 Additivity를 변경한다.
     *
     * @param logName    변경 대상 로거 이름
     * @param additivity "Y"이면 true, 그 외 false
     */
    public void applyAdditivity(String logName, String additivity) {
        Logger logger = getOrCreateLogger(logName);
        logger.setAdditive("Y".equals(additivity));
        log.info("[LogLevelApplier] Additivity 변경: {} → {}", logName, additivity);
    }

    /**
     * LoggerContext에서 기존 로거를 찾거나, 없으면 새로 생성하여 반환한다.
     *
     * @param logName 로거 이름
     * @return Logback Logger 인스턴스
     */
    private Logger getOrCreateLogger(String logName) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        // ctx.exists()는 이미 생성된 로거만 반환 → 없으면 getLogger()로 새로 생성
        Logger existing = ctx.exists(logName);
        if (existing != null) {
            return existing;
        }
        return (Logger) LoggerFactory.getLogger(logName);
    }
}
