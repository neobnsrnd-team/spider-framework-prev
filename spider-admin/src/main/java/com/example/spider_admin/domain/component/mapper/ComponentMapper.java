package com.example.spider_admin.domain.component.mapper;

import com.example.spider_admin.domain.component.dto.ComponentCreateRequest;
import com.example.spider_admin.domain.component.dto.ComponentParamRequest;
import com.example.spider_admin.domain.component.dto.ComponentParamResponse;
import com.example.spider_admin.domain.component.dto.ComponentResponse;
import com.example.spider_admin.domain.component.dto.ComponentUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 컴포넌트 Mapper (CRUD + Query, FWK_COMPONENT + FWK_COMPONENT_PARAM) */
@Mapper
public interface ComponentMapper {

    // ─── 마스터 (FWK_COMPONENT) ───────────────────────────────────────

    /** 컴포넌트 ID로 단건 조회 (params 미포함) */
    ComponentResponse selectResponseById(@Param("componentId") String componentId);

    /** 컴포넌트 ID 존재 확인용 카운트 */
    int countById(@Param("componentId") String componentId);

    /** 새 컴포넌트 등록 */
    void insertComponent(
            @Param("dto") ComponentCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** 컴포넌트 수정 */
    void updateComponent(
            @Param("componentId") String componentId,
            @Param("dto") ComponentUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** 컴포넌트 삭제 */
    void deleteById(@Param("componentId") String componentId);

    /** 검색 조건으로 컴포넌트 목록 조회 (페이지네이션) */
    @SuppressWarnings("java:S107")
    List<ComponentResponse> findAllWithSearch(
            @Param("componentId") String componentId,
            @Param("componentName") String componentName,
            @Param("componentType") String componentType,
            @Param("useYn") String useYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /** 검색 조건으로 컴포넌트 전체 건수 조회 */
    long countAllWithSearch(
            @Param("componentId") String componentId,
            @Param("componentName") String componentName,
            @Param("componentType") String componentType,
            @Param("useYn") String useYn);

    // ─── 파라미터 (FWK_COMPONENT_PARAM) ──────────────────────────────

    /** 컴포넌트 ID로 파라미터 목록 조회 */
    List<ComponentParamResponse> findParamsByComponentId(@Param("componentId") String componentId);

    /** 컴포넌트 ID에 해당하는 파라미터 전체 삭제 */
    void deleteParamsByComponentId(@Param("componentId") String componentId);

    /** 파라미터 배치 등록 (Oracle DUAL UNION ALL 패턴) */
    void insertParamBatch(
            @Param("componentId") String componentId, @Param("params") List<ComponentParamRequest> params);
}
