package com.example.spideradmin.domain.property.mapper;

import com.example.spideradmin.domain.property.dto.PropertyHistoryResponse;
import com.example.spideradmin.domain.property.dto.PropertyHistoryVersionResponse;
import com.example.spideradmin.domain.property.dto.PropertyResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 프로퍼티 백업/복원 Command Mapper
 * FWK_PROPERTY + FWK_PROPERTY_HISTORY 담당
 */
@Mapper
public interface PropertyHistoryMapper {

    /**
     * 프로퍼티 그룹의 CUR_VERSION 일괄 증가
     */
    void incrementVersionByGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 프로퍼티 그룹의 전체 프로퍼티 조회 (백업용)
     */
    List<PropertyResponse> selectByPropertyGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 프로퍼티 그룹의 최대 버전 조회
     * @return 최대 버전 (데이터 없으면 null)
     */
    Integer selectMaxVersionByGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 프로퍼티 이력 일괄 등록 (백업) - PropertyResponse 리스트 기반
     */
    void insertBatchHistory(
            @Param("list") List<PropertyResponse> properties,
            @Param("version") int version,
            @Param("reason") String reason,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로퍼티 그룹의 버전 목록 조회 (최신순)
     */
    List<PropertyHistoryVersionResponse> selectVersionsByGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 프로퍼티 그룹의 특정 버전 이력 조회
     */
    List<PropertyHistoryResponse> selectHistoryByGroupIdAndVersion(
            @Param("propertyGroupId") String propertyGroupId, @Param("version") int version);

    /**
     * 이력 데이터로 FWK_PROPERTY 복원 (일괄 등록)
     */
    void insertBatchFromHistory(
            @Param("list") List<PropertyHistoryResponse> histories,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);
}
