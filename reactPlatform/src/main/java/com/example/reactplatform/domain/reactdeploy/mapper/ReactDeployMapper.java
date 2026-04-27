/**
 * @file ReactDeployMapper.java
 * @description FWK_REACT_DEPLOY_HIS 테이블 CRUD MyBatis 매퍼.
 */
package com.example.reactplatform.domain.reactdeploy.mapper;

import com.example.reactplatform.domain.reactdeploy.dto.ReactDeployHistoryResponse;
import com.example.reactplatform.domain.reactdeploy.dto.ReactDeployListResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReactDeployMapper {

    /**
     * 배포 이력을 FWK_REACT_DEPLOY_HIS에 INSERT한다.
     *
     * @param deployId          UUID PK
     * @param codeId            FK → FWK_REACT_CODE_HIS
     * @param deployMode        {@code local | git-pr}
     * @param deployStatus      {@code SUCCESS | FAILED}
     * @param failReason        실패 사유 (null 가능)
     * @param prUrl             PR URL — git-pr 모드 (null 가능)
     * @param lastUpdateDtime   배포 일시 (yyyyMMddHHmmss)
     * @param lastUpdateUserId  배포 실행자 ID
     */
    void insert(
            @Param("deployId")         String deployId,
            @Param("codeId")           String codeId,
            @Param("deployMode")       String deployMode,
            @Param("deployStatus")     String deployStatus,
            @Param("failReason")       String failReason,
            @Param("prUrl")            String prUrl,
            @Param("lastUpdateDtime")  String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId
    );

    /**
     * APPROVED 상태 코드 목록에 각 코드의 최근 배포 이력 1건을 LEFT JOIN하여 반환한다.
     *
     * @param offset  시작 행 오프셋 (0-based)
     * @param endRow  종료 행 번호
     * @param search  코드 ID 또는 요청자 ID 키워드 (null·빈 문자열이면 전체)
     */
    List<ReactDeployListResponse> selectDeployList(
            @Param("offset") int offset,
            @Param("endRow") int endRow,
            @Param("search") String search
    );

    /** 배포 가능 목록 전체 건수 */
    int selectDeployListCount(@Param("search") String search);

    /**
     * 전체 배포 이력 목록을 최근 순으로 반환한다.
     *
     * @param offset  시작 행 오프셋 (0-based)
     * @param endRow  종료 행 번호
     * @param search  코드 ID 또는 실행자 ID 키워드 (null·빈 문자열이면 전체)
     * @param userId  실행자 ID 일치 필터 (null이면 전체)
     */
    List<ReactDeployHistoryResponse> selectAllHistoryList(
            @Param("offset") int offset,
            @Param("endRow") int endRow,
            @Param("search") String search,
            @Param("userId") String userId
    );

    /** 전체 배포 이력 건수 */
    int selectAllHistoryCount(@Param("search") String search, @Param("userId") String userId);

    /**
     * 특정 코드의 배포 이력 목록을 최근 순으로 반환한다 (모달용).
     *
     * @param codeId  조회할 코드 ID
     * @param offset  시작 행 오프셋
     * @param endRow  종료 행 번호
     */
    List<ReactDeployHistoryResponse> selectHistoryByCodeId(
            @Param("codeId")  String codeId,
            @Param("offset")  int offset,
            @Param("endRow")  int endRow
    );

    /** 특정 코드의 배포 이력 건수 */
    int selectHistoryCountByCodeId(@Param("codeId") String codeId);
}
