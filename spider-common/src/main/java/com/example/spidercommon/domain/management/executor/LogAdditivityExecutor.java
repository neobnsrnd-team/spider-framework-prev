package com.example.spidercommon.domain.management.executor;

import com.example.spidercommon.domain.loglevel.LogLevelApplier;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로거 Additivity 변경 관리 실행기.
 *
 * <p>{@code gubun = "log_config_additivity"} 명령을 처리한다.
 * {@link LogLevelApplier}에 실제 Logback 조작을 위임한다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LogAdditivityExecutor implements ManagementExecutor {

    static final String GUBUN = "log_config_additivity";

    private final LogLevelApplier logLevelApplier;

    @Override
    public boolean supports(String gubun) {
        return GUBUN.equals(gubun);
    }

    /**
     * @param params {@code logName} (필수), {@code additivity} — "Y" 또는 "N" (필수)
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String logName = (String) params.get("logName");
        String additivity = (String) params.get("additivity");

        if (logName == null || logName.isBlank()) {
            throw new IllegalArgumentException("logName은 필수입니다");
        }
        if (!"Y".equals(additivity) && !"N".equals(additivity)) {
            throw new IllegalArgumentException("additivity는 Y 또는 N이어야 합니다");
        }

        logLevelApplier.applyAdditivity(logName, additivity);
        log.info("[LogAdditivityExecutor] Additivity 변경 완료: logName={}, additivity={}", logName, additivity);
        return Map.of("logName", logName, "additivity", additivity);
    }
}
