package com.example.spideradmin.domain.org.mapper;

import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.domain.org.dto.OrgUpsertRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 기관 Mapper (Command + Query)
 */
@Mapper
public interface OrgMapper {

    // ==================== Command ====================

    OrgResponse selectResponseById(@Param("orgId") String orgId);

    int countByOrgId(@Param("orgId") String orgId);

    void insertOrg(@Param("dto") OrgUpsertRequest dto);

    void updateOrg(@Param("dto") OrgUpsertRequest dto);

    void deleteOrgById(@Param("orgId") String orgId);

    // ==================== Query ====================

    List<OrgResponse> findAll();

    List<OrgResponse> findAllResponse();

    List<OrgResponse> findBySearchPaging(
            @Param("searchField") String searchField,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(@Param("searchField") String searchField, @Param("keyword") String keyword);

    List<OrgResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
