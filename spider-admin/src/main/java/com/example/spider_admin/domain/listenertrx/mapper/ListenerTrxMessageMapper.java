package com.example.spider_admin.domain.listenertrx.mapper;

import com.example.spider_admin.domain.listenertrx.dto.AppMappingResponse;
import com.example.spider_admin.domain.listenertrx.dto.AppMappingUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 요청처리 App 맵핑 Command Mapper
 */
@Mapper
public interface ListenerTrxMessageMapper {

    AppMappingResponse selectResponseByPk(@Param("gwId") String gwId, @Param("reqIdCode") String reqIdCode);

    int countByPk(@Param("gwId") String gwId, @Param("reqIdCode") String reqIdCode);

    void insert(@Param("dto") AppMappingUpsertRequest dto);

    void update(
            @Param("gwId") String gwId,
            @Param("reqIdCode") String reqIdCode,
            @Param("dto") AppMappingUpsertRequest dto);

    void deleteByPk(@Param("gwId") String gwId, @Param("reqIdCode") String reqIdCode);

    List<AppMappingResponse> findBySearch(
            @Param("gwId") String gwId,
            @Param("orgId") String orgId,
            @Param("reqIdCode") String reqIdCode,
            @Param("trxKeyword") String trxKeyword,
            @Param("bizAppId") String bizAppId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countBySearch(
            @Param("gwId") String gwId,
            @Param("orgId") String orgId,
            @Param("reqIdCode") String reqIdCode,
            @Param("trxKeyword") String trxKeyword,
            @Param("bizAppId") String bizAppId);

    List<AppMappingResponse> findAllForExport(
            @Param("gwId") String gwId,
            @Param("orgId") String orgId,
            @Param("reqIdCode") String reqIdCode,
            @Param("trxKeyword") String trxKeyword,
            @Param("bizAppId") String bizAppId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
