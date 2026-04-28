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
@DisplayName("LogAdditivityExecutor 테스트")
class LogAdditivityExecutorTest {

    @Mock
    private LogLevelApplier logLevelApplier;

    @InjectMocks
    private LogAdditivityExecutor logAdditivityExecutor;

    @Test
    @DisplayName("log_config_additivity gubun을 지원한다")
    void supports_log_config_additivity() {
        assertThat(logAdditivityExecutor.supports("log_config_additivity")).isTrue();
        assertThat(logAdditivityExecutor.supports("log_config_level")).isFalse();
        assertThat(logAdditivityExecutor.supports("batch_reload")).isFalse();
    }

    @Test
    @DisplayName("logName과 additivity=Y를 올바르게 전달하여 applyAdditivity를 호출한다")
    void execute_additivity_Y_성공() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example.service");
        params.put("additivity", "Y");

        Map<String, Object> result = logAdditivityExecutor.execute(params);

        verify(logLevelApplier).applyAdditivity("com.example.service", "Y");
        assertThat(result).containsEntry("logName", "com.example.service");
        assertThat(result).containsEntry("additivity", "Y");
    }

    @Test
    @DisplayName("additivity=N을 올바르게 전달하여 applyAdditivity를 호출한다")
    void execute_additivity_N_성공() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example");
        params.put("additivity", "N");

        Map<String, Object> result = logAdditivityExecutor.execute(params);

        verify(logLevelApplier).applyAdditivity("com.example", "N");
        assertThat(result).containsEntry("additivity", "N");
    }

    @Test
    @DisplayName("logName이 없으면 IllegalArgumentException을 던진다")
    void execute_logName이_없으면_예외() {
        Map<String, Object> params = new HashMap<>();
        params.put("additivity", "Y");

        assertThatThrownBy(() -> logAdditivityExecutor.execute(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logName");
    }

    @Test
    @DisplayName("additivity가 Y/N 외의 값이면 IllegalArgumentException을 던진다")
    void execute_additivity가_Y_N_외이면_예외() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example");
        params.put("additivity", "X");

        assertThatThrownBy(() -> logAdditivityExecutor.execute(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("additivity");
    }

    @Test
    @DisplayName("additivity가 null이면 IllegalArgumentException을 던진다")
    void execute_additivity가_null이면_예외() {
        Map<String, Object> params = new HashMap<>();
        params.put("logName", "com.example");
        params.put("additivity", null);

        assertThatThrownBy(() -> logAdditivityExecutor.execute(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("additivity");
    }
}
