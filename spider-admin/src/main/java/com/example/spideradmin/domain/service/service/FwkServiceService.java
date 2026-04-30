package com.example.spideradmin.domain.service.service;

import com.example.spideradmin.domain.service.dto.FwkServiceCreateRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceDetailResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceSearchRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceUpdateRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceUseYnBulkRequest;
import com.example.spideradmin.domain.service.dto.WorkSpaceResponse;
import com.example.spideradmin.domain.service.mapper.FwkServiceMapper;
import com.example.spideradmin.domain.service.mapper.FwkServiceRelationMapper;
import com.example.spideradmin.global.aop.WorkListRecord;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서비스 관리 Service (FWK_SERVICE CRUD + USE_YN 일괄 변경 + Excel 내보내기) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FwkServiceService {

    private final FwkServiceMapper fwkServiceMapper;
    private final FwkServiceRelationMapper fwkServiceRelationMapper;

    /** 검색 조건으로 서비스 목록 조회 (페이지네이션) */
    public PageResponse<FwkServiceResponse> getServicesWithSearch(FwkServiceSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = fwkServiceMapper.countAllWithSearch(
                searchDTO.getServiceId(),
                searchDTO.getServiceName(),
                searchDTO.getServiceType(),
                searchDTO.getUseYn(),
                searchDTO.getBizGroupId(),
                searchDTO.getReqChannelCode(),
                searchDTO.getComponentId(),
                searchDTO.getComponentName(),
                searchDTO.getLoginOnlyYn(),
                searchDTO.getSecureSignYn(),
                searchDTO.getBankStatusCheckYn(),
                searchDTO.getBizdayServiceYn(),
                searchDTO.getSaturdayServiceYn(),
                searchDTO.getHolidayServiceYn());

        List<FwkServiceResponse> services = fwkServiceMapper.findAllWithSearch(
                searchDTO.getServiceId(),
                searchDTO.getServiceName(),
                searchDTO.getServiceType(),
                searchDTO.getUseYn(),
                searchDTO.getBizGroupId(),
                searchDTO.getReqChannelCode(),
                searchDTO.getComponentId(),
                searchDTO.getComponentName(),
                searchDTO.getLoginOnlyYn(),
                searchDTO.getSecureSignYn(),
                searchDTO.getBankStatusCheckYn(),
                searchDTO.getBizdayServiceYn(),
                searchDTO.getSaturdayServiceYn(),
                searchDTO.getHolidayServiceYn(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(services, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 서비스 단건 상세 조회 (연결 컴포넌트 포함) */
    public FwkServiceDetailResponse getById(String serviceId) {
        FwkServiceResponse base = fwkServiceMapper.selectResponseById(serviceId);
        if (base == null) {
            throw new NotFoundException("serviceId: " + serviceId);
        }
        return toDetailResponse(base, serviceId);
    }

    /** 서비스 등록 */
    @WorkListRecord(
            workIdExpression = "#dto.serviceType",
            crudType = "C",
            pkExpression = "#dto.serviceId",
            workName = "서비스")
    @Transactional
    public FwkServiceDetailResponse create(FwkServiceCreateRequest dto) {
        try {
            fwkServiceMapper.insertFwkService(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("serviceId: " + dto.getServiceId());
        }
        return getById(dto.getServiceId());
    }

    /** 서비스 수정 */
    @WorkListRecord(
            workIdExpression = "#dto.serviceType",
            crudType = "U",
            pkExpression = "#serviceId",
            workName = "서비스")
    @Transactional
    public FwkServiceDetailResponse update(String serviceId, FwkServiceUpdateRequest dto) {
        if (fwkServiceMapper.countById(serviceId) == 0) {
            throw new NotFoundException("serviceId: " + serviceId);
        }
        fwkServiceMapper.updateFwkService(serviceId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        return getById(serviceId);
    }

    /**
     * 서비스 삭제 (3계층 FK 순서 준수).
     *
     * <p>삭제 순서: FWK_RELATION_PARAM → FWK_SERVICE_RELATION → FWK_SERVICE
     * serviceType은 WorkListRecord 이력 적재에 사용된다 (컨트롤러에서 조회 후 전달).
     */
    @WorkListRecord(workIdExpression = "#serviceType", crudType = "D", pkExpression = "#serviceId", workName = "서비스")
    @Transactional
    public void delete(String serviceId, String serviceType) {
        if (fwkServiceMapper.countById(serviceId) == 0) {
            throw new NotFoundException("serviceId: " + serviceId);
        }
        fwkServiceRelationMapper.deleteParamsByServiceId(serviceId);
        fwkServiceRelationMapper.deleteRelationsByServiceId(serviceId);
        fwkServiceMapper.deleteById(serviceId);
    }

    /** USE_YN 일괄 변경 */
    @Transactional
    public void bulkUpdateUseYn(FwkServiceUseYnBulkRequest dto) {
        fwkServiceMapper.updateUseYnBatch(
                dto.getServiceIds(), dto.getUseYn(), AuditUtil.now(), AuditUtil.currentUserId());
    }

    /** 서비스 목록 Excel 내보내기 */
    public byte[] exportFwkServices(FwkServiceSearchRequest searchDTO) throws IOException {
        List<FwkServiceResponse> data = fwkServiceMapper.findAllWithSearch(
                searchDTO.getServiceId(),
                searchDTO.getServiceName(),
                searchDTO.getServiceType(),
                searchDTO.getUseYn(),
                searchDTO.getBizGroupId(),
                searchDTO.getReqChannelCode(),
                searchDTO.getComponentId(),
                searchDTO.getComponentName(),
                searchDTO.getLoginOnlyYn(),
                searchDTO.getSecureSignYn(),
                searchDTO.getBankStatusCheckYn(),
                searchDTO.getBizdayServiceYn(),
                searchDTO.getSaturdayServiceYn(),
                searchDTO.getHolidayServiceYn(),
                "serviceId",
                "ASC",
                0,
                ExcelExportUtil.MAX_ROW_LIMIT);

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("서비스 ID", 20, "serviceId"),
                new ExcelColumnDefinition("서비스명", 30, "serviceName"),
                new ExcelColumnDefinition("서비스 유형", 10, "serviceType"),
                new ExcelColumnDefinition("클래스명", 35, "className"),
                new ExcelColumnDefinition("메서드명", 20, "methodName"),
                new ExcelColumnDefinition("Biz Group ID", 15, "bizGroupId"),
                new ExcelColumnDefinition("사용여부", 8, "useYn"),
                new ExcelColumnDefinition("최종수정일시", 16, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = data.stream()
                .map(s -> Map.<String, Object>of(
                        "serviceId", nullToEmpty(s.getServiceId()),
                        "serviceName", nullToEmpty(s.getServiceName()),
                        "serviceType", nullToEmpty(s.getServiceType()),
                        "className", nullToEmpty(s.getClassName()),
                        "methodName", nullToEmpty(s.getMethodName()),
                        "bizGroupId", nullToEmpty(s.getBizGroupId()),
                        "useYn", nullToEmpty(s.getUseYn()),
                        "lastUpdateDtime", nullToEmpty(s.getLastUpdateDtime()),
                        "lastUpdateUserId", nullToEmpty(s.getLastUpdateUserId())))
                .toList();

        return ExcelExportUtil.createWorkbook(
                ExcelExportUtil.generateFileName("FwkService", LocalDate.now()), columns, rows);
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────

    private FwkServiceDetailResponse toDetailResponse(FwkServiceResponse base, String serviceId) {
        return FwkServiceDetailResponse.builder()
                .serviceId(base.getServiceId())
                .serviceName(base.getServiceName())
                .serviceDesc(base.getServiceDesc())
                .className(base.getClassName())
                .methodName(base.getMethodName())
                .serviceType(base.getServiceType())
                .bizGroupId(base.getBizGroupId())
                .orgId(base.getOrgId())
                .ioType(base.getIoType())
                .workSpaceId(base.getWorkSpaceId())
                .trxId(base.getTrxId())
                .useYn(base.getUseYn())
                .preProcessAppId(base.getPreProcessAppId())
                .postProcessAppId(base.getPostProcessAppId())
                .timeCheckYn(base.getTimeCheckYn())
                .startTime(base.getStartTime())
                .endTime(base.getEndTime())
                .bizdayServiceYn(base.getBizdayServiceYn())
                .bizdayStartTime(base.getBizdayStartTime())
                .bizdayEndTime(base.getBizdayEndTime())
                .saturdayServiceYn(base.getSaturdayServiceYn())
                .saturdayStartTime(base.getSaturdayStartTime())
                .saturdayEndTime(base.getSaturdayEndTime())
                .holidayServiceYn(base.getHolidayServiceYn())
                .holidayStartTime(base.getHolidayStartTime())
                .holidayEndTime(base.getHolidayEndTime())
                .loginOnlyYn(base.getLoginOnlyYn())
                .secureSignYn(base.getSecureSignYn())
                .reqChannelCode(base.getReqChannelCode())
                .bankStatusCheckYn(base.getBankStatusCheckYn())
                .bankCodeField(base.getBankCodeField())
                .lastUpdateDtime(base.getLastUpdateDtime())
                .lastUpdateUserId(base.getLastUpdateUserId())
                .relations(fwkServiceRelationMapper.findRelationsByServiceId(serviceId))
                .build();
    }

    // ─── WorkSpace 팝업 ───────────────────────────────────────────────────

    public List<WorkSpaceResponse> getWorkspacePage(String workSpaceId, String workSpaceName, int offset, int limit) {
        return fwkServiceMapper.selectWorkspacePage(workSpaceId, workSpaceName, offset, limit);
    }

    public int countWorkspace(String workSpaceId, String workSpaceName) {
        return fwkServiceMapper.countWorkspace(workSpaceId, workSpaceName);
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
