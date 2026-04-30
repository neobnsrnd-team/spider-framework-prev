package com.example.spideradmin.domain.wasinstance.mapper;

import com.example.spideradmin.domain.wasinstance.dto.WasInstanceRequest;
import com.example.spideradmin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spideradmin.domain.wasproperty.dto.WasInstanceSimpleResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * WAS Instance Mapper
 */
public interface WasInstanceMapper {

    WasInstanceResponse selectResponseById(String instanceId);

    int countById(String instanceId);

    List<WasInstanceResponse> selectAll();

    /**
     * 인스턴스 ID와 이름만 조회 (선택 목록용)
     */
    List<WasInstanceSimpleResponse> selectAllSimple();

    void insert(@Param("dto") WasInstanceRequest dto);

    void update(@Param("instanceId") String instanceId, @Param("dto") WasInstanceRequest dto);

    void deleteById(String instanceId);

    List<WasInstanceResponse> findBySearchPaging(
            @Param("instanceName") String instanceName,
            @Param("instanceType") String instanceType,
            @Param("operModeType") String operModeType,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(
            @Param("instanceName") String instanceName,
            @Param("instanceType") String instanceType,
            @Param("operModeType") String operModeType);

    List<WasInstanceResponse> findAllForExport(
            @Param("instanceName") String instanceName,
            @Param("instanceType") String instanceType,
            @Param("operModeType") String operModeType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
