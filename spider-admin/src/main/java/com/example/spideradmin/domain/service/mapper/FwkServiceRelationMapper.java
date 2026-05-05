package com.example.spideradmin.domain.service.mapper;

import com.example.spideradmin.domain.service.dto.FwkServiceRelationItemRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationParamRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 서비스 연결 Mapper (FWK_SERVICE_RELATION + FWK_RELATION_PARAM).
 *
 * <p>findRelationsByServiceId: FWK_SERVICE_RELATION + FWK_COMPONENT + FWK_COMPONENT_PARAM +
 * FWK_RELATION_PARAM 4테이블 조인으로 resultMap 사용.
 */
@Mapper
public interface FwkServiceRelationMapper {

    // ─── 조회 ─────────────────────────────────────────────────────────

    /**
     * 서비스 ID로 연결 컴포넌트 + 파라미터 목록 조회 (resultMap — 4테이블 조인).
     */
    List<FwkServiceRelationResponse> findRelationsByServiceId(@Param("serviceId") String serviceId);

    // ─── 삭제 ─────────────────────────────────────────────────────────

    /** 서비스 ID의 연결 파라미터 전체 삭제 (FK 의존 상 먼저 삭제) */
    void deleteParamsByServiceId(@Param("serviceId") String serviceId);

    /** 서비스 ID의 연결 컴포넌트 전체 삭제 */
    void deleteRelationsByServiceId(@Param("serviceId") String serviceId);

    // ─── 배치 등록 ────────────────────────────────────────────────────

    /** 연결 컴포넌트 배치 등록 (Oracle DUAL UNION ALL 패턴) */
    void insertRelationBatch(
            @Param("serviceId") String serviceId,
            @Param("relations") List<FwkServiceRelationItemRequest> relations,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** 연결 파라미터 배치 등록 (Oracle DUAL UNION ALL 패턴) */
    void insertRelationParamBatch(
            @Param("serviceId") String serviceId, @Param("params") List<FwkServiceRelationParamRequest> params);
}
