package com.example.spider_admin.domain.cmsdashboard.mapper;

import com.example.spider_admin.domain.cmsdashboard.dto.CmsDashboardListRequest;
import com.example.spider_admin.domain.cmsdashboard.dto.CmsDashboardPageResponse;
import com.example.spider_admin.domain.cmsdashboard.dto.CmsTemplateResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CMS 사용자 대시보드 Mapper (SPW_CMS_PAGE, SPW_CMS_PAGE_HISTORY)
 */
@Mapper
public interface CmsDashboardMapper {

    /** 내 페이지 목록 페이징 조회 */
    List<CmsDashboardPageResponse> findMyPageList(
            @Param("req") CmsDashboardListRequest req,
            @Param("userId") String userId,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /** 내 페이지 목록 건수 */
    long countMyPageList(@Param("req") CmsDashboardListRequest req, @Param("userId") String userId);

    /** 페이지 존재 여부 확인 (소유권 포함 — 본인 페이지만 허용) */
    int existsByPageIdAndUserId(@Param("pageId") String pageId, @Param("userId") String userId);

    /** 새 페이지 INSERT */
    void insertPage(
            @Param("pageId") String pageId,
            @Param("pageName") String pageName,
            @Param("viewMode") String viewMode,
            @Param("templateId") String templateId,
            @Param("userId") String userId,
            @Param("userName") String userName);

    /**
     * 템플릿 목록 조회 — PAGE_TYPE = 'TEMPLATE', USE_YN = 'Y'
     * 페이지 생성 모달의 템플릿 선택 목록에 사용된다.
     */
    List<CmsTemplateResponse> findTemplateList();

    /** 단건 템플릿 조회 — 생성 시 레이아웃 일치 여부 검증용 */
    CmsTemplateResponse findTemplateById(@Param("templateId") String templateId);

    /**
     * 템플릿 존재 여부 확인 — createPage 시 templateId 유효성 검증용.
     * PAGE_TYPE = 'TEMPLATE' AND USE_YN = 'Y' 인 페이지인지 확인한다.
     */
    int existsTemplate(@Param("templateId") String templateId);

    /** 이력 존재 여부 확인 — 삭제 분기(하드/소프트)에 사용 */
    int hasHistory(@Param("pageId") String pageId);

    /** 소프트 삭제 — USE_YN = 'N' (이력 있는 페이지) */
    void deleteSoft(@Param("pageId") String pageId, @Param("userId") String userId);

    /** 하드 삭제 — 물리 행 삭제 (이력 없는 페이지) */
    void deleteHard(@Param("pageId") String pageId, @Param("userId") String userId);

    /**
     * 승인자 이름 조회 — 클라이언트 전달값 대신 서버에서 직접 조회하여 위변조 방지
     *
     * @return 승인자 USER_NAME, 없으면 null
     */
    String findApproverNameById(@Param("approverId") String approverId);

    /** 승인 요청 — APPROVE_STATE = 'PENDING', 승인자 정보 저장 */
    int requestApproval(
            @Param("pageId") String pageId,
            @Param("approverId") String approverId,
            @Param("approverName") String approverName,
            @Param("beginningDate") String beginningDate,
            @Param("expiredDate") String expiredDate,
            @Param("userId") String userId);
}
