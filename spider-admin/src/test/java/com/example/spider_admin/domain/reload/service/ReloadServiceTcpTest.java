package com.example.spider_admin.domain.reload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.spider_admin.domain.property.dto.PropertyResponse;
import com.example.spider_admin.domain.property.mapper.PropertyMapper;
import com.example.spider_admin.domain.reload.dto.ReloadExecuteRequest;
import com.example.spider_admin.domain.reload.dto.ReloadResultResponse;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.spider_admin.infra.tcp.client.TcpClient;
import com.example.spider_admin.infra.tcp.model.JsonCommandRequest;
import com.example.spider_admin.infra.tcp.model.JsonCommandResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReloadService HTTP/TCP 분기 테스트")
class ReloadServiceTcpTest {

    private static final String FIELD_DEFAULT_PORT = "defaultManagementPort";
    private static final String FIELD_DEFAULT_IP = "defaultManagementIp";
    private static final String FIELD_ENDPOINT = "managementEndpoint";
    private static final String FIELD_PROPERTY_GROUP = "propertyGroup";

    @Mock
    private WasInstanceMapper wasInstanceMapper;

    @Mock
    private PropertyMapper propertyMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TcpClient tcpClient;

    @InjectMocks
    private ReloadService reloadService;

    @BeforeEach
    void setUpValues() {
        // @Value 필드는 Spring 컨텍스트 없이는 주입되지 않으므로 직접 설정
        ReflectionTestUtils.setField(reloadService, FIELD_DEFAULT_PORT, 50005);
        ReflectionTestUtils.setField(reloadService, FIELD_DEFAULT_IP, "localhost");
        ReflectionTestUtils.setField(reloadService, FIELD_ENDPOINT, "/api/management/reload");
        ReflectionTestUtils.setField(reloadService, FIELD_PROPERTY_GROUP, "was_config");
    }

    // ─── TCP 분기 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("COMM_TYPE=TCP이면 TcpClient를 사용하고 RestTemplate은 호출되지 않는다")
    void COMM_TYPE이_TCP이면_TcpClient를_사용한다() throws IOException {
        // given — 인스턴스 조회
        given(wasInstanceMapper.selectResponseById("biz-auth"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-auth")
                        .instanceName("BizAuth WAS")
                        .build());

        // given — FWK_PROPERTY: COMM_TYPE=TCP, IP/PORT는 기본값 사용
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.COMM_TYPE")))
                .willReturn(PropertyResponse.builder().defaultValue("TCP").build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_IP")))
                .willReturn(null);
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_PORT")))
                .willReturn(null);

        // given — TcpClient 성공 응답
        given(tcpClient.sendJson(anyString(), anyInt(), any(JsonCommandRequest.class)))
                .willReturn(JsonCommandResponse.builder()
                        .success(true)
                        .message("완료")
                        .build());

        ReloadExecuteRequest request = ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-auth"))
                .additionalParams(Map.of("logName", "com.example", "level", "DEBUG"))
                .build();

        // when
        ReloadResultResponse result = reloadService.executeReload(request);

        // then
        verify(tcpClient).sendJson(anyString(), anyInt(), any(JsonCommandRequest.class));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));

        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).isSuccess()).isTrue();
        assertThat(result.getResults().get(0).getInstanceId()).isEqualTo("biz-auth");
    }

    @Test
    @DisplayName("COMM_TYPE=TCP일 때 TcpClient payload에 gubun과 additionalParams가 포함된다")
    void TCP_요청_payload_검증() throws IOException {
        given(wasInstanceMapper.selectResponseById("biz-auth"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-auth")
                        .instanceName("BizAuth WAS")
                        .build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.COMM_TYPE")))
                .willReturn(PropertyResponse.builder().defaultValue("TCP").build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_IP")))
                .willReturn(null);
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_PORT")))
                .willReturn(null);
        given(tcpClient.sendJson(anyString(), anyInt(), any()))
                .willReturn(JsonCommandResponse.builder().success(true).build());

        reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-auth"))
                .additionalParams(Map.of("logName", "com.example", "level", "INFO"))
                .build());

        ArgumentCaptor<JsonCommandRequest> captor = ArgumentCaptor.forClass(JsonCommandRequest.class);
        verify(tcpClient).sendJson(anyString(), anyInt(), captor.capture());

        JsonCommandRequest captured = captor.getValue();
        assertThat(captured.getCommand()).isEqualTo("MANAGEMENT_RELOAD");
        assertThat(captured.getPayload()).containsEntry("gubun", "log_config_level");
        assertThat(captured.getPayload()).containsEntry("logName", "com.example");
        assertThat(captured.getPayload()).containsEntry("level", "INFO");
    }

    @Test
    @DisplayName("COMM_TYPE=TCP일 때 TcpClient IOException이 발생하면 실패 결과가 반환된다")
    void TCP_IOException이면_실패_결과() throws IOException {
        given(wasInstanceMapper.selectResponseById("biz-auth"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-auth")
                        .instanceName("BizAuth WAS")
                        .build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.COMM_TYPE")))
                .willReturn(PropertyResponse.builder().defaultValue("TCP").build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_IP")))
                .willReturn(null);
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-auth.MANAGEMENT_SERVER_PORT")))
                .willReturn(null);
        given(tcpClient.sendJson(anyString(), anyInt(), any())).willThrow(new IOException("연결 거부"));

        ReloadResultResponse result = reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-auth"))
                .additionalParams(Map.of("logName", "com.example"))
                .build());

        assertThat(result.getResults().get(0).isSuccess()).isFalse();
        assertThat(result.getResults().get(0).getErrorMessage()).contains("biz-auth");
    }

    // ─── HTTP 분기 ────────────────────────────────────────────────────

    @Test
    @DisplayName("COMM_TYPE 프로퍼티가 없으면 RestTemplate을 사용하고 TcpClient는 호출되지 않는다")
    void COMM_TYPE이_없으면_HTTP를_사용한다() throws IOException {
        given(wasInstanceMapper.selectResponseById("biz-demo"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-demo")
                        .instanceName("BizDemo WAS")
                        .build());

        // COMM_TYPE 프로퍼티 없음 → null 반환 → 기본값 "HTTP" 적용
        given(propertyMapper.selectResponseById(eq("was_config"), anyString())).willReturn(null);

        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("success", true)));

        reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-demo"))
                .additionalParams(Map.of("logName", "com.example"))
                .build());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
        verify(tcpClient, never()).sendJson(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("COMM_TYPE=HTTP이면 RestTemplate을 사용한다")
    void COMM_TYPE이_HTTP이면_RestTemplate을_사용한다() throws IOException {
        given(wasInstanceMapper.selectResponseById("biz-demo"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-demo")
                        .instanceName("BizDemo WAS")
                        .build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-demo.COMM_TYPE")))
                .willReturn(PropertyResponse.builder().defaultValue("HTTP").build());
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-demo.MANAGEMENT_SERVER_IP")))
                .willReturn(null);
        given(propertyMapper.selectResponseById(eq("was_config"), eq("biz-demo.MANAGEMENT_SERVER_PORT")))
                .willReturn(null);

        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("success", true)));

        ReloadResultResponse result = reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-demo"))
                .additionalParams(Map.of("logName", "com.example"))
                .build());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
        verify(tcpClient, never()).sendJson(anyString(), anyInt(), any());
        assertThat(result.getResults().get(0).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("HTTP 응답 success=false이면 실패 결과가 반환된다")
    void HTTP_응답_success_false이면_실패_결과() throws IOException {
        given(wasInstanceMapper.selectResponseById("biz-demo"))
                .willReturn(WasInstanceResponse.builder()
                        .instanceId("biz-demo")
                        .instanceName("BizDemo WAS")
                        .build());
        given(propertyMapper.selectResponseById(eq("was_config"), anyString())).willReturn(null);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("success", false, "message", "처리 실패")));

        ReloadResultResponse result = reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("biz-demo"))
                .additionalParams(Map.of("logName", "com.example"))
                .build());

        assertThat(result.getResults().get(0).isSuccess()).isFalse();
        assertThat(result.getResults().get(0).getErrorMessage()).contains("처리 실패");
    }

    // ─── 인스턴스 미존재 ──────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 instanceId이면 실패 결과가 반환된다")
    void 존재하지_않는_인스턴스면_실패_결과() {
        given(wasInstanceMapper.selectResponseById("unknown-was")).willReturn(null);

        ReloadResultResponse result = reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType("log_config_level")
                .instanceIds(List.of("unknown-was"))
                .build());

        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).isSuccess()).isFalse();
        assertThat(result.getResults().get(0).getErrorMessage()).contains("unknown-was");
    }
}
