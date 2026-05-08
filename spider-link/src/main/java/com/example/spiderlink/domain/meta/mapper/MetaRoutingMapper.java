package com.example.spiderlink.domain.meta.mapper;

import com.example.spiderlink.domain.meta.dto.ComponentInfo;
import com.example.spiderlink.domain.meta.dto.RelationParam;
import com.example.spiderlink.domain.meta.dto.ServiceStep;
import com.example.spiderlink.domain.meta.dto.TrxMappingEntry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK 서비스 라우팅 메타데이터 조회 전용 Mapper.
 *
 * <p>MetaDrivenServiceOrchestrator 가 커맨드 수신 시 아래 체인으로 DB를 조회한다.</p>
 * <pre>
 *   FWK_LISTENER_TRX_MESSAGE → FWK_SERVICE → FWK_SERVICE_RELATION
 *     → FWK_COMPONENT + (FWK_RELATION_PARAM ⋈ FWK_COMPONENT_PARAM)
 * </pre>
 */
@Mapper
public interface MetaRoutingMapper {

    /**
     * FWK_LISTENER_TRX_MESSAGE 전체 행을 반환한다.
     *
     * <p>기동 시 {@code MessageEngineContext}가 일괄 로딩하여 메모리 맵을 구성한다.
     * 이후 거래 수신 시 DB 재조회 없이 메모리에서 TRX_ID·ORG_ID를 반환한다.</p>
     */
    List<TrxMappingEntry> selectAllTrxMappings();

    /**
     * 게이트웨이에 등록된 커맨드(REQ_ID_CODE) 목록을 반환한다.
     * 시작 시 지원 커맨드 캐시를 초기화하는 데 사용한다.
     * @deprecated {@link #selectAllTrxMappings()} 일괄 로딩으로 대체 — MessageEngineContext 사용 권장
     */
    @Deprecated
    List<String> selectRegisteredCommands(@Param("gwId") String gwId);

    /** FWK_LISTENER_TRX_MESSAGE에서 커맨드에 해당하는 TRX_ID를 반환한다. */
    String selectTrxId(@Param("gwId") String gwId, @Param("command") String command);

    /**
     * TRX_ID + ORG_ID + IO_TYPE으로 SERVICE_ID를 반환한다.
     *
     * @param ioType I(입력 전문)
     */
    String selectServiceId(
            @Param("trxId")   String trxId,
            @Param("orgId")   String orgId,
            @Param("ioType")  String ioType);

    /** SERVICE_ID에 속한 컴포넌트 실행 단계를 순서 오름차순으로 반환한다. */
    List<ServiceStep> selectServiceSteps(@Param("serviceId") String serviceId);

    /** COMPONENT_ID로 컴포넌트 메타데이터를 반환한다. */
    ComponentInfo selectComponent(@Param("componentId") String componentId);

    /**
     * 특정 서비스 스텝의 SQL 파라미터 바인딩 정보를 반환한다.
     *
     * <p>FWK_RELATION_PARAM(실행 시 payload 키) ⋈ FWK_COMPONENT_PARAM(SQL 파라미터 키) 조인 결과.</p>
     */
    List<RelationParam> selectRelationParams(
            @Param("serviceId")     String serviceId,
            @Param("serviceSeqNo")  int    serviceSeqNo,
            @Param("componentId")   String componentId);
}
