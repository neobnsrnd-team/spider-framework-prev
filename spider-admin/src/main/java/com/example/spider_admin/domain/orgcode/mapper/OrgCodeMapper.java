package com.example.spider_admin.domain.orgcode.mapper;

import com.example.spider_admin.domain.orgcode.dto.OrgCodePopupResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeRowRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrgCodeMapper {
    void insert(
            @Param("row") OrgCodeRowRequest row,
            @Param("orgId") String orgId,
            @Param("codeGroupId") String codeGroupId);

    void delete(
            @Param("orgId") String orgId,
            @Param("codeGroupId") String codeGroupId,
            @Param("code") String code,
            @Param("orgCode") String orgCode);

    int countByPk(
            @Param("orgId") String orgId,
            @Param("codeGroupId") String codeGroupId,
            @Param("code") String code,
            @Param("orgCode") String orgCode);

    List<OrgCodeResponse> findAllByCondition(
            @Param("orgId") String orgId,
            @Param("codeGroupId") String codeGroupId,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countByCondition(@Param("orgId") String orgId, @Param("codeGroupId") String codeGroupId);

    List<OrgCodePopupResponse> findPopupData(@Param("codeGroupId") String codeGroupId, @Param("orgId") String orgId);

    List<OrgCodeResponse> findAllForExport(
            @Param("orgId") String orgId,
            @Param("codeGroupId") String codeGroupId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
