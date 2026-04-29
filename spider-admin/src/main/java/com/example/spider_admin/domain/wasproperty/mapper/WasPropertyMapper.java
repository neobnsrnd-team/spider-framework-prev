package com.example.spider_admin.domain.wasproperty.mapper;

import com.example.spider_admin.domain.wasproperty.dto.WasInstanceSimpleResponse;
import com.example.spider_admin.domain.wasproperty.dto.WasPropertyForPropertyResponse;
import com.example.spider_admin.domain.wasproperty.dto.WasPropertyResponse;
import com.example.spider_admin.domain.wasproperty.dto.WasPropertyWithDefaultResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * WAS Property Mapper
 */
public interface WasPropertyMapper {

    /**
     * 복합키로 프로퍼티 조회 (ResponseDTO 반환)
     */
    WasPropertyResponse selectResponseById(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId);

    /**
     * 복합키로 존재 여부 확인
     */
    int countById(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId);

    /**
     * 특정 인스턴스의 모든 프로퍼티 조회
     */
    List<WasPropertyResponse> selectByInstanceId(@Param("instanceId") String instanceId);

    /**
     * 특정 인스턴스의 프로퍼티 조회 (기본값 포함)
     */
    List<Map<String, Object>> selectByInstanceIdWithDefaults(@Param("instanceId") String instanceId);

    /**
     * 프로퍼티 생성
     */
    void insert(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId,
            @Param("propertyValue") String propertyValue,
            @Param("propertyDesc") String propertyDesc);

    /**
     * 프로퍼티 수정
     * @return 수정된 행의 수 (낙관적 락 체크용)
     */
    int update(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId,
            @Param("propertyValue") String propertyValue,
            @Param("propertyDesc") String propertyDesc);

    /**
     * 프로퍼티 삭제
     */
    void deleteById(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId);

    /**
     * 특정 프로퍼티의 WAS 프로퍼티 삭제
     */
    void deleteByGroupAndProperty(
            @Param("propertyGroupId") String propertyGroupId, @Param("propertyId") String propertyId);

    /**
     * 프로퍼티 그룹의 모든 WAS 프로퍼티 삭제
     */
    void deleteByPropertyGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 특정 인스턴스의 모든 WAS 프로퍼티 삭제 (cascade delete용)
     */
    void deleteByInstanceId(@Param("instanceId") String instanceId);

    /**
     * WAS 프로퍼티 MERGE (INSERT or UPDATE)
     */
    void mergeProperty(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("propertyId") String propertyId,
            @Param("propertyValue") String propertyValue,
            @Param("propertyDesc") String propertyDesc);

    /**
     * 특정 프로퍼티에 대한 WAS 인스턴스별 설정 값 조회
     */
    List<WasPropertyForPropertyResponse> selectWasPropertiesByProperty(
            @Param("propertyGroupId") String propertyGroupId, @Param("propertyId") String propertyId);

    /**
     * WAS 인스턴스 목록 조회 (인스턴스 선택용)
     */
    List<WasInstanceSimpleResponse> selectAllInstances();

    /**
     * 특정 인스턴스의 프로퍼티 페이지네이션 조회 (기본값 포함)
     */
    List<WasPropertyWithDefaultResponse> selectByInstanceIdPaging(
            @Param("instanceId") String instanceId,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 특정 인스턴스의 프로퍼티 건수 (페이지네이션용)
     */
    long countByInstanceId(@Param("instanceId") String instanceId, @Param("propertyGroupId") String propertyGroupId);
}
