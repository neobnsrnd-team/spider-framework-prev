package com.example.spideradmin.domain.datasource.mapper;

import com.example.spideradmin.domain.datasource.dto.DataSourceCreateRequest;
import com.example.spideradmin.domain.datasource.dto.DataSourceResponse;
import com.example.spideradmin.domain.datasource.dto.DataSourceUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 데이터소스 Mapper (CRUD + Query)
 */
@Mapper
public interface DataSourceMapper {

    DataSourceResponse selectResponseById(@Param("dbId") String dbId);

    int countByDbId(@Param("dbId") String dbId);

    void insert(
            @Param("dto") DataSourceCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("dbId") String dbId,
            @Param("dto") DataSourceUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("dbId") String dbId);

    List<DataSourceResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("jndiYnFilter") String jndiYnFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("jndiYnFilter") String jndiYnFilter);

    List<DataSourceResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("jndiYnFilter") String jndiYnFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
