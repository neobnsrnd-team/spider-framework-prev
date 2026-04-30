package com.example.spideradmin.domain.transdata.mapper;

import com.example.spideradmin.domain.transdata.dto.TransDataSourceResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 이행 데이터 생성 Command Mapper
 * INSERT 전용
 */
public interface TransDataGenerationMapper {

    void insertTransDataTimes(
            @Param("tranSeq") long tranSeq,
            @Param("userId") String userId,
            @Param("tranTime") String tranTime,
            @Param("tranResult") String tranResult,
            @Param("tranReason") String tranReason);

    void insertTransDataHisBatch(@Param("list") List<Map<String, Object>> list);

    String selectNextTranSeq();

    List<TransDataSourceResponse> findTrxList(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgId") String orgId);

    List<TransDataSourceResponse> findMessageList(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgId") String orgId);

    List<TransDataSourceResponse> findCodeGroupList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<TransDataSourceResponse> findWebappList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<TransDataSourceResponse> findServiceList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<TransDataSourceResponse> findErrorList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<TransDataSourceResponse> findComponentList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<TransDataSourceResponse> findPropertyList(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);
}
