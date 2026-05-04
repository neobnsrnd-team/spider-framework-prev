package com.example.spideradmin.domain.codegroup.mapper;

import com.example.spideradmin.domain.codegroup.dto.CodeGroupCreateRequest;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupResponse;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupUpdateRequest;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupWithCodesResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CodeGroupMapper {

    CodeGroupResponse selectResponseById(String codeGroupId);

    void insert(
            @Param("dto") CodeGroupCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("codeGroupId") String codeGroupId,
            @Param("dto") CodeGroupUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(String codeGroupId);

    int countByCodeGroupId(String codeGroupId);

    int countByCodeGroupName(String codeGroupName);

    List<CodeGroupResponse> findAllWithCodeCount();

    List<CodeGroupResponse> findAllWithCodeCountAndSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithCodeCountAndSearch(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    CodeGroupWithCodesResponse findByIdWithCodes(String codeGroupId);

    List<CodeGroupResponse> findByBizGroupId(String bizGroupId);

    List<CodeGroupResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
