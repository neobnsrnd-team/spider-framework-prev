package com.example.spidercommon.domain.management.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.example.spidercommon.domain.loglevel.LogLevelApplier;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogLevelExecutor 테스트")
class LogLevelExecutorTest {

    @Mock
    private LogLevelApplier logLevelApplier;

    @InjectMocks
    private LogLevelExecutor logLevelExecutor;

    @Test
    @DisplayName("log_config_level gubun을 지원한다")
    void supports_log_config_level() {
        assertThat(logLevelExecutor.supports("log_config_level")).isTrue();
        assertThat(logLevelExecutor.supports("log_config_additivity")).isFalse();
        assertThat(logLevelExecutor.supports("batch_reload")).isFalse();
    }

    @Test
    @DisplayName("logName과 level을 올바르게 전달하여 applyLevel을 호출한다")
    void execute_레벨_변경_성공() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example.service");
        params.put("level", "DEBUG");

        Map<String, Object> result = logLevelExecutor.execute(params);

        verify(logLevelApplier).applyLevel("com.example.service", "DEBUG");
        assertThat(result).containsEntry("logName", "com.example.service");
        assertThat(result).containsEntry("level", "DEBUG");
    }

    @Test
    @DisplayName("level이 null이면 applyLevel에 null을 전달한다 (부모 상속)")
    void execute_level이_null이면_상속() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example.service");
        params.put("level", null);

        Map<String, Object> result = logLevelExecutor.execute(params);

        verify(logLevelApplier).applyLevel("com.example.service", null);
        assertThat(result).containsEntry("level", "inherited");
    }

    @Test
    @DisplayName("logName이 없으면 IllegalArgumentException을 던진다")
    void execute_logName이_없으면_예외() {
        Map<String, Object> params = new HashMap<>();
        params.put("level", "DEBUG");

        assertThatThrownBy(() -> logLevelExecutor.execute(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logName");
    }

    @Test
    @DisplayName("logName이 공백이면 IllegalArgumentException을 던진다")
    void execute_logName이_공백이면_예외() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "   ");

        assertThatThrownBy(() -> logLevelExecutor.execute(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logName");
    }
}
