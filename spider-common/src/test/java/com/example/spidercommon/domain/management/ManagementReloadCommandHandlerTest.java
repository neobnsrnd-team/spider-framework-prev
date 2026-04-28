package com.example.spidercommon.domain.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManagementReloadCommandHandler 테스트")
class ManagementReloadCommandHandlerTest {

    @Mock
    private ManagementExecutor levelExecutor;

    @Mock
    private ManagementExecutor additivityExecutor;

    // ─── supports ────────────────────────────────────────────────────

    @Test
    @DisplayName("MANAGEMENT_RELOAD 커맨드를 지원한다")
    void supports_MANAGEMENT_RELOAD() {
        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        assertThat(handler.supports("MANAGEMENT_RELOAD")).isTrue();
        assertThat(handler.supports("OTHER_COMMAND")).isFalse();
    }

    // ─── 정상 분기 ────────────────────────────────────────────────────

    @Test
    @DisplayName("gubun에 맞는 executor가 선택되고 다른 executor는 호출되지 않는다")
    void gubun에_맞는_executor를_선택한다() {
        given(levelExecutor.supports("log_config_level")).willReturn(true);
        given(levelExecutor.execute(any())).willReturn(Map.of("logName", "com.example", "level", "DEBUG"));

        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        Map<String, Object> payload = new HashMap<>();
        payload.put("gubun", "log_config_level");
        payload.put("logName", "com.example");
        payload.put("level", "DEBUG");

        JsonCommandRequest request =
                JsonCommandRequest.builder().command("MANAGEMENT_RELOAD").payload(payload).build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCommand()).isEqualTo("MANAGEMENT_RELOAD");
        verify(levelExecutor).execute(any());
        verify(additivityExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("additivity executor가 gubun에 맞으면 호출된다")
    void additivity_executor가_선택된다() {
        given(additivityExecutor.supports("log_config_additivity")).willReturn(true);
        given(additivityExecutor.execute(any())).willReturn(Map.of("logName", "com.example", "additivity", "N"));

        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        Map<String, Object> payload = new HashMap<>();
        payload.put("gubun", "log_config_additivity");
        payload.put("logName", "com.example");
        payload.put("additivity", "N");

        JsonCommandRequest request =
                JsonCommandRequest.builder().command("MANAGEMENT_RELOAD").payload(payload).build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isTrue();
        verify(additivityExecutor).execute(any());
        verify(levelExecutor, never()).execute(any());
    }

    // ─── 에러 분기 ────────────────────────────────────────────────────

    @Test
    @DisplayName("payload가 null이면 success=false이고 error에 'payload'가 포함된다")
    void payload가_null이면_오류_응답() {
        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        JsonCommandRequest request =
                JsonCommandRequest.builder().command("MANAGEMENT_RELOAD").payload(null).build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).containsIgnoringCase("payload");
        verify(levelExecutor, never()).execute(any());
        verify(additivityExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("gubun이 없으면 success=false이고 error에 'gubun'이 포함된다")
    void gubun이_없으면_오류_응답() {
        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        // gubun 키 없음
        JsonCommandRequest request = JsonCommandRequest.builder()
                .command("MANAGEMENT_RELOAD")
                .payload(new HashMap<>())
                .build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).containsIgnoringCase("gubun");
    }

    @Test
    @DisplayName("지원하지 않는 gubun이면 success=false이고 error에 gubun 값이 포함된다")
    void 지원하지_않는_gubun이면_오류_응답() {
        given(levelExecutor.supports("unknown_gubun")).willReturn(false);
        given(additivityExecutor.supports("unknown_gubun")).willReturn(false);

        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        Map<String, Object> payload = new HashMap<>();
        payload.put("gubun", "unknown_gubun");

        JsonCommandRequest request =
                JsonCommandRequest.builder().command("MANAGEMENT_RELOAD").payload(payload).build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("unknown_gubun");
    }

    @Test
    @DisplayName("executor에서 IllegalArgumentException이 발생하면 success=false이고 error에 메시지가 포함된다")
    void executor_IllegalArgumentException이면_오류_응답() {
        given(levelExecutor.supports("log_config_level")).willReturn(true);
        given(levelExecutor.execute(any())).willThrow(new IllegalArgumentException("logName은 필수입니다"));

        ManagementReloadCommandHandler handler =
                new ManagementReloadCommandHandler(List.of(levelExecutor, additivityExecutor));

        Map<String, Object> payload = new HashMap<>();
        payload.put("gubun", "log_config_level");

        JsonCommandRequest request =
                JsonCommandRequest.builder().command("MANAGEMENT_RELOAD").payload(payload).build();

        JsonCommandResponse response = handler.handle("MANAGEMENT_RELOAD", request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).contains("logName은 필수입니다");
    }
}
