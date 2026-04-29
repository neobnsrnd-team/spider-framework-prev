package com.example.spider_admin.domain.workgroup.mapper;

import com.example.spider_admin.domain.workgroup.dto.WorkGroupResponse;
import com.example.spider_admin.domain.workgroup.dto.WorkGroupUpdateRequest;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_WORK_GROUP 테이블 MyBatis Mapper.
 *
 * <p>GROUP_ID 형식: userId + LPAD(seq, 3, '0') — SQL에서 자동 채번.
 */
@Mapper
public interface WorkGroupMapper {

    /** userId 소속 그룹 전체 조회. */
    List<WorkGroupResponse> findByUserId(@Param("userId") String userId);

    /** 그룹 생성 — GROUP_ID는 SQL에서 자동 채번. */
    int insert(Map<String, Object> params);

    /** 방금 생성된 그룹 단건 조회 (INSERT 후 응답용). */
    WorkGroupResponse findLatestByUserId(@Param("userId") String userId);

    /** groupId 기준 단건 조회. */
    WorkGroupResponse findByGroupId(@Param("groupId") String groupId);

    /** 그룹명·설명 수정. */
    int update(WorkGroupUpdateRequest request);

    /**
     * 그룹 삭제.
     *
     * <p>FWK_WORK_LIST에 참조되는 그룹은 삭제되지 않음 (SQL에서 NOT EXISTS 조건).
     */
    int delete(@Param("groupId") String groupId);
}
