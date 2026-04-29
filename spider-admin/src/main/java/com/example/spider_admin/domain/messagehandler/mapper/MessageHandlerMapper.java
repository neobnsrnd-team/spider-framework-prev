package com.example.spider_admin.domain.messagehandler.mapper;

import com.example.spider_admin.domain.messagehandler.dto.HandlerResponse;
import com.example.spider_admin.domain.messagehandler.dto.HandlerUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageHandlerMapper {

    int countByHandler(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("operModeType") String operModeType);

    void insertHandler(
            @Param("dto") HandlerUpsertRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void updateHandler(
            @Param("dto") HandlerUpsertRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteHandler(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("operModeType") String operModeType);

    List<HandlerResponse> findBySearchPagingDto(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(@Param("orgId") String orgId, @Param("trxType") String trxType, @Param("ioType") String ioType);

    List<HandlerResponse> findAllForExport(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
