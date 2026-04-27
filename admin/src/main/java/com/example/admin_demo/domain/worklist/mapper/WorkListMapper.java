package com.example.admin_demo.domain.worklist.mapper;

import com.example.admin_demo.domain.worklist.dto.WorkListResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_WORK_LIST 테이블 MyBatis Mapper.
 *
 * <p>작업함 목록 조회, 그룹 이동, 권한이양 쿼리를 제공한다.
 */
@Mapper
public interface WorkListMapper {

    /**
     * 사용자 작업함 목록 조회.
     *
     * @param userId  사용자 ID (GROUP_ID LIKE userId||'%' 패턴으로 검색)
     * @param groupId 그룹 필터 (null이면 전체 조회)
     */
    List<WorkListResponse> findByUserId(@Param("userId") String userId, @Param("groupId") String groupId);

    /** 선택 항목을 지정 그룹으로 이동. */
    int moveGroup(Map<String, Object> params);

    /**
     * 권한이양 — GROUP_ID를 toUserId+'001'로, LAST_UPDATE_USER_ID를 toUserId로 변경.
     *
     * <p>FIRST_INSERT_USER_ID는 변경하지 않는다 (as-is 동일).
     */
    int transfer(Map<String, Object> params);
}
