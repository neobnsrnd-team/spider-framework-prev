package com.example.spideradmin.domain.codetemplate.mapper;

import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateCreateRequest;
import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateResponse;
import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CodeTemplateMapper {

    CodeTemplateResponse selectById(String templateId);

    void insert(
            @Param("dto") CodeTemplateCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("templateId") String templateId,
            @Param("dto") CodeTemplateUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(String templateId);

    int countByTemplateId(String templateId);

    List<CodeTemplateResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithSearch(@Param("searchField") String searchField, @Param("searchValue") String searchValue);

    List<CodeTemplateResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /** 사용여부 Y인 템플릿 전체 조회 (코드 생성용) */
    List<CodeTemplateResponse> findAllActive();
}
