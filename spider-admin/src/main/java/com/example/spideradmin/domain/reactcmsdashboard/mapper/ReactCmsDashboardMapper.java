package com.example.spideradmin.domain.reactcmsdashboard.mapper;

import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsApprovalStatusResponse;
import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsDashboardListRequest;
import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsDashboardPageResponse;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * React CMS 사용자 대시보드 Mapper (SPW_CMS_PAGE, SPW_CMS_PAGE_HISTORY)
 *
 * <p>PAGE_TYPE = 'REACT' 조건을 모든 쿼리에 적용한다.
 * 새 페이지 생성은 react-cms 빌더에서 직접 수행하므로 INSERT 없음.
 */
@Mapper
public interface ReactCmsDashboardMapper {

    /** 내 페이지 목록 페이징 조회 */
    List<ReactCmsDashboardPageResponse> findMyPageList(
            @Param("req") ReactCmsDashboardListRequest req,
            @Param("userId") String userId,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /** 내 페이지 목록 건수 */
    long countMyPageList(@Param("req") ReactCmsDashboardListRequest req, @Param("userId") String userId);

    /** 이력 존재 여부 확인 — 삭제 분기(하드/소프트)에 사용 */
    int hasHistory(@Param("pageId") String pageId);

    /** 소프트 삭제 — USE_YN = 'N' (이력 있는 페이지). 영향받은 행 수 반환 */
    int deleteSoft(@Param("pageId") String pageId, @Param("userId") String userId);

    /** 하드 삭제 — 물리 행 삭제 (이력 없는 페이지). 영향받은 행 수 반환 */
    int deleteHard(@Param("pageId") String pageId, @Param("userId") String userId);

    /**
     * 승인자 이름 조회 — 클라이언트 전달값 대신 서버에서 직접 조회하여 위변조 방지
     *
     * @return 승인자 USER_NAME, 없으면 null
     */
    String findApproverNameById(@Param("approverId") String approverId);

    /**
     * 페이지 승인 상태 조회
     *
     * @return 승인 상태·반려 사유, 페이지 없으면 null
     */
    ReactCmsApprovalStatusResponse findApprovalStatus(@Param("pageId") String pageId);

    /** 승인 요청 — APPROVE_STATE = 'PENDING', 승인자 정보 저장. 영향받은 행 수 반환 */
    int requestApproval(
            @Param("pageId") String pageId,
            @Param("approverId") String approverId,
            @Param("approverName") String approverName,
            @Param("beginningDate") LocalDate beginningDate,
            @Param("expiredDate") LocalDate expiredDate,
            @Param("userId") String userId);
}
