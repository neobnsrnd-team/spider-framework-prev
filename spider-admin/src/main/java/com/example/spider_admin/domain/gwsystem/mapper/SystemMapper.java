package com.example.spider_admin.domain.gwsystem.mapper;

import com.example.spider_admin.domain.gwsystem.dto.SystemResponse;
import com.example.spider_admin.domain.gwsystem.dto.SystemUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemMapper {

    SystemResponse selectResponseBySystem(@Param("gwId") String gwId, @Param("systemId") String systemId);

    int countBySystem(@Param("gwId") String gwId, @Param("systemId") String systemId);

    void insertSystem(@Param("dto") SystemUpsertRequest dto);

    void updateSystem(@Param("dto") SystemUpsertRequest dto);

    void deleteSystem(@Param("gwId") String gwId, @Param("systemId") String systemId);

    List<SystemResponse> findByGateway(@Param("gwId") String gwId);

    List<SystemResponse> findBySearchPaging(
            @Param("gwId") String gwId,
            @Param("operModeType") String operModeType,
            @Param("stopYn") String stopYn,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countBySearch(
            @Param("gwId") String gwId, @Param("operModeType") String operModeType, @Param("stopYn") String stopYn);
}
