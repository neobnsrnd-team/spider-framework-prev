package com.example.spider_admin.domain.property.service;

import com.example.spider_admin.domain.code.dto.CodeCreateRequest;
import com.example.spider_admin.domain.code.mapper.CodeMapper;
import com.example.spider_admin.domain.property.dto.PropertyGroupCreateRequest;
import com.example.spider_admin.domain.property.dto.PropertyGroupResponse;
import com.example.spider_admin.domain.property.dto.PropertyResponse;
import com.example.spider_admin.domain.property.dto.PropertySaveRequest;
import com.example.spider_admin.domain.property.mapper.PropertyMapper;
import com.example.spider_admin.domain.wasproperty.service.WasPropertyService;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로퍼티 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private static final String PROPERTY_GROUP_CODE_GROUP_ID = "FR00006";
    private final PropertyMapper propertyMapper;
    private final CodeMapper codeMapper;
    private final WasPropertyService wasPropertyService;

    public PageResponse<PropertyGroupResponse> getPropertyGroups(PageRequest pageRequest) {
        long total = propertyMapper.countPropertyGroupsWithSearch(
                pageRequest.getSearchField(), pageRequest.getSearchValue());

        List<PropertyGroupResponse> propertyGroups = propertyMapper.selectPropertyGroups(
                pageRequest.getSearchField(),
                pageRequest.getSearchValue(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(propertyGroups, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<PropertyGroupResponse> getAllPropertyGroups() {
        return propertyMapper.selectAllPropertyGroups();
    }

    public List<PropertyResponse> getPropertiesByGroupId(
            String propertyGroupId, String searchField, String searchValue) {
        return propertyMapper.selectPropertiesByGroupId(propertyGroupId, searchField, searchValue);
    }

    public boolean existsPropertyGroup(String propertyGroupId) {
        return codeMapper.countByCodeGroupIdAndCode(PROPERTY_GROUP_CODE_GROUP_ID, propertyGroupId) > 0;
    }

    public byte[] exportPropertyGroups(String searchField, String searchValue, String sortBy, String sortDirection) {
        List<PropertyGroupResponse> data =
                propertyMapper.findAllForExport(searchField, searchValue, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("프로퍼티그룹ID", 20, "propertyGroupId"),
                new ExcelColumnDefinition("프로퍼티그룹명", 25, "propertyGroupName"),
                new ExcelColumnDefinition("프로퍼티 수", 12, "propertyCount"),
                new ExcelColumnDefinition("최종수정일시", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("propertyGroupId", item.getPropertyGroupId());
            row.put("propertyGroupName", item.getPropertyGroupName());
            row.put("propertyCount", item.getPropertyCount());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("프로퍼티DB", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public void createPropertyGroup(PropertyGroupCreateRequest requestDTO) {
        // 1. 중복 확인
        if (existsPropertyGroup(requestDTO.getPropertyGroupId())) {
            throw new DuplicateException("propertyGroupId: " + requestDTO.getPropertyGroupId());
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 2. FWK_CODE에 프로퍼티 그룹 코드 등록
        CodeCreateRequest codeRequest = CodeCreateRequest.builder()
                .codeGroupId(PROPERTY_GROUP_CODE_GROUP_ID)
                .code(requestDTO.getPropertyGroupId())
                .codeName(requestDTO.getPropertyGroupName())
                .codeDesc(requestDTO.getPropertyGroupId())
                .useYn("Y")
                .sortOrder(0)
                .build();
        codeMapper.insert(codeRequest, now, userId);

        // 3. FWK_PROPERTY에 하위 프로퍼티 일괄 등록
        propertyMapper.insertBatch(requestDTO.getProperties(), requestDTO.getPropertyGroupId(), now, userId);

        log.info(
                "프로퍼티 그룹 생성 완료: groupId={}, propertyCount={}",
                requestDTO.getPropertyGroupId(),
                requestDTO.getProperties().size());
    }

    @Transactional
    public int saveProperties(List<PropertySaveRequest> properties) {
        if (properties == null || properties.isEmpty()) {
            return 0;
        }

        String currentUserId = AuditUtil.currentUserId();
        String currentDtime = AuditUtil.now();
        int processedCount = 0;

        for (PropertySaveRequest dto : properties) {
            String crud = dto.getCrud();

            switch (crud) {
                case "C":
                    // 중복 체크
                    int existCount = propertyMapper.countById(dto.getPropertyGroupId(), dto.getPropertyId());
                    if (existCount > 0) {
                        log.warn(
                                "Property already exists: groupId={}, propertyId={}",
                                dto.getPropertyGroupId(),
                                dto.getPropertyId());
                        continue;
                    }

                    propertyMapper.insert(dto, currentDtime, currentUserId);
                    processedCount++;
                    log.info(
                            "Property created: groupId={}, propertyId={}",
                            dto.getPropertyGroupId(),
                            dto.getPropertyId());
                    break;

                case "U":
                    int updateExistCount = propertyMapper.countById(dto.getPropertyGroupId(), dto.getPropertyId());
                    if (updateExistCount == 0) {
                        log.warn(
                                "Property not found for update: groupId={}, propertyId={}",
                                dto.getPropertyGroupId(),
                                dto.getPropertyId());
                        continue;
                    }

                    propertyMapper.update(dto, currentDtime, currentUserId);
                    processedCount++;
                    log.info(
                            "Property updated: groupId={}, propertyId={}",
                            dto.getPropertyGroupId(),
                            dto.getPropertyId());
                    break;

                case "D":
                    // FWK_WAS_PROPERTY 테이블에서 연관 데이터 먼저 삭제
                    wasPropertyService.deleteByGroupAndProperty(dto.getPropertyGroupId(), dto.getPropertyId());
                    log.info(
                            "WAS properties deleted: groupId={}, propertyId={}",
                            dto.getPropertyGroupId(),
                            dto.getPropertyId());

                    // FWK_PROPERTY 테이블에서 삭제
                    propertyMapper.delete(dto.getPropertyGroupId(), dto.getPropertyId());
                    processedCount++;
                    log.info(
                            "Property deleted: groupId={}, propertyId={}",
                            dto.getPropertyGroupId(),
                            dto.getPropertyId());
                    break;

                default:
                    log.warn("Unknown CRUD type: {}", crud);
            }
        }

        return processedCount;
    }

    @Transactional
    public void deletePropertyGroup(String propertyGroupId) {
        // 1. 프로퍼티 그룹 존재 여부 확인
        if (!existsPropertyGroup(propertyGroupId)) {
            throw new NotFoundException("propertyGroupId: " + propertyGroupId);
        }

        // 2. FWK_WAS_PROPERTY 테이블에서 해당 그룹의 모든 WAS 프로퍼티 삭제
        wasPropertyService.deleteByPropertyGroupId(propertyGroupId);
        log.info("WAS 프로퍼티 삭제 완료: groupId={}", propertyGroupId);

        // 3. FWK_PROPERTY 테이블에서 해당 그룹의 모든 프로퍼티 삭제
        propertyMapper.deleteByPropertyGroupId(propertyGroupId);
        log.info("프로퍼티 삭제 완료: groupId={}", propertyGroupId);

        // 4. FWK_CODE 테이블에서 해당 그룹 코드 삭제 (code_group_id='FR00006', code=propertyGroupId)
        codeMapper.deleteById(PROPERTY_GROUP_CODE_GROUP_ID, propertyGroupId);
        log.info("프로퍼티 그룹 코드 삭제 완료: groupId={}", propertyGroupId);
    }
}
