package com.example.admin_demo.domain.userworkgroup.mapper;

import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupCreateRequest;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupResponse;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <h3>사용자 작업 그룹 Mapper</h3>
 * <p>FWK_USER_WORK_GROUP 테이블의 데이터 변경(insert, update, delete) 및 조회를 담당합니다.</p>
 */
@Mapper
public interface UserWorkGroupMapper {

    /**
     * 사용자 작업 그룹을 생성합니다.
     * @param dto 생성 요청 DTO
     * @param groupId 자동 생성된 그룹 ID
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     */
    int insertUserWorkGroup(
            @Param("dto") UserWorkGroupCreateRequest dto,
            @Param("groupId") String groupId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 사용자 작업 그룹을 수정합니다.
     * @param dto 수정 요청 DTO (userId + groupId = PK)
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     */
    int updateUserWorkGroup(
            @Param("dto") UserWorkGroupUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 사용자 작업 그룹을 삭제합니다.
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     */
    int deleteUserWorkGroup(@Param("userId") String userId, @Param("groupId") String groupId);

    /**
     * 복합 PK로 존재 여부를 확인합니다.
     * @return 존재하면 1, 없으면 0
     */
    int existsByPk(@Param("userId") String userId, @Param("groupId") String groupId);

    /**
     * 복합 PK로 단건 조회합니다.
     */
    UserWorkGroupResponse selectResponseByPk(@Param("userId") String userId, @Param("groupId") String groupId);

    /**
     * 사용자 ID 기준으로 작업 그룹 목록을 조회합니다. (GROUP_ORDER 오름차순)
     */
    List<UserWorkGroupResponse> findAllByUserId(@Param("userId") String userId);
}
