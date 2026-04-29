package com.example.spider_admin.domain.property.mapper;

import com.example.spider_admin.domain.property.dto.PropertyCreateRequest;
import com.example.spider_admin.domain.property.dto.PropertyGroupResponse;
import com.example.spider_admin.domain.property.dto.PropertyResponse;
import com.example.spider_admin.domain.property.dto.PropertySaveRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 프로퍼티 Command Mapper
 * CRUD 작업 담당
 */
@Mapper
public interface PropertyMapper {

    void insertBatch(
            @Param("list") List<PropertyCreateRequest> properties,
            @Param("propertyGroupId") String propertyGroupId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로퍼티 단건 조회 (ResponseDTO)
     */
    PropertyResponse selectResponseById(
            @Param("propertyGroupId") String propertyGroupId, @Param("propertyId") String propertyId);

    /**
     * 프로퍼티 생성
     */
    void insert(
            @Param("dto") PropertySaveRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로퍼티 수정
     */
    void update(
            @Param("dto") PropertySaveRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로퍼티 삭제
     */
    void delete(@Param("propertyGroupId") String propertyGroupId, @Param("propertyId") String propertyId);

    /**
     * 프로퍼티 존재 여부 확인
     */
    int countById(@Param("propertyGroupId") String propertyGroupId, @Param("propertyId") String propertyId);

    /**
     * 프로퍼티 그룹 전체 삭제
     */
    void deleteByPropertyGroupId(@Param("propertyGroupId") String propertyGroupId);

    /**
     * 프로퍼티 그룹 목록 조회 (페이징, 검색 지원)
     */
    List<PropertyGroupResponse> selectPropertyGroups(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 프로퍼티 그룹 검색 조건 카운트
     */
    long countPropertyGroupsWithSearch(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);

    /**
     * 프로퍼티 그룹 전체 목록 조회
     */
    List<PropertyGroupResponse> selectAllPropertyGroups();

    /**
     * 프로퍼티 그룹 ID로 프로퍼티 목록 조회
     */
    List<PropertyResponse> selectPropertiesByGroupId(
            @Param("propertyGroupId") String propertyGroupId,
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue);

    /**
     * 엑셀 내보내기용 프로퍼티 그룹 전체 조회 (페이징 없음)
     */
    List<PropertyGroupResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
