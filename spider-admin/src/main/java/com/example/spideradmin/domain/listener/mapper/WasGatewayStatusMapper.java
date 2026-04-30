package com.example.spideradmin.domain.listener.mapper;

import com.example.spideradmin.domain.listener.dto.SimpleResponse;
import com.example.spideradmin.domain.listener.dto.WasGatewayStatusResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WasGatewayStatusMapper {

    List<WasGatewayStatusResponse> findBySearch(
            @Param("instanceId") String instanceId,
            @Param("gwId") String gwId,
            @Param("operModeType") String operModeType,
            @Param("stopYn") String stopYn,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(
            @Param("instanceId") String instanceId,
            @Param("gwId") String gwId,
            @Param("operModeType") String operModeType,
            @Param("stopYn") String stopYn);

    List<SimpleResponse> findDistinctInstances();

    List<SimpleResponse> findDistinctGateways();

    List<String> findDistinctOperModes();

    List<WasGatewayStatusResponse> findAllForExport(
            @Param("instanceId") String instanceId,
            @Param("gwId") String gwId,
            @Param("operModeType") String operModeType,
            @Param("stopYn") String stopYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
