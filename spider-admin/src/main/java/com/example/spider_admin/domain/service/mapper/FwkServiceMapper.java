package com.example.spider_admin.domain.service.mapper;

import com.example.spider_admin.domain.service.dto.FwkServiceCreateRequest;
import com.example.spider_admin.domain.service.dto.FwkServiceResponse;
import com.example.spider_admin.domain.service.dto.FwkServiceUpdateRequest;
import com.example.spider_admin.domain.service.dto.WorkSpaceResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 서비스 Mapper (FWK_SERVICE CRUD + 페이지네이션 + USE_YN 일괄 변경) */
@Mapper
public interface FwkServiceMapper {

    // ─── 마스터 조회 ──────────────────────────────────────────────────

    /** 서비스 ID로 단건 조회 */
    FwkServiceResponse selectResponseById(@Param("serviceId") String serviceId);

    /** 서비스 ID 존재 확인용 카운트 */
    int countById(@Param("serviceId") String serviceId);

    // ─── 마스터 CRUD ──────────────────────────────────────────────────

    /** 서비스 등록 */
    void insertFwkService(
            @Param("dto") FwkServiceCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** 서비스 수정 */
    void updateFwkService(
            @Param("serviceId") String serviceId,
            @Param("dto") FwkServiceUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** 서비스 삭제 */
    void deleteById(@Param("serviceId") String serviceId);

    // ─── 목록 조회 ────────────────────────────────────────────────────

    /** 검색 조건으로 서비스 목록 조회 (페이지네이션) */
    @SuppressWarnings("java:S107")
    List<FwkServiceResponse> findAllWithSearch(
            @Param("serviceId") String serviceId,
            @Param("serviceName") String serviceName,
            @Param("serviceType") String serviceType,
            @Param("useYn") String useYn,
            @Param("bizGroupId") String bizGroupId,
            @Param("reqChannelCode") String reqChannelCode,
            @Param("componentId") String componentId,
            @Param("componentName") String componentName,
            @Param("loginOnlyYn") String loginOnlyYn,
            @Param("secureSignYn") String secureSignYn,
            @Param("bankStatusCheckYn") String bankStatusCheckYn,
            @Param("bizdayServiceYn") String bizdayServiceYn,
            @Param("saturdayServiceYn") String saturdayServiceYn,
            @Param("holidayServiceYn") String holidayServiceYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /** 검색 조건으로 서비스 전체 건수 조회 */
    @SuppressWarnings("java:S107")
    long countAllWithSearch(
            @Param("serviceId") String serviceId,
            @Param("serviceName") String serviceName,
            @Param("serviceType") String serviceType,
            @Param("useYn") String useYn,
            @Param("bizGroupId") String bizGroupId,
            @Param("reqChannelCode") String reqChannelCode,
            @Param("componentId") String componentId,
            @Param("componentName") String componentName,
            @Param("loginOnlyYn") String loginOnlyYn,
            @Param("secureSignYn") String secureSignYn,
            @Param("bankStatusCheckYn") String bankStatusCheckYn,
            @Param("bizdayServiceYn") String bizdayServiceYn,
            @Param("saturdayServiceYn") String saturdayServiceYn,
            @Param("holidayServiceYn") String holidayServiceYn);

    // ─── USE_YN 일괄 변경 ─────────────────────────────────────────────

    /** 선택된 서비스 ID 목록의 USE_YN 일괄 변경 */
    void updateUseYnBatch(
            @Param("serviceIds") List<String> serviceIds,
            @Param("useYn") String useYn,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // ─── WorkSpace 팝업 ───────────────────────────────────────────────

    /** WorkSpace 팝업 목록 조회 */
    List<WorkSpaceResponse> selectWorkspacePage(
            @Param("workSpaceId") String workSpaceId,
            @Param("workSpaceName") String workSpaceName,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /** WorkSpace 팝업 전체 건수 */
    int countWorkspace(@Param("workSpaceId") String workSpaceId, @Param("workSpaceName") String workSpaceName);
}
