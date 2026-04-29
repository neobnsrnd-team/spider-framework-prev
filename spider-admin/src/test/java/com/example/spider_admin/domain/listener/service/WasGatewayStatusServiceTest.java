package com.example.spider_admin.domain.listener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.spider_admin.domain.gwsystem.dto.SystemResponse;
import com.example.spider_admin.domain.gwsystem.mapper.SystemMapper;
import com.example.spider_admin.domain.listener.dto.SimpleResponse;
import com.example.spider_admin.domain.listener.dto.WasGatewayConnectionTestResponse;
import com.example.spider_admin.domain.listener.dto.WasGatewayStatusOptionsResponse;
import com.example.spider_admin.domain.listener.dto.WasGatewayStatusResponse;
import com.example.spider_admin.domain.listener.mapper.WasGatewayStatusMapper;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WasGatewayStatusService 테스트")
class WasGatewayStatusServiceTest {

    @Mock
    private WasGatewayStatusMapper wasGatewayStatusMapper;

    @Mock
    private WasInstanceMapper wasInstanceMapper;

    @Mock
    private SystemMapper systemMapper;

    @InjectMocks
    private WasGatewayStatusService wasGatewayStatusService;

    // ── getStatusPage ────────────────────────────────────────────────────────

    @Test
    @DisplayName("[목록] 정상 조회 시 PageResponse를 반환한다")
    void getStatusPage_withData_returnsPageResponse() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        List<WasGatewayStatusResponse> rows = List.of(WasGatewayStatusResponse.builder()
                .instanceId("INST1")
                .gwId("GW1")
                .systemId("SYS1")
                .build());
        given(wasGatewayStatusMapper.findBySearch(null, null, null, null, 0, 20, null, null))
                .willReturn(rows);
        given(wasGatewayStatusMapper.countBySearch(null, null, null, null)).willReturn(1L);

        PageResponse<WasGatewayStatusResponse> result =
                wasGatewayStatusService.getStatusPage(pageRequest, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("[목록] 결과가 없으면 빈 PageResponse를 반환한다")
    void getStatusPage_noData_returnsEmptyPageResponse() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        given(wasGatewayStatusMapper.findBySearch(null, null, null, null, 0, 20, null, null))
                .willReturn(List.of());
        given(wasGatewayStatusMapper.countBySearch(null, null, null, null)).willReturn(0L);

        PageResponse<WasGatewayStatusResponse> result =
                wasGatewayStatusService.getStatusPage(pageRequest, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("[목록] 검색 조건이 있을 때 normalize된 값으로 조회한다")
    void getStatusPage_withFilters_normalizesAndQueries() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        given(wasGatewayStatusMapper.findBySearch("INST1", "GW1", "R", null, 0, 20, null, null))
                .willReturn(List.of());
        given(wasGatewayStatusMapper.countBySearch("INST1", "GW1", "R", null)).willReturn(0L);

        PageResponse<WasGatewayStatusResponse> result =
                wasGatewayStatusService.getStatusPage(pageRequest, " INST1 ", " GW1 ", " R ", null);

        assertThat(result).isNotNull();
        verify(wasGatewayStatusMapper).findBySearch("INST1", "GW1", "R", null, 0, 20, null, null);
        verify(wasGatewayStatusMapper).countBySearch("INST1", "GW1", "R", null);
    }

    @Test
    @DisplayName("[목록] mapper가 null을 반환하면 빈 content로 처리한다")
    void getStatusPage_mapperReturnsNull_treatsAsEmpty() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        given(wasGatewayStatusMapper.findBySearch(null, null, null, null, 0, 20, null, null))
                .willReturn(null);
        given(wasGatewayStatusMapper.countBySearch(null, null, null, null)).willReturn(0L);

        PageResponse<WasGatewayStatusResponse> result =
                wasGatewayStatusService.getStatusPage(pageRequest, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
    }

    // ── getOptions ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("[옵션] 정상 조회 시 instances/gateways/operModes를 반환한다")
    void getOptions_withData_returnsOptions() {
        given(wasGatewayStatusMapper.findDistinctInstances())
                .willReturn(List.of(
                        SimpleResponse.builder().value("INST1").label("인스턴스1").build()));
        given(wasGatewayStatusMapper.findDistinctGateways())
                .willReturn(List.of(
                        SimpleResponse.builder().value("GW1").label("게이트웨이1").build()));
        given(wasGatewayStatusMapper.findDistinctOperModes()).willReturn(List.of("R", "D"));

        WasGatewayStatusOptionsResponse result = wasGatewayStatusService.getOptions();

        assertThat(result.getInstances()).hasSize(1);
        assertThat(result.getGateways()).hasSize(1);
        assertThat(result.getOperModes()).hasSize(2);
        assertThat(result.getOperModes().get(0).getValue()).isEqualTo("R");
        assertThat(result.getOperModes().get(0).getLabel()).isEqualTo("운영");
        assertThat(result.getOperModes().get(1).getLabel()).isEqualTo("개발");
    }

    @Test
    @DisplayName("[옵션] mapper가 null을 반환하면 빈 목록으로 처리한다")
    void getOptions_mapperReturnsNull_returnsEmptyLists() {
        given(wasGatewayStatusMapper.findDistinctInstances()).willReturn(null);
        given(wasGatewayStatusMapper.findDistinctGateways()).willReturn(null);
        given(wasGatewayStatusMapper.findDistinctOperModes()).willReturn(null);

        WasGatewayStatusOptionsResponse result = wasGatewayStatusService.getOptions();

        assertThat(result.getInstances()).isEmpty();
        assertThat(result.getGateways()).isEmpty();
        assertThat(result.getOperModes()).isEmpty();
    }

    @Test
    @DisplayName("[옵션] operModes에 null 항목이 있으면 필터링한다")
    void getOptions_operModesWithNull_filtersNulls() {
        given(wasGatewayStatusMapper.findDistinctInstances()).willReturn(List.of());
        given(wasGatewayStatusMapper.findDistinctGateways()).willReturn(List.of());
        given(wasGatewayStatusMapper.findDistinctOperModes()).willReturn(List.of("R"));

        WasGatewayStatusOptionsResponse result = wasGatewayStatusService.getOptions();

        assertThat(result.getOperModes()).hasSize(1);
    }

    // ── testConnection ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[연결테스트] 시스템이 정지 상태(STOP_YN=Y)이면 connected=false를 반환한다")
    void testConnection_stoppedSystem_returnsNotConnected() {
        SystemResponse stoppedSystem = SystemResponse.builder()
                .gwId("GW1")
                .systemId("SYS1")
                .stopYn("Y")
                .ip("192.168.1.1")
                .port("8080")
                .build();
        WasInstanceResponse instance = WasInstanceResponse.builder()
                .instanceId("INST1")
                .ip("10.0.0.1")
                .port("9090")
                .build();

        given(systemMapper.selectResponseBySystem("GW1", "SYS1")).willReturn(stoppedSystem);
        given(wasInstanceMapper.selectResponseById("INST1")).willReturn(instance);

        WasGatewayConnectionTestResponse result = wasGatewayStatusService.testConnection("INST1", "GW1", "SYS1");

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getMessage()).isEqualTo("정지 상태의 시스템입니다.");
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("[연결테스트] 시스템을 찾을 수 없으면 NotFoundException을 던진다")
    void testConnection_systemNotFound_throwsNotFoundException() {
        given(systemMapper.selectResponseBySystem("GW1", "NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> wasGatewayStatusService.testConnection("INST1", "GW1", "NOT-EXIST"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[연결테스트] 인스턴스를 찾을 수 없으면 NotFoundException을 던진다")
    void testConnection_instanceNotFound_throwsNotFoundException() {
        SystemResponse system = SystemResponse.builder()
                .gwId("GW1")
                .systemId("SYS1")
                .stopYn("N")
                .build();
        given(systemMapper.selectResponseBySystem("GW1", "SYS1")).willReturn(system);
        given(wasInstanceMapper.selectResponseById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> wasGatewayStatusService.testConnection("NOT-EXIST", "GW1", "SYS1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[연결테스트] instanceId가 blank이면 InvalidInputException을 던진다")
    void testConnection_blankInstanceId_throwsInvalidInputException() {
        assertThatThrownBy(() -> wasGatewayStatusService.testConnection("", "GW1", "SYS1"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[연결테스트] IP/PORT가 없으면 InvalidInputException을 던진다")
    void testConnection_noIpPort_throwsInvalidInputException() {
        SystemResponse system = SystemResponse.builder()
                .gwId("GW1")
                .systemId("SYS1")
                .stopYn("N")
                .ip(null)
                .port(null)
                .build();
        WasInstanceResponse instance = WasInstanceResponse.builder()
                .instanceId("INST1")
                .ip(null)
                .port(null)
                .build();

        given(systemMapper.selectResponseBySystem("GW1", "SYS1")).willReturn(system);
        given(wasInstanceMapper.selectResponseById("INST1")).willReturn(instance);

        assertThatThrownBy(() -> wasGatewayStatusService.testConnection("INST1", "GW1", "SYS1"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[연결테스트] 연결 불가 대상이면 connected=false와 실패 메시지를 반환한다")
    void testConnection_unreachableTarget_returnsFailResult() {
        SystemResponse system = SystemResponse.builder()
                .gwId("GW1")
                .systemId("SYS1")
                .stopYn("N")
                .ip("192.0.2.1") // TEST-NET — 실제 연결 불가
                .port("9")
                .build();
        WasInstanceResponse instance = WasInstanceResponse.builder()
                .instanceId("INST1")
                .ip("192.0.2.1")
                .port("9")
                .build();

        given(systemMapper.selectResponseBySystem("GW1", "SYS1")).willReturn(system);
        given(wasInstanceMapper.selectResponseById("INST1")).willReturn(instance);

        WasGatewayConnectionTestResponse result = wasGatewayStatusService.testConnection("INST1", "GW1", "SYS1");

        assertThat(result.isConnected()).isFalse();
        assertThat(result.getMessage()).startsWith("연결 실패");
        assertThat(result.getCheckedAt()).isNotNull();
    }

    // ── exportGatewayStatus ──────────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀-상태] 정상 데이터로 엑셀 바이트 배열을 반환한다")
    void exportGatewayStatus_withData_returnsBytes() {
        List<WasGatewayStatusResponse> data = List.of(WasGatewayStatusResponse.builder()
                .instanceName("인스턴스1")
                .gwName("게이트웨이1")
                .systemIp("10.0.0.1")
                .systemPort("8080")
                .stopYn("N")
                .build());
        given(wasGatewayStatusMapper.findAllForExport(null, null, null, null, null, null))
                .willReturn(data);

        byte[] result = wasGatewayStatusService.exportGatewayStatus(null, null, null, null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀-상태] 빈 데이터로도 엑셀을 생성한다")
    void exportGatewayStatus_emptyData_returnsBytes() {
        given(wasGatewayStatusMapper.findAllForExport(null, null, null, null, null, null))
                .willReturn(List.of());

        byte[] result = wasGatewayStatusService.exportGatewayStatus(null, null, null, null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    // ── exportGatewayMonitor ─────────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀-모니터] 정상 데이터로 엑셀 바이트 배열을 반환한다")
    void exportGatewayMonitor_withData_returnsBytes() {
        List<WasGatewayStatusResponse> data = List.of(WasGatewayStatusResponse.builder()
                .instanceName("인스턴스1")
                .gwName("게이트웨이1")
                .wasInstanceStatus("RUNNING")
                .activeCountIdle(5)
                .build());
        given(wasGatewayStatusMapper.findAllForExport(null, null, null, null, null, null))
                .willReturn(data);

        byte[] result = wasGatewayStatusService.exportGatewayMonitor(null, null, null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀-모니터] 빈 데이터로도 엑셀을 생성한다")
    void exportGatewayMonitor_emptyData_returnsBytes() {
        given(wasGatewayStatusMapper.findAllForExport(null, null, null, null, null, null))
                .willReturn(List.of());

        byte[] result = wasGatewayStatusService.exportGatewayMonitor(null, null, null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }
}
