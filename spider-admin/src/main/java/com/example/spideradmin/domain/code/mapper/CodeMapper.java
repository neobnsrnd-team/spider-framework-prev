package com.example.spideradmin.domain.code.mapper;

import com.example.spideradmin.domain.code.dto.CodeCreateRequest;
import com.example.spideradmin.domain.code.dto.CodeResponse;
import com.example.spideradmin.domain.code.dto.CodeUpdateRequest;
import com.example.spideradmin.domain.code.dto.CodeWithGroupResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CodeMapper {

    CodeResponse selectResponseById(@Param("codeGroupId") String codeGroupId, @Param("code") String code);

    void insert(
            @Param("dto") CodeCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("codeGroupId") String codeGroupId,
            @Param("code") String code,
            @Param("dto") CodeUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("codeGroupId") String codeGroupId, @Param("code") String code);

    void deleteByCodeGroupId(String codeGroupId);

    int countByCodeGroupIdAndCode(@Param("codeGroupId") String codeGroupId, @Param("code") String code);

    long countByCodeGroupId(String codeGroupId);

    List<CodeWithGroupResponse> findAllWithGroup(
            @Param("codeGroupId") String codeGroupId,
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithGroup(
            @Param("codeGroupId") String codeGroupId,
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue);

    CodeWithGroupResponse findByIdWithGroup(@Param("codeGroupId") String codeGroupId, @Param("code") String code);

    List<CodeResponse> findByCodeGroupId(String codeGroupId);

    List<CodeWithGroupResponse> findAllForExport(
            @Param("codeGroupId") String codeGroupId,
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
