package com.example.spideradmin.domain.property.service;

import com.example.spideradmin.domain.property.dto.PropertyHistoryResponse;
import com.example.spideradmin.domain.property.dto.PropertyHistoryVersionResponse;
import com.example.spideradmin.domain.property.dto.PropertyResponse;
import com.example.spideradmin.domain.property.mapper.PropertyHistoryMapper;
import com.example.spideradmin.domain.property.mapper.PropertyMapper;
import com.example.spideradmin.domain.wasproperty.service.WasPropertyService;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로퍼티 이력(백업/복원) 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyHistoryService {

    private final PropertyMapper propertyMapper;
    private final PropertyHistoryMapper propertyHistoryMapper;
    private final WasPropertyService wasPropertyService;

    // ============================ 전체 백업/복원 [S] =====================================
    @Transactional
    public void backupPropertyGroup(String propertyGroupId, String reason) {
        // 1. 현재 프로퍼티 목록 조회
        List<PropertyResponse> currentProperties = propertyHistoryMapper.selectByPropertyGroupId(propertyGroupId);
        if (currentProperties == null || currentProperties.isEmpty()) {
            throw new InvalidInputException("propertyGroupId: " + propertyGroupId);
        }

        // 2. 최대 버전 조회
        Integer maxVersion = propertyHistoryMapper.selectMaxVersionByGroupId(propertyGroupId);
        int newVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 3. FWK_PROPERTY_HISTORY에 현재 프로퍼티 데이터 저장
        propertyHistoryMapper.insertBatchHistory(currentProperties, newVersion, reason, now, userId);

        // 4. FWK_PROPERTY의 CUR_VERSION 일괄 증가
        propertyHistoryMapper.incrementVersionByGroupId(propertyGroupId);

        log.info(
                "프로퍼티 그룹 백업 완료: groupId={}, version={}, propertyCount={}",
                propertyGroupId,
                newVersion,
                currentProperties.size());
    }

    public List<PropertyHistoryVersionResponse> getHistoryVersions(String propertyGroupId) {
        return propertyHistoryMapper.selectVersionsByGroupId(propertyGroupId);
    }

    public List<PropertyHistoryResponse> getHistoryByVersion(String propertyGroupId, int version) {
        return propertyHistoryMapper.selectHistoryByGroupIdAndVersion(propertyGroupId, version);
    }

    @Transactional
    public void restorePropertyGroup(String propertyGroupId, int version) {
        // 1. 복원할 히스토리 데이터 조회
        List<PropertyHistoryResponse> histories =
                propertyHistoryMapper.selectHistoryByGroupIdAndVersion(propertyGroupId, version);
        if (histories == null || histories.isEmpty()) {
            throw new NotFoundException("propertyGroupId: " + propertyGroupId + ", version: " + version);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 2. 기존 FWK_WAS_PROPERTY 삭제
        wasPropertyService.deleteByPropertyGroupId(propertyGroupId);

        // 3. 기존 FWK_PROPERTY 삭제
        propertyMapper.deleteByPropertyGroupId(propertyGroupId);

        // 4. 히스토리 데이터로 FWK_PROPERTY에 INSERT
        propertyHistoryMapper.insertBatchFromHistory(histories, now, userId);

        log.info("프로퍼티 그룹 복원 완료: groupId={}, version={}, propertyCount={}", propertyGroupId, version, histories.size());
    }
    // ============================ 전체 백업/복원 [E] =====================================

}
