package com.example.spideradmin.domain.transport.mapper;

import com.example.spideradmin.domain.transport.dto.TransportResponse;
import com.example.spideradmin.domain.transport.dto.TransportUpsertRequest;
import com.example.spideradmin.domain.transport.dto.TrxTypeOptionResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TransportMapper {

    int countByPk(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("reqResType") String reqResType);

    void insertTransport(@Param("dto") TransportUpsertRequest dto);

    void updateTransport(@Param("dto") TransportUpsertRequest dto);

    void deleteTransport(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("reqResType") String reqResType);

    List<TransportResponse> findBySearchPaging(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("reqResType") String reqResType,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    List<TransportResponse> findByGatewayId(@Param("gwId") String gwId);

    long countBySearch(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("reqResType") String reqResType);

    List<TrxTypeOptionResponse> findTrxTypeOptions();

    long countByTrxType(@Param("trxType") String trxType);

    List<TransportResponse> findAllForExport(
            @Param("orgId") String orgId,
            @Param("trxType") String trxType,
            @Param("ioType") String ioType,
            @Param("reqResType") String reqResType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
