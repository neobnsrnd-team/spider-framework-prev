package com.example.spideradmin.domain.wasgroup.mapper;

import com.example.spideradmin.domain.wasgroup.dto.WasGroupRequest;
import com.example.spideradmin.domain.wasgroup.dto.WasGroupResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * WAS Group Mapper
 */
public interface WasGroupMapper {

    // ==================== WAS Group CRUD ====================

    /**
     * 그룹 ID로 조회 (ResponseDTO 반환)
     */
    WasGroupResponse selectResponseById(@Param("wasGroupId") String wasGroupId);

    /**
     * 그룹 존재 여부 확인
     */
    int countById(@Param("wasGroupId") String wasGroupId);

    /**
     * 전체 그룹 조회
     */
    List<WasGroupResponse> selectAll();

    /**
     * 그룹 생성
     */
    void insert(@Param("dto") WasGroupRequest dto);

    /**
     * 그룹 수정
     */
    void update(@Param("wasGroupId") String wasGroupId, @Param("dto") WasGroupRequest dto);

    /**
     * 그룹 삭제
     */
    void deleteById(@Param("wasGroupId") String wasGroupId);

    // ==================== Group-Instance 매핑 ====================

    /**
     * 그룹에 인스턴스 추가
     */
    void insertGroupInstance(@Param("wasGroupId") String wasGroupId, @Param("instanceId") String instanceId);

    /**
     * 그룹의 모든 인스턴스 매핑 삭제
     */
    void deleteAllGroupInstances(@Param("wasGroupId") String wasGroupId);

    /**
     * 그룹에 속한 인스턴스 ID 목록 조회
     */
    List<String> selectInstanceIdsByGroupId(@Param("wasGroupId") String wasGroupId);

    /**
     * 그룹에 속한 인스턴스 전체 정보 조회 (JOIN)
     */
    List<Map<String, Object>> selectInstanceDetailsByGroupId(@Param("wasGroupId") String wasGroupId);

    /**
     * 그룹-인스턴스 매핑 존재 여부 확인
     */
    int existsGroupInstance(@Param("wasGroupId") String wasGroupId, @Param("instanceId") String instanceId);

    /**
     * 그룹의 인스턴스 개수 조회
     */
    long countInstancesByGroupId(@Param("wasGroupId") String wasGroupId);

    List<WasGroupResponse> findBySearchPaging(
            @Param("wasGroupId") String wasGroupId,
            @Param("wasGroupName") String wasGroupName,
            @Param("wasGroupDesc") String wasGroupDesc,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    long countBySearch(
            @Param("wasGroupId") String wasGroupId,
            @Param("wasGroupName") String wasGroupName,
            @Param("wasGroupDesc") String wasGroupDesc);

    List<WasGroupResponse> findAllForExport(
            @Param("wasGroupId") String wasGroupId,
            @Param("wasGroupName") String wasGroupName,
            @Param("wasGroupDesc") String wasGroupDesc,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
