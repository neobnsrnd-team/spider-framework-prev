package com.example.spider_admin.domain.worklist.mapper;

import com.example.spider_admin.domain.worklist.dto.WorkListResponse;
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

    /**
     * 변경 이력 UPSERT.
     *
     * <p>(WORK_ID, WORK_DATA_PK) 기준으로 사용자 소속 항목이 이미 존재하면 UPDATE,
     * 없으면 INSERT. GROUP_ID 기본값은 userId+'001'.
     *
     * <p>파라미터 키: workId, workDataPk, workName, crudType, userId
     */
    void upsertHistory(Map<String, Object> params);

    /**
     * 결재요청 — APPROVAL_SEQ를 FWK_SETTLEMENT의 APPROVAL_ID로 갱신.
     *
     * <p>파라미터 키: approvalId, workSeqs
     */
    int updateApprovalSeq(Map<String, Object> params);

    /** 단건 조회 — 이행스크립트 생성·조회 시 사용 (workOriId, workDataPk, fileName). */
    WorkListResponse findByWorkSeq(int workSeq);

    /** 이행스크립트 파일명 업데이트. */
    void updateFileName(@Param("workSeq") int workSeq, @Param("fileName") String fileName);
}
