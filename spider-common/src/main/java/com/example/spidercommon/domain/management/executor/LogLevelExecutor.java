package com.example.spidercommon.domain.management.executor;

import com.example.spidercommon.domain.loglevel.LogLevelApplier;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그 레벨 변경 관리 실행기.
 *
 * <p>{@code gubun = "log_config_level"} 명령을 처리한다.
 * {@link LogLevelApplier}에 실제 Logback 조작을 위임한다.</p>
 *
 * <p>spider-common Auto-Configuration으로 자동 등록되므로
 * 각 WAS에서 별도 설정 없이 TCP 관리 명령으로 로그 레벨을 변경할 수 있다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LogLevelExecutor implements ManagementExecutor {

    static final String GUBUN = "log_config_level";

    private final LogLevelApplier logLevelApplier;

    @Override
    public boolean supports(String gubun) {
        return GUBUN.equals(gubun);
    }

    /**
     * @param params {@code logName} (필수), {@code level} (선택 — 없으면 부모 상속)
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String logName = (String) params.get("logName");
        String level = (String) params.get("level");

        if (logName == null || logName.isBlank()) {
            throw new IllegalArgumentException("logName은 필수입니다");
        }

        logLevelApplier.applyLevel(logName, level);
        log.info("[LogLevelExecutor] 레벨 변경 완료: logName={}, level={}", logName, level);
        return Map.of("logName", logName, "level", level != null ? level : "inherited");
    }
}
