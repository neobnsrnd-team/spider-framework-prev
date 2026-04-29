package com.example.spider_admin.domain.listener.service;

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
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasGatewayStatusService {

    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final WasInstanceMapper wasInstanceMapper;
    private final SystemMapper systemMapper;
    private final WasGatewayStatusMapper wasGatewayStatusMapper;

    public PageResponse<WasGatewayStatusResponse> getStatusPage(
            PageRequest pageRequest, String instanceId, String gwId, String operModeType, String stopYn) {
        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        List<WasGatewayStatusResponse> rows = wasGatewayStatusMapper.findBySearch(
                normalize(instanceId),
                normalize(gwId),
                normalize(operModeType),
                normalize(stopYn),
                offset,
                size,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());
        long total = wasGatewayStatusMapper.countBySearch(
                normalize(instanceId), normalize(gwId), normalize(operModeType), normalize(stopYn));

        int totalPages = (int) Math.ceil((double) total / size);

        return PageResponse.<WasGatewayStatusResponse>builder()
                .content(rows == null ? List.of() : rows)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(total)
                .size(size)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    public WasGatewayStatusOptionsResponse getOptions() {
        List<SimpleResponse> instances = defaultList(wasGatewayStatusMapper.findDistinctInstances());
        List<SimpleResponse> gateways = defaultList(wasGatewayStatusMapper.findDistinctGateways());

        List<String> operModesRaw = defaultList(wasGatewayStatusMapper.findDistinctOperModes());
        List<SimpleResponse> operModes = operModesRaw.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(code -> SimpleResponse.builder()
                        .value(code)
                        .label(operModeLabel(code))
                        .build())
                .toList();

        return WasGatewayStatusOptionsResponse.builder()
                .instances(instances)
                .gateways(gateways)
                .operModes(operModes)
                .build();
    }

    public WasGatewayConnectionTestResponse testConnection(String instanceId, String gwId, String systemId) {
        if (isBlank(instanceId) || isBlank(gwId) || isBlank(systemId)) {
            throw new InvalidInputException("InstanceId, GwId, SystemId are required.");
        }

        SystemResponse system = systemMapper.selectResponseBySystem(gwId, systemId);
        if (system == null) {
            throw new NotFoundException("System not found for gwId=" + gwId + ", systemId=" + systemId);
        }

        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(instanceId);
        if (instance == null) {
            throw new NotFoundException("Instance not found: " + instanceId);
        }

        if ("Y".equals(system.getStopYn())) {
            return WasGatewayConnectionTestResponse.builder()
                    .connected(false)
                    .latencyMs(null)
                    .message("정지 상태의 시스템입니다.")
                    .checkedAt(LocalDateTime.now().format(TS_FORMATTER))
                    .targetIp(system.getIp())
                    .targetPort(parsePort(system.getPort()))
                    .build();
        }

        String[] target = resolveTargetIpPort(instance, system);
        String targetIp = target[0];
        Integer targetPort = parsePort(target[1]);

        boolean connected = false;
        Long latency = null;
        String message;

        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetIp, targetPort), DEFAULT_TIMEOUT_MS);
            latency = System.currentTimeMillis() - start;
            connected = true;
            message = "연결 성공";
        } catch (Exception e) {
            message = "연결 실패: " + e.getMessage();
            log.warn(
                    "Connection test failed: instanceId={}, gwId={}, systemId={}, ip={}, port={}, error={}",
                    instanceId,
                    gwId,
                    systemId,
                    targetIp,
                    targetPort,
                    e.getMessage());
        }

        return WasGatewayConnectionTestResponse.builder()
                .connected(connected)
                .latencyMs(latency)
                .message(message)
                .checkedAt(LocalDateTime.now().format(TS_FORMATTER))
                .targetIp(targetIp)
                .targetPort(targetPort)
                .build();
    }

    private String[] resolveTargetIpPort(WasInstanceResponse instance, SystemResponse system) {
        // 1순위: 현재 행의 WAS 인스턴스 IP/PORT (실제 연결 주체)
        String targetIp = !isBlank(instance.getIp()) ? instance.getIp() : null;
        String targetPortStr = !isBlank(instance.getPort()) ? instance.getPort() : null;

        // 2순위: 시스템에 연계된 적용 대상 WAS 인스턴스(APPLIED_WAS_INSTANCE) IP/PORT
        if (isBlank(targetIp) || isBlank(targetPortStr)) {
            WasInstanceResponse applied = lookupAppliedInstance(system.getAppliedWasInstance());
            if (applied != null) {
                if (isBlank(targetIp)) targetIp = applied.getIp();
                if (isBlank(targetPortStr)) targetPortStr = applied.getPort();
            }
        }

        // 3순위: 시스템에 설정된 IP/PORT
        if (isBlank(targetIp)) targetIp = system.getIp();
        if (isBlank(targetPortStr)) targetPortStr = system.getPort();

        validateTargetIpPort(targetIp, targetPortStr);

        return new String[] {targetIp, targetPortStr};
    }

    private WasInstanceResponse lookupAppliedInstance(String appliedWasInstanceId) {
        if (isBlank(appliedWasInstanceId)) return null;
        return wasInstanceMapper.selectResponseById(appliedWasInstanceId);
    }

    private void validateTargetIpPort(String targetIp, String targetPortStr) {
        if (isBlank(targetIp) || isBlank(targetPortStr)) {
            throw new InvalidInputException("테스트 대상 IP/PORT가 없습니다. 시스템 IP/PORT 또는 적용 인스턴스 정보를 설정해주세요.");
        }
        if (parsePort(targetPortStr) == null) {
            throw new InvalidInputException("PORT 형식이 올바르지 않습니다: " + targetPortStr);
        }
    }

    public byte[] exportGatewayStatus(
            String instanceId, String gwId, String operModeType, String stopYn, String sortBy, String sortDirection) {
        List<WasGatewayStatusResponse> data = wasGatewayStatusMapper.findAllForExport(
                normalize(instanceId),
                normalize(gwId),
                normalize(operModeType),
                normalize(stopYn),
                sortBy,
                sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("인스턴스명", 18, "instanceName"),
                new ExcelColumnDefinition("유형", 10, "instanceType"),
                new ExcelColumnDefinition("인스턴스IP", 15, "instanceIp"),
                new ExcelColumnDefinition("인스턴스PORT", 12, "instancePort"),
                new ExcelColumnDefinition("G/W명", 18, "gwName"),
                new ExcelColumnDefinition("G/W속성", 20, "gwProperties"),
                new ExcelColumnDefinition("G/WTHREAD", 10, "threadCount"),
                new ExcelColumnDefinition("기능수행", 10, "ioType"),
                new ExcelColumnDefinition("운영모드", 10, "operModeType"),
                new ExcelColumnDefinition("SYSTEMIP", 15, "systemIp"),
                new ExcelColumnDefinition("SYSTEMPORT", 12, "systemPort"),
                new ExcelColumnDefinition("정지여부", 10, "stopYn"),
                new ExcelColumnDefinition("SYSTEM설명", 25, "systemDesc"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instanceName", item.getInstanceName());
            row.put("instanceType", item.getInstanceType());
            row.put("instanceIp", item.getInstanceIp());
            row.put("instancePort", item.getInstancePort());
            row.put("gwName", item.getGwName());
            row.put("gwProperties", item.getGwProperties());
            row.put("threadCount", item.getThreadCount());
            row.put("ioType", item.getIoType());
            row.put("operModeType", item.getOperModeType());
            row.put("systemIp", item.getSystemIp());
            row.put("systemPort", item.getSystemPort());
            row.put("stopYn", item.getStopYn());
            row.put("systemDesc", item.getSystemDesc());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("WAS_Gateway_Status", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public byte[] exportGatewayMonitor(
            String instanceId, String gwId, String operModeType, String sortBy, String sortDirection) {
        List<WasGatewayStatusResponse> data = wasGatewayStatusMapper.findAllForExport(
                normalize(instanceId), normalize(gwId), normalize(operModeType), null, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("인스턴스 명", 18, "instanceName"),
                new ExcelColumnDefinition("유형", 10, "instanceType"),
                new ExcelColumnDefinition("인스턴스 IP", 15, "instanceIp"),
                new ExcelColumnDefinition("인스턴스 PORT", 12, "instancePort"),
                new ExcelColumnDefinition("상태", 10, "wasInstanceStatus"),
                new ExcelColumnDefinition("소켓 연결갯수", 12, "activeCountIdle"),
                new ExcelColumnDefinition("최근 수정시간", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("G/W 명", 18, "gwName"),
                new ExcelColumnDefinition("G/W 속성", 20, "gwProperties"),
                new ExcelColumnDefinition("G/W THREAD", 10, "threadCount"),
                new ExcelColumnDefinition("기동 수동", 10, "ioType"),
                new ExcelColumnDefinition("운영모드", 10, "operModeType"),
                new ExcelColumnDefinition("IP", 15, "systemIp"),
                new ExcelColumnDefinition("PORT", 12, "systemPort"),
                new ExcelColumnDefinition("SYSTEM 설명", 25, "systemDesc"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instanceName", item.getInstanceName());
            row.put("instanceType", item.getInstanceType());
            row.put("instanceIp", item.getInstanceIp());
            row.put("instancePort", item.getInstancePort());
            row.put("wasInstanceStatus", item.getWasInstanceStatus());
            row.put("activeCountIdle", item.getActiveCountIdle());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("gwName", item.getGwName());
            row.put("gwProperties", item.getGwProperties());
            row.put("threadCount", item.getThreadCount());
            row.put("ioType", item.getIoType());
            row.put("operModeType", item.getOperModeType());
            row.put("systemIp", item.getSystemIp());
            row.put("systemPort", item.getSystemPort());
            row.put("systemDesc", item.getSystemDesc());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("WAS_Gateway_Monitor", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    private String operModeLabel(String code) {
        if ("R".equalsIgnoreCase(code)) return "운영";
        if ("T".equalsIgnoreCase(code)) return "테스트";
        if ("D".equalsIgnoreCase(code)) return "개발";
        return code;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Integer parsePort(String port) {
        if (isBlank(port)) {
            return null;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private <T> List<T> defaultList(List<T> src) {
        return src == null ? new ArrayList<>() : src;
    }
}
