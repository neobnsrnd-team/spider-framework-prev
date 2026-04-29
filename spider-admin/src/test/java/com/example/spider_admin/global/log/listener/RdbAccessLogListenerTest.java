package com.example.spider_admin.global.log.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.adminhistory.mapper.AdminActionLogMapper;
import com.example.spider_admin.global.log.event.AccessLogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RdbAccessLogListener 테스트")
class RdbAccessLogListenerTest {

    @Mock
    private AdminActionLogMapper adminActionLogMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RdbAccessLogListener listener;

    @Test
    @DisplayName("CMS 승인 write 요청은 FWK_USER_ACCESS_HIS 저장 형식으로 기록된다")
    void onAccessLog_cmsApprovalWriteRequest_recordsMethodPathAndBody() throws Exception {
        AccessLogEvent event = new AccessLogEvent(
                "trace-1",
                "RES",
                "POST",
                "/api/cms-admin/pages/PAGE-001/approval/approve",
                "admin",
                "127.0.0.1",
                "20260415120000",
                "{\"beginningDate\":\"2026-04-15\"}",
                200,
                15,
                "SUCCESS",
                null);

        listener.onAccessLog(event);

        ArgumentCaptor<String> inputDataCaptor = ArgumentCaptor.forClass(String.class);
        then(adminActionLogMapper)
                .should()
                .insert(
                        eq("admin"),
                        eq("20260415120000"),
                        eq("127.0.0.1"),
                        eq("[POST] /api/cms-admin/pages/PAGE-001/approval/approve"),
                        inputDataCaptor.capture(),
                        eq("SUCCESS"));

        assertThat(inputDataCaptor.getValue()).contains("\"traceId\":\"trace-1\"");
        assertThat(inputDataCaptor.getValue()).contains("\"phase\":\"RES\"");
        assertThat(inputDataCaptor.getValue()).contains("\"status\":200");
        assertThat(objectMapper.readTree(inputDataCaptor.getValue()).get("data").asText())
                .contains("\"beginningDate\":\"2026-04-15\"");
    }

    @Test
    @DisplayName("REQ phase 이벤트는 status·duration 필드를 포함하지 않는다")
    void onAccessLog_reqPhase_doesNotIncludeStatusAndDuration() {
        AccessLogEvent event = new AccessLogEvent(
                "trace-2",
                "REQ",
                "GET",
                "/api/users",
                "admin",
                "127.0.0.1",
                "20260415120000",
                null,
                0,
                0,
                "SUCCESS",
                null);

        listener.onAccessLog(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(adminActionLogMapper).should().insert(any(), any(), any(), any(), captor.capture(), any());

        String inputData = captor.getValue();
        assertThat(inputData).contains("\"phase\":\"REQ\"");
        assertThat(inputData).doesNotContain("\"status\"");
        assertThat(inputData).doesNotContain("\"duration\"");
    }

    @Test
    @DisplayName("errorMessage가 있으면 inputData JSON에 포함된다")
    void onAccessLog_withErrorMessage_includesErrorMessageInInputData() {
        AccessLogEvent event = new AccessLogEvent(
                "trace-3",
                "RES",
                "POST",
                "/api/users",
                "admin",
                "127.0.0.1",
                "20260415120000",
                null,
                500,
                30,
                "FAIL",
                "NullPointerException");

        listener.onAccessLog(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(adminActionLogMapper).should().insert(any(), any(), any(), any(), captor.capture(), any());

        assertThat(captor.getValue()).contains("\"errorMessage\":\"NullPointerException\"");
    }

    @Test
    @DisplayName("inputData가 4000바이트를 초과하면 ...(truncated) 마커와 함께 잘린다")
    void onAccessLog_inputDataExceedsMaxBytes_truncatesWithMarker() {
        // 4000바이트를 확실히 초과하는 data 생성
        String largeData = "x".repeat(5000);
        AccessLogEvent event = new AccessLogEvent(
                "trace-4",
                "RES",
                "POST",
                "/api/data",
                "admin",
                "127.0.0.1",
                "20260415120000",
                largeData,
                200,
                10,
                "SUCCESS",
                null);

        listener.onAccessLog(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(adminActionLogMapper).should().insert(any(), any(), any(), any(), captor.capture(), any());

        String inputData = captor.getValue();
        assertThat(inputData.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(4000);
        assertThat(inputData).endsWith("...(truncated)");
    }
}
