package com.example.admin_demo.domain.wasinstance.service;

import com.example.admin_demo.domain.batch.mapper.WasExecBatchMapper;
import com.example.admin_demo.domain.property.dto.PropertyResponse;
import com.example.admin_demo.domain.property.mapper.PropertyMapper;
import com.example.admin_demo.domain.wasinstance.dto.PoolStatusResponse;
import com.example.admin_demo.domain.wasinstance.dto.WasInstanceBatchSaveRequest;
import com.example.admin_demo.domain.wasinstance.dto.WasInstanceRequest;
import com.example.admin_demo.domain.wasinstance.dto.WasInstanceResponse;
import com.example.admin_demo.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.admin_demo.domain.wasproperty.service.WasPropertyService;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.DuplicateException;
import com.example.admin_demo.global.exception.InternalException;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import com.example.admin_demo.global.util.ExcelColumnDefinition;
import com.example.admin_demo.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasInstanceService {

    private final WasInstanceMapper wasInstanceMapper;
    private final WasPropertyService wasPropertyService;
    private final WasExecBatchMapper wasExecBatchMapper;
    private final RestTemplate restTemplate;
    private final PropertyMapper propertyMapper;

    @Value("${reload.management.default-port:50005}")
    private int defaultManagementPort;

    @Value("${reload.management.default-ip:localhost}")
    private String defaultManagementIp;

    @Value("${reload.management.property-group:was_config}")
    private String propertyGroup;

    public List<WasInstanceResponse> getAllInstances() {
        log.info("Fetching all WAS instances");
        return wasInstanceMapper.selectAll();
    }

    public PageResponse<WasInstanceResponse> getInstances(
            PageRequest pageRequest, String instanceName, String instanceType, String operModeType) {
        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        List<WasInstanceResponse> dtos = wasInstanceMapper.findBySearchPaging(
                instanceName,
                instanceType,
                operModeType,
                offset,
                size,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());
        long totalCount = wasInstanceMapper.countBySearch(instanceName, instanceType, operModeType);

        int totalPages = (int) Math.ceil((double) totalCount / size);
        return PageResponse.<WasInstanceResponse>builder()
                .content(dtos)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(totalCount)
                .size(size)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    public WasInstanceResponse getInstanceById(String instanceId) {
        log.info("Fetching WAS instance by ID: {}", instanceId);

        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(instanceId);
        if (instance == null) {
            throw new NotFoundException("instanceId: " + instanceId);
        }

        return instance;
    }

    @Transactional
    public WasInstanceResponse createInstance(WasInstanceRequest dto) {
        log.info("Creating new WAS instance: {}", dto.getInstanceId());

        if (wasInstanceMapper.countById(dto.getInstanceId()) > 0) {
            throw new DuplicateException("instanceId: " + dto.getInstanceId());
        }

        wasInstanceMapper.insert(dto);

        log.info("WAS instance created successfully: {}", dto.getInstanceId());
        return wasInstanceMapper.selectResponseById(dto.getInstanceId());
    }

    @Transactional
    public WasInstanceResponse updateInstance(String instanceId, WasInstanceRequest dto) {
        log.info("Updating WAS instance: {}", instanceId);

        if (wasInstanceMapper.countById(instanceId) == 0) {
            throw new NotFoundException("instanceId: " + instanceId);
        }

        wasInstanceMapper.update(instanceId, dto);

        log.info("WAS instance updated successfully: {}", instanceId);
        return wasInstanceMapper.selectResponseById(instanceId);
    }

    @Transactional
    public void deleteInstance(String instanceId) {
        log.info("Deleting WAS instance: {}", instanceId);

        if (wasInstanceMapper.countById(instanceId) == 0) {
            throw new NotFoundException("instanceId: " + instanceId);
        }

        wasPropertyService.deleteByInstanceId(instanceId);
        wasExecBatchMapper.deleteByInstanceId(instanceId);
        wasInstanceMapper.deleteById(instanceId);
        log.info("WAS instance deleted successfully: {}", instanceId);
    }

    @Transactional
    public int batchSave(List<WasInstanceBatchSaveRequest> requests) {
        int processedCount = 0;

        for (WasInstanceBatchSaveRequest request : requests) {
            WasInstanceRequest dto = toRequest(request);
            switch (request.getCrud()) {
                case "C" -> {
                    if (wasInstanceMapper.countById(request.getInstanceId()) > 0) {
                        throw new DuplicateException("instanceId: " + request.getInstanceId());
                    }
                    wasInstanceMapper.insert(dto);
                    processedCount++;
                }
                case "U" -> {
                    if (wasInstanceMapper.countById(request.getInstanceId()) == 0) {
                        throw new NotFoundException("instanceId: " + request.getInstanceId());
                    }
                    wasInstanceMapper.update(request.getInstanceId(), dto);
                    processedCount++;
                }
                case "D" -> {
                    if (wasInstanceMapper.countById(request.getInstanceId()) == 0) {
                        throw new NotFoundException("instanceId: " + request.getInstanceId());
                    }
                    wasPropertyService.deleteByInstanceId(request.getInstanceId());
                    wasExecBatchMapper.deleteByInstanceId(request.getInstanceId());
                    wasInstanceMapper.deleteById(request.getInstanceId());
                    processedCount++;
                }
                default -> log.warn("Unknown CRUD action: {}", request.getCrud());
            }
        }

        return processedCount;
    }

    private WasInstanceRequest toRequest(WasInstanceBatchSaveRequest batch) {
        return WasInstanceRequest.builder()
                .instanceId(batch.getInstanceId())
                .instanceName(batch.getInstanceName())
                .instanceDesc(batch.getInstanceDesc())
                .wasConfigId(batch.getWasConfigId())
                .instanceType(batch.getInstanceType())
                .ip(batch.getIp())
                .port(batch.getPort())
                .operModeType(batch.getOperModeType())
                .build();
    }

    public byte[] exportWasInstances(
            String instanceName, String instanceType, String operModeType, String sortBy, String sortDirection) {
        List<WasInstanceResponse> data =
                wasInstanceMapper.findAllForExport(instanceName, instanceType, operModeType, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("인스턴스 ID", 12, "instanceId"),
                new ExcelColumnDefinition("인스턴스명", 20, "instanceName"),
                new ExcelColumnDefinition("인스턴스 설명", 30, "instanceDesc"),
                new ExcelColumnDefinition("WAS 설정 ID", 12, "wasConfigId"),
                new ExcelColumnDefinition("인스턴스 구분", 12, "instanceType"),
                new ExcelColumnDefinition("IP", 15, "ip"),
                new ExcelColumnDefinition("포트", 8, "port"),
                new ExcelColumnDefinition("운영 모드", 10, "operModeType"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (WasInstanceResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instanceId", item.getInstanceId());
            row.put("instanceName", item.getInstanceName());
            row.put("instanceDesc", item.getInstanceDesc());
            row.put("wasConfigId", item.getWasConfigId());
            row.put("instanceType", item.getInstanceType());
            row.put("ip", item.getIp());
            row.put("port", item.getPort());
            row.put("operModeType", item.getOperModeType());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("WAS 인스턴스", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * AP 서버 소켓 풀 현황 조회.
     * spider-link {@code GET /api/internal/pool/status}를 호출하여 반환한다.
     */
    public PoolStatusResponse getPoolStatus(String instanceId) {
        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(instanceId);
        if (instance == null) {
            throw new NotFoundException("instanceId: " + instanceId);
        }

        String managementIp = resolveManagementProperty(instanceId, "MANAGEMENT_SERVER_IP", defaultManagementIp);
        int managementPort = resolveManagementPort(instanceId);
        String url = String.format("http://%s:%d/api/internal/pool/status", managementIp, managementPort);

        log.info("풀 현황 조회: instanceId={}, url={}", instanceId, url);

        try {
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return PoolStatusResponse.builder()
                        .instanceId(instanceId)
                        .instanceName(instance.getInstanceName())
                        .success(true)
                        .pools(parsePools(body.get("pools")))
                        .build();
            }

            return PoolStatusResponse.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage("응답 상태: " + response.getStatusCode())
                    .build();

        } catch (RestClientException e) {
            String errorMsg = String.format(
                    "%s 서버에 연결 중 오류가 발생하였습니다.[host=%s,port=%d]", instanceId, managementIp, managementPort);
            log.error("풀 현황 조회 통신 오류: {}", errorMsg, e);
            return PoolStatusResponse.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        }
    }

    private Map<String, PoolStatusResponse.PoolInfo> parsePools(Object poolsObj) {
        if (!(poolsObj instanceof Map<?, ?> rawPools)) {
            return Collections.emptyMap();
        }
        Map<String, PoolStatusResponse.PoolInfo> result = new LinkedHashMap<>();
        rawPools.forEach((key, value) -> {
            if (!(value instanceof Map<?, ?> poolMap)) return;
            result.put(
                    String.valueOf(key),
                    PoolStatusResponse.PoolInfo.builder()
                            .host(poolMap.get("host") != null ? String.valueOf(poolMap.get("host")) : "")
                            .port(toPoolInt(poolMap.get("port")))
                            .active(toPoolInt(poolMap.get("active")))
                            .idle(toPoolInt(poolMap.get("idle")))
                            .total(toPoolInt(poolMap.get("total")))
                            .maxActive(toPoolInt(poolMap.get("maxActive")))
                            .build());
        });
        return result;
    }

    private int toPoolInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        return 0;
    }

    private int resolveManagementPort(String instanceId) {
        String value = resolveManagementProperty(instanceId, "MANAGEMENT_SERVER_PORT", null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.debug("Management port 파싱 실패, 기본값 사용: instanceId={}", instanceId, e);
            }
        }
        return defaultManagementPort;
    }

    private String resolveManagementProperty(String instanceId, String suffix, String defaultValue) {
        try {
            String propertyId = instanceId + "." + suffix;
            PropertyResponse property = propertyMapper.selectResponseById(propertyGroup, propertyId);
            if (property != null
                    && property.getDefaultValue() != null
                    && !property.getDefaultValue().isBlank()) {
                return property.getDefaultValue();
            }
        } catch (Exception e) {
            log.warn("Management property 조회 실패, 기본값 사용: instanceId={}, suffix={}", instanceId, suffix, e);
        }
        return defaultValue;
    }
}
