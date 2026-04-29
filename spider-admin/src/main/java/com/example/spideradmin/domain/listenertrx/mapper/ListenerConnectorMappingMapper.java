package com.example.spideradmin.domain.listenertrx.mapper;

import com.example.spideradmin.domain.listenertrx.dto.ListenerConnectorMappingResponse;
import com.example.spideradmin.domain.listenertrx.dto.ListenerConnectorMappingUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ListenerConnectorMappingMapper {

    ListenerConnectorMappingResponse selectResponseByPk(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier);

    int countByPk(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier);

    void insert(@Param("dto") ListenerConnectorMappingUpsertRequest dto);

    void insertBatch(@Param("list") List<ListenerConnectorMappingUpsertRequest> dtos);

    void update(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier,
            @Param("dto") ListenerConnectorMappingUpsertRequest dto);

    void deleteByPk(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier);

    List<ListenerConnectorMappingResponse> findAll();

    List<ListenerConnectorMappingResponse> findBySearchPaging(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier,
            @Param("connectorGwId") String connectorGwId,
            @Param("connectorSystemId") String connectorSystemId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier,
            @Param("connectorGwId") String connectorGwId,
            @Param("connectorSystemId") String connectorSystemId);

    List<ListenerConnectorMappingResponse> findAllForExport(
            @Param("listenerGwId") String listenerGwId,
            @Param("listenerSystemId") String listenerSystemId,
            @Param("identifier") String identifier,
            @Param("connectorGwId") String connectorGwId,
            @Param("connectorSystemId") String connectorSystemId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
