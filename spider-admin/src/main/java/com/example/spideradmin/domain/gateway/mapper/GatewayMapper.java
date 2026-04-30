package com.example.spideradmin.domain.gateway.mapper;

import com.example.spideradmin.domain.gateway.dto.GatewayResponse;
import com.example.spideradmin.domain.gateway.dto.GatewayUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GatewayMapper {

    GatewayResponse selectResponseById(@Param("gwId") String gwId);

    int countByGwId(@Param("gwId") String gwId);

    void insertGateway(@Param("dto") GatewayUpsertRequest dto);

    void updateGateway(@Param("dto") GatewayUpsertRequest dto);

    List<GatewayResponse> findBySearchPaging(
            @Param("gwId") String gwId,
            @Param("gwName") String gwName,
            @Param("ioType") String ioType,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(@Param("gwId") String gwId, @Param("gwName") String gwName, @Param("ioType") String ioType);

    List<GatewayResponse> findAllForExport(
            @Param("gwId") String gwId,
            @Param("gwName") String gwName,
            @Param("ioType") String ioType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
