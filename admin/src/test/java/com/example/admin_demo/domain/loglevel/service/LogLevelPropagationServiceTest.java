package com.example.admin_demo.domain.loglevel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.admin_demo.domain.loglevel.dto.LogLevelPropagateRequest;
import com.example.admin_demo.domain.reload.dto.ReloadExecuteRequest;
import com.example.admin_demo.domain.reload.dto.ReloadResultResponse;
import com.example.admin_demo.domain.reload.service.ReloadService;
import com.example.admin_demo.global.exception.InvalidInputException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogLevelPropagationService 테스트")
class LogLevelPropagationServiceTest {

    @Mock
    private ReloadService reloadService;

    @InjectMocks
    private LogLevelPropagationService logLevelPropagationService;

    // ─── 레벨 Reload ──────────────────────────────────────────────────

    @Test
    @DisplayName("레벨 Reload — ReloadService에 올바른 파라미터가 전달된다")
    void 레벨_Reload_성공() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-auth"))
                .gubun("log_config_level")
                .logName("com.example.service")
                .level("DEBUG")
                .build();

        ReloadResultResponse mockResponse = ReloadResultResponse.builder()
                .reloadType("log_config_level")
                .results(List.of())
                .build();
        given(reloadService.executeReload(any())).willReturn(mockResponse);

        ReloadResultResponse result = logLevelPropagationService.propagate(request);

        ArgumentCaptor<ReloadExecuteRequest> captor = ArgumentCaptor.forClass(ReloadExecuteRequest.class);
        verify(reloadService).executeReload(captor.capture());

        ReloadExecuteRequest captured = captor.getValue();
        assertThat(captured.getReloadType()).isEqualTo("log_config_level");
        assertThat(captured.getInstanceIds()).containsExactly("biz-auth");
        assertThat(captured.getAdditionalParams()).containsEntry("logName", "com.example.service");
        assertThat(captured.getAdditionalParams()).containsEntry("level", "DEBUG");
        assertThat(result).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("레벨 Reload — level이 null이면 additionalParams에 포함되지 않는다 (부모 상속)")
    void level이_null이면_additionalParams에_포함되지_않음() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-demo"))
                .gubun("log_config_level")
                .logName("com.example")
                .level(null) // null → 부모 상속
                .build();

        given(reloadService.executeReload(any())).willReturn(ReloadResultResponse.builder()
                .reloadType("log_config_level")
                .results(List.of())
                .build());

        logLevelPropagationService.propagate(request);

        ArgumentCaptor<ReloadExecuteRequest> captor = ArgumentCaptor.forClass(ReloadExecuteRequest.class);
        verify(reloadService).executeReload(captor.capture());

        assertThat(captor.getValue().getAdditionalParams()).doesNotContainKey("level");
        assertThat(captor.getValue().getAdditionalParams()).containsEntry("logName", "com.example");
    }

    @Test
    @DisplayName("레벨 Reload — level이 공백이면 additionalParams에 포함되지 않는다")
    void level이_공백이면_additionalParams에_포함되지_않음() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-demo"))
                .gubun("log_config_level")
                .logName("com.example")
                .level("   ")
                .build();

        given(reloadService.executeReload(any())).willReturn(ReloadResultResponse.builder()
                .reloadType("log_config_level")
                .results(List.of())
                .build());

        logLevelPropagationService.propagate(request);

        ArgumentCaptor<ReloadExecuteRequest> captor = ArgumentCaptor.forClass(ReloadExecuteRequest.class);
        verify(reloadService).executeReload(captor.capture());

        assertThat(captor.getValue().getAdditionalParams()).doesNotContainKey("level");
    }

    // ─── Additivity Reload ────────────────────────────────────────────

    @Test
    @DisplayName("Additivity Reload — Y 값이 올바르게 전달된다")
    void additivity_Y_Reload_성공() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-auth"))
                .gubun("log_config_additivity")
                .logName("com.example")
                .additivity("Y")
                .build();

        given(reloadService.executeReload(any())).willReturn(ReloadResultResponse.builder()
                .reloadType("log_config_additivity")
                .results(List.of())
                .build());

        logLevelPropagationService.propagate(request);

        ArgumentCaptor<ReloadExecuteRequest> captor = ArgumentCaptor.forClass(ReloadExecuteRequest.class);
        verify(reloadService).executeReload(captor.capture());

        assertThat(captor.getValue().getAdditionalParams()).containsEntry("additivity", "Y");
        assertThat(captor.getValue().getAdditionalParams()).containsEntry("logName", "com.example");
    }

    @Test
    @DisplayName("Additivity Reload — additivity가 Y/N 외의 값이면 InvalidInputException")
    void additivity가_Y_N_외의_값이면_InvalidInputException() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-auth"))
                .gubun("log_config_additivity")
                .logName("com.example")
                .additivity("X")
                .build();

        assertThatThrownBy(() -> logLevelPropagationService.propagate(request))
                .isInstanceOf(InvalidInputException.class)
                .satisfies(e -> assertThat(((InvalidInputException) e).getDetailMessage()).contains("additivity"));

        verify(reloadService, never()).executeReload(any());
    }

    // ─── gubun 검증 ───────────────────────────────────────────────────

    @Test
    @DisplayName("잘못된 gubun이면 InvalidInputException이 발생하고 ReloadService가 호출되지 않는다")
    void 잘못된_gubun은_InvalidInputException() {
        LogLevelPropagateRequest request = LogLevelPropagateRequest.builder()
                .instanceIds(List.of("biz-auth"))
                .gubun("batch_reload")
                .logName("com.example")
                .build();

        assertThatThrownBy(() -> logLevelPropagationService.propagate(request))
                .isInstanceOf(InvalidInputException.class)
                .satisfies(e -> assertThat(((InvalidInputException) e).getDetailMessage()).contains("batch_reload"));

        verify(reloadService, never()).executeReload(any());
    }
}
