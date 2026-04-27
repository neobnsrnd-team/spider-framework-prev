package com.example.admin_demo.domain.worktask.mapper;

import com.example.admin_demo.domain.worktask.dto.WorkTaskResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <h3>작업함 Mapper</h3>
 * <p>FWK_USER_WORK_TASK, FWK_USER_MENU 데이터 변경 및 조회를 담당합니다.</p>
 */
@Mapper
public interface WorkTaskMapper {

    /**
     * 사용자 작업함 목록을 조회합니다.
     * FWK_USER_MENU JOIN FWK_MENU LEFT JOIN FWK_USER_WORK_TASK LEFT JOIN FWK_USER_WORK_GROUP
     * @param userId 사용자 ID
     * @param workGroupId 그룹 필터 (null 이면 전체 조회)
     */
    List<WorkTaskResponse> findWorkTasks(@Param("userId") String userId, @Param("workGroupId") String workGroupId);

    /**
     * 작업함 항목 그룹을 이동합니다 (Oracle MERGE INTO).
     * FWK_USER_WORK_TASK 레코드가 없으면 INSERT, 있으면 UPDATE.
     * @param userId 사용자 ID
     * @param menuId 메뉴 ID
     * @param workGroupId 이동할 그룹 ID (null = 미분류)
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     */
    int upsertWorkTask(
            @Param("userId") String userId,
            @Param("menuId") String menuId,
            @Param("workGroupId") String workGroupId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 권한이양: fromUserId 의 FWK_USER_MENU 레코드를 toUserId 로 복사합니다 (이미 존재하면 스킵).
     * @param fromUserId 이양 출처 사용자 ID
     * @param toUserId   이양 대상 사용자 ID
     * @param menuIds    이양할 메뉴 ID 목록
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     */
    int insertUserMenuIfNotExists(
            @Param("fromUserId") String fromUserId,
            @Param("toUserId") String toUserId,
            @Param("menuIds") List<String> menuIds,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 권한이양: fromUserId 의 FWK_USER_MENU 레코드를 삭제합니다.
     * @param userId  삭제 대상 사용자 ID
     * @param menuIds 삭제할 메뉴 ID 목록
     */
    int deleteUserMenuByMenuIds(@Param("userId") String userId, @Param("menuIds") List<String> menuIds);

    /**
     * fromUserId 의 FWK_USER_WORK_TASK 레코드를 삭제합니다 (권한이양 후 정리).
     * @param userId  삭제 대상 사용자 ID
     * @param menuIds 삭제할 메뉴 ID 목록
     */
    int deleteWorkTaskByMenuIds(@Param("userId") String userId, @Param("menuIds") List<String> menuIds);
}
