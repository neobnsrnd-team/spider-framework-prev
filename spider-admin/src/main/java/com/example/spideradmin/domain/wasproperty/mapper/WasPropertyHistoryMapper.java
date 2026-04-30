package com.example.spideradmin.domain.wasproperty.mapper;

import com.example.spideradmin.domain.wasproperty.dto.WasPropertyHistoryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * WAS Property History Mapper
 * FWK_WAS_PROPERTY_HISTORY 백업/복원 담당
 */
@Mapper
public interface WasPropertyHistoryMapper {

    /**
     * 특정 인스턴스+프로퍼티그룹의 WAS 프로퍼티 CUR_VERSION 일괄 증가
     */
    void incrementVersion(@Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);

    /**
     * 특정 인스턴스+프로퍼티그룹의 WAS 프로퍼티 전체 삭제
     */
    void deleteByInstanceAndGroup(
            @Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);

    /**
     * 특정 인스턴스+프로퍼티그룹의 최대 버전 조회
     */
    Integer selectMaxVersion(@Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);

    /**
     * WAS 프로퍼티 이력 일괄 등록 (백업)
     */
    void insertBatchHistory(@Param("list") List<WasPropertyHistoryResponse> histories);

    /**
     * 특정 인스턴스+프로퍼티그룹의 버전 목록 조회 (최신순)
     */
    List<WasPropertyHistoryResponse> selectVersions(
            @Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);

    /**
     * 특정 인스턴스+프로퍼티그룹+버전의 이력 데이터 조회
     */
    List<WasPropertyHistoryResponse> selectHistoryByVersion(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("version") int version);

    /**
     * 특정 인스턴스+프로퍼티그룹의 현재 WAS 프로퍼티 조회 (백업용)
     */
    List<WasPropertyHistoryResponse> selectCurrentProperties(
            @Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);

    /**
     * WAS 프로퍼티 일괄 INSERT (복원용)
     */
    void insertBatchProperty(@Param("list") List<WasPropertyHistoryResponse> wasProperties);
}
