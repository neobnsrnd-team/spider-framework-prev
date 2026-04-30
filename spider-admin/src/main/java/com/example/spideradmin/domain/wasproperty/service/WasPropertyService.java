package com.example.spideradmin.domain.wasproperty.service;

import com.example.spideradmin.domain.wasproperty.dto.*;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyBatchSaveRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyCreateRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertySaveRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyUpdateRequest;
import com.example.spideradmin.domain.wasproperty.mapper.WasPropertyHistoryMapper;
import com.example.spideradmin.domain.wasproperty.mapper.WasPropertyMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.SecurityUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasPropertyService {

    private final WasPropertyMapper wasPropertyMapper;
    private final WasPropertyHistoryMapper wasPropertyHistoryMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public WasPropertyResponse getPropertyById(String instanceId, String propertyGroupId, String propertyId) {
        WasPropertyResponse property = wasPropertyMapper.selectResponseById(instanceId, propertyGroupId, propertyId);
        if (property == null) {
            throw new NotFoundException(String.format(
                    "instanceId: %s, groupId: %s, propertyId: %s", instanceId, propertyGroupId, propertyId));
        }
        return property;
    }

    public List<Map<String, Object>> getPropertiesByInstanceWithDefaults(String instanceId) {
        try {
            List<Map<String, Object>> properties = wasPropertyMapper.selectByInstanceIdWithDefaults(instanceId);
            if (properties != null && !properties.isEmpty()) {
                return properties;
            }
            log.warn("No properties found with defaults for instance: {}", instanceId);
            return properties;
        } catch (Exception e) {
            log.warn("Fallback to simple query for instance: {}", instanceId, e);
            List<WasPropertyResponse> simpleProps = wasPropertyMapper.selectByInstanceId(instanceId);
            return simpleProps.stream()
                    .map(prop -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("instanceId", prop.getInstanceId());
                        map.put("propertyGroupId", prop.getPropertyGroupId());
                        map.put("propertyId", prop.getPropertyId());
                        map.put("propertyValue", prop.getPropertyValue());
                        map.put("propertyDesc", prop.getPropertyDesc());
                        map.put("curVersion", prop.getCurVersion());
                        map.put("defaultValue", "USE_DEFAULT");
                        map.put("propertyName", "");
                        map.put("dataType", "");
                        return map;
                    })
                    .toList();
        }
    }

    public List<WasPropertyResponse> getPropertiesByInstance(String instanceId) {
        return wasPropertyMapper.selectByInstanceId(instanceId);
    }

    public PageResponse<WasPropertyWithDefaultResponse> getPropertiesByInstancePaging(
            String instanceId, String propertyGroupId, PageRequest pageRequest) {
        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        List<WasPropertyWithDefaultResponse> content = wasPropertyMapper.selectByInstanceIdPaging(
                instanceId, propertyGroupId, offset, size, pageRequest.getSortBy(), pageRequest.getSortDirection());
        long totalCount = wasPropertyMapper.countByInstanceId(instanceId, propertyGroupId);

        return PageResponse.of(content, totalCount, page, size);
    }

    @Transactional
    public WasPropertyResponse createProperty(WasPropertyCreateRequest dto, String userId) {
        if (wasPropertyMapper.countById(dto.getInstanceId(), dto.getPropertyGroupId(), dto.getPropertyId()) > 0) {
            throw new DuplicateException(String.format(
                    "instanceId: %s, groupId: %s, propertyId: %s",
                    dto.getInstanceId(), dto.getPropertyGroupId(), dto.getPropertyId()));
        }

        wasPropertyMapper.insert(
                dto.getInstanceId(),
                dto.getPropertyGroupId(),
                dto.getPropertyId(),
                dto.getPropertyValue(),
                dto.getPropertyDesc());

        return wasPropertyMapper.selectResponseById(dto.getInstanceId(), dto.getPropertyGroupId(), dto.getPropertyId());
    }

    @Transactional
    public WasPropertyResponse updateProperty(
            String instanceId,
            String propertyGroupId,
            String propertyId,
            WasPropertyUpdateRequest dto,
            String reason,
            String userId) {
        if (wasPropertyMapper.countById(instanceId, propertyGroupId, propertyId) == 0) {
            throw new NotFoundException(String.format(
                    "instanceId: %s, groupId: %s, propertyId: %s", instanceId, propertyGroupId, propertyId));
        }

        int updatedRows = wasPropertyMapper.update(
                instanceId, propertyGroupId, propertyId, dto.getPropertyValue(), dto.getPropertyDesc());

        if (updatedRows == 0) {
            throw new NotFoundException(String.format(
                    "instanceId: %s, groupId: %s, propertyId: %s", instanceId, propertyGroupId, propertyId));
        }

        return wasPropertyMapper.selectResponseById(instanceId, propertyGroupId, propertyId);
    }

    @Transactional
    public void deleteProperty(String instanceId, String propertyGroupId, String propertyId) {
        if (wasPropertyMapper.countById(instanceId, propertyGroupId, propertyId) == 0) {
            throw new NotFoundException(String.format(
                    "instanceId: %s, groupId: %s, propertyId: %s", instanceId, propertyGroupId, propertyId));
        }
        wasPropertyMapper.deleteById(instanceId, propertyGroupId, propertyId);
    }

    public WasPropertyCompareResponse compareProperties(
            String instanceId1, String instanceId2, List<String> propertyGroupIds) {
        List<WasPropertyResponse> props1 = wasPropertyMapper.selectByInstanceId(instanceId1);
        List<WasPropertyResponse> props2 = wasPropertyMapper.selectByInstanceId(instanceId2);

        if (propertyGroupIds != null && !propertyGroupIds.isEmpty()) {
            props1 = props1.stream()
                    .filter(p -> propertyGroupIds.contains(p.getPropertyGroupId()))
                    .toList();
            props2 = props2.stream()
                    .filter(p -> propertyGroupIds.contains(p.getPropertyGroupId()))
                    .toList();
        }

        java.util.Map<String, WasPropertyResponse> map1 = props1.stream()
                .collect(Collectors.toMap(p -> p.getPropertyGroupId() + ":" + p.getPropertyId(), p -> p));
        java.util.Map<String, WasPropertyResponse> map2 = props2.stream()
                .collect(Collectors.toMap(p -> p.getPropertyGroupId() + ":" + p.getPropertyId(), p -> p));

        java.util.Set<String> allKeys = new java.util.HashSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        List<WasPropertyCompareResponse.PropertyCompareItem> items = new java.util.ArrayList<>();
        int differentCount = 0;

        for (String key : allKeys) {
            WasPropertyCompareResponse.PropertyCompareItem item = buildCompareItem(key, map1, map2);
            items.add(item);
            if ("DIFFERENT".equals(item.getCompareResult())) {
                differentCount++;
            }
        }

        items.sort(java.util.Comparator.comparing(WasPropertyCompareResponse.PropertyCompareItem::getPropertyGroupId)
                .thenComparing(WasPropertyCompareResponse.PropertyCompareItem::getPropertyId));

        return WasPropertyCompareResponse.builder()
                .instanceId1(instanceId1)
                .instanceId2(instanceId2)
                .items(items)
                .differentCount(differentCount)
                .totalCount(items.size())
                .build();
    }

    private WasPropertyCompareResponse.PropertyCompareItem buildCompareItem(
            String key,
            java.util.Map<String, WasPropertyResponse> map1,
            java.util.Map<String, WasPropertyResponse> map2) {
        WasPropertyResponse p1 = map1.get(key);
        WasPropertyResponse p2 = map2.get(key);

        String[] parts = key.split(":");
        String groupId = parts[0];
        String propId = parts[1];

        String value1 = p1 != null ? p1.getPropertyValue() : "NO DATA";
        String value2 = p2 != null ? p2.getPropertyValue() : "NO DATA";

        String compareResult = value1.equals(value2) ? "SAME" : "DIFFERENT";
        String defaultValue = p1 != null ? p1.getPropertyValue() : value2;

        return WasPropertyCompareResponse.PropertyCompareItem.builder()
                .propertyGroupId(groupId)
                .propertyId(propId)
                .defaultValue(defaultValue)
                .compareResult(compareResult)
                .value1(value1)
                .value2(value2)
                .build();
    }

    @Transactional
    public int copyProperties(
            String sourceInstanceId,
            String targetInstanceId,
            List<String> propertyGroupIds,
            Boolean overwrite,
            String reason,
            String userId) {
        List<WasPropertyResponse> sourceProps = wasPropertyMapper.selectByInstanceId(sourceInstanceId);

        if (propertyGroupIds != null && !propertyGroupIds.isEmpty()) {
            sourceProps = sourceProps.stream()
                    .filter(p -> propertyGroupIds.contains(p.getPropertyGroupId()))
                    .toList();
        }

        int copiedCount = 0;
        for (WasPropertyResponse sourceProp : sourceProps) {
            if (copySingleProperty(sourceProp, targetInstanceId, overwrite)) {
                copiedCount++;
            }
        }

        return copiedCount;
    }

    private boolean copySingleProperty(WasPropertyResponse sourceProp, String targetInstanceId, Boolean overwrite) {
        boolean targetExists = wasPropertyMapper.countById(
                        targetInstanceId, sourceProp.getPropertyGroupId(), sourceProp.getPropertyId())
                > 0;

        if (!targetExists) {
            wasPropertyMapper.insert(
                    targetInstanceId,
                    sourceProp.getPropertyGroupId(),
                    sourceProp.getPropertyId(),
                    sourceProp.getPropertyValue(),
                    sourceProp.getPropertyDesc());
            return true;
        }

        if (Boolean.TRUE.equals(overwrite)) {
            int updatedRows = wasPropertyMapper.update(
                    targetInstanceId,
                    sourceProp.getPropertyGroupId(),
                    sourceProp.getPropertyId(),
                    sourceProp.getPropertyValue(),
                    sourceProp.getPropertyDesc());
            return updatedRows > 0;
        }

        return false;
    }

    @Transactional
    public int batchSaveProperties(List<WasPropertyBatchSaveRequest> requests, String userId) {
        int processedCount = 0;
        for (var request : requests) {
            if (processBatchItem(request)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    private boolean processBatchItem(WasPropertyBatchSaveRequest request) {
        String instanceId = request.getInstanceId();
        String propertyGroupId = request.getPropertyGroupId();
        String propertyId = request.getPropertyId();

        return switch (request.getCrud()) {
            case "C" -> {
                if (wasPropertyMapper.countById(instanceId, propertyGroupId, propertyId) == 0) {
                    wasPropertyMapper.insert(
                            instanceId,
                            propertyGroupId,
                            propertyId,
                            request.getPropertyValue(),
                            request.getPropertyDesc());
                    yield true;
                }
                yield false;
            }
            case "U" -> {
                if (wasPropertyMapper.countById(instanceId, propertyGroupId, propertyId) > 0) {
                    wasPropertyMapper.update(
                            instanceId,
                            propertyGroupId,
                            propertyId,
                            request.getPropertyValue(),
                            request.getPropertyDesc());
                    yield true;
                }
                yield false;
            }
            case "D" -> {
                wasPropertyMapper.deleteById(instanceId, propertyGroupId, propertyId);
                yield true;
            }
            default -> {
                log.warn("Unknown CRUD action: {}", request.getCrud());
                yield false;
            }
        };
    }

    // ============================ Property 도메인에서 이동된 메서드 [S] ============================

    public List<WasPropertyForPropertyResponse> getWasPropertiesByProperty(String propertyGroupId, String propertyId) {
        return wasPropertyMapper.selectWasPropertiesByProperty(propertyGroupId, propertyId);
    }

    @Transactional
    public int saveWasProperties(List<WasPropertySaveRequest> wasProperties) {
        if (wasProperties == null || wasProperties.isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        for (WasPropertySaveRequest dto : wasProperties) {
            wasPropertyMapper.mergeProperty(
                    dto.getInstanceId(),
                    dto.getPropertyGroupId(),
                    dto.getPropertyId(),
                    dto.getPropertyValue(),
                    dto.getPropertyDesc());
            processedCount++;
        }

        return processedCount;
    }

    @Transactional
    public void deleteByGroupAndProperty(String propertyGroupId, String propertyId) {
        wasPropertyMapper.deleteByGroupAndProperty(propertyGroupId, propertyId);
    }

    @Transactional
    public void deleteByPropertyGroupId(String propertyGroupId) {
        wasPropertyMapper.deleteByPropertyGroupId(propertyGroupId);
    }

    @Transactional
    public void deleteByInstanceId(String instanceId) {
        wasPropertyMapper.deleteByInstanceId(instanceId);
    }

    // ============================ WAS 인스턴스 목록 ============================

    public List<WasInstanceSimpleResponse> getWasInstances() {
        return wasPropertyMapper.selectAllInstances();
    }

    // ============================ WAS별 설정 백업/복원 [S] ============================

    @Transactional
    public void backupWasProperties(String propertyGroupId, List<String> instanceIds, String reason) {
        String now = LocalDateTime.now().format(FORMATTER);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();

        for (String instanceId : instanceIds) {
            List<WasPropertyHistoryResponse> currentProps =
                    wasPropertyHistoryMapper.selectCurrentProperties(instanceId, propertyGroupId);
            if (currentProps == null || currentProps.isEmpty()) {
                continue;
            }

            Integer maxVersion = wasPropertyHistoryMapper.selectMaxVersion(instanceId, propertyGroupId);
            int newVersion = (maxVersion == null ? 0 : maxVersion) + 1;

            List<WasPropertyHistoryResponse> histories = currentProps.stream()
                    .map(prop -> WasPropertyHistoryResponse.builder()
                            .instanceId(instanceId)
                            .propertyGroupId(propertyGroupId)
                            .propertyId(prop.getPropertyId())
                            .version(newVersion)
                            .propertyValue(prop.getPropertyValue())
                            .propertyDesc(prop.getPropertyDesc())
                            .reason(reason)
                            .lastUpdateUserId(userId)
                            .lastUpdateDtime(now)
                            .build())
                    .toList();

            wasPropertyHistoryMapper.insertBatchHistory(histories);
            wasPropertyHistoryMapper.incrementVersion(instanceId, propertyGroupId);
        }
    }

    public List<WasPropertyHistoryVersionResponse> getWasHistoryVersions(String propertyGroupId, String instanceId) {
        List<WasPropertyHistoryResponse> versions =
                wasPropertyHistoryMapper.selectVersions(instanceId, propertyGroupId);
        return versions.stream().map(this::toWasHistoryVersionDTO).toList();
    }

    public List<WasPropertyHistoryResponse> getWasHistoryByVersion(
            String propertyGroupId, String instanceId, int version) {
        return wasPropertyHistoryMapper.selectHistoryByVersion(instanceId, propertyGroupId, version);
    }

    public List<WasPropertyHistoryResponse> getCurrentWasProperties(String instanceId, String propertyGroupId) {
        return wasPropertyHistoryMapper.selectCurrentProperties(instanceId, propertyGroupId);
    }

    @Transactional
    public void restoreWasProperties(String instanceId, String propertyGroupId, int version) {
        List<WasPropertyHistoryResponse> histories =
                wasPropertyHistoryMapper.selectHistoryByVersion(instanceId, propertyGroupId, version);
        if (histories == null || histories.isEmpty()) {
            throw new NotFoundException(
                    "instanceId: " + instanceId + ", groupId: " + propertyGroupId + ", version: " + version);
        }

        wasPropertyHistoryMapper.deleteByInstanceAndGroup(instanceId, propertyGroupId);

        List<WasPropertyHistoryResponse> restoreData = histories.stream()
                .map(h -> WasPropertyHistoryResponse.builder()
                        .instanceId(h.getInstanceId())
                        .propertyGroupId(h.getPropertyGroupId())
                        .propertyId(h.getPropertyId())
                        .version(h.getVersion())
                        .propertyValue(h.getPropertyValue())
                        .propertyDesc(h.getPropertyDesc())
                        .build())
                .toList();

        wasPropertyHistoryMapper.insertBatchProperty(restoreData);
    }

    // ============================ WAS별 설정 백업/복원 [E] ============================

    private WasPropertyHistoryVersionResponse toWasHistoryVersionDTO(WasPropertyHistoryResponse response) {
        return WasPropertyHistoryVersionResponse.builder()
                .version(response.getVersion())
                .reason(response.getReason())
                .lastUpdateUserId(response.getLastUpdateUserId())
                .lastUpdateDtime(response.getLastUpdateDtime())
                .build();
    }
}
