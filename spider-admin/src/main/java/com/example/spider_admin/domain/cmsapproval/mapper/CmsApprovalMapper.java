package com.example.spider_admin.domain.cmsapproval.mapper;

import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalHistoryResponse;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalListRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalPageResponse;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalRollbackHistoryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CMS 승인 관리 Mapper (SPW_CMS_PAGE, SPW_CMS_PAGE_HISTORY)
 */
@Mapper
public interface CmsApprovalMapper {

    /** 승인 관리 목록 페이징 조회 */
    List<CmsApprovalPageResponse> findPageList(
            @Param("req") CmsApprovalListRequest req, @Param("offset") int offset, @Param("endRow") int endRow);

    /** 승인 관리 목록 건수 */
    long countPageList(@Param("req") CmsApprovalListRequest req);

    /** 페이지 존재 여부 확인 */
    int existsByPageId(@Param("pageId") String pageId);

    /** 승인 확정 — APPROVE_STATE → APPROVED */
    int approve(
            @Param("pageId") String pageId,
            @Param("beginningDate") String beginningDate,
            @Param("expiredDate") String expiredDate,
            @Param("modifierId") String modifierId);

    /** 반려 — APPROVE_STATE → REJECTED */
    int reject(
            @Param("pageId") String pageId,
            @Param("rejectedReason") String rejectedReason,
            @Param("modifierId") String modifierId);

    /** 공개 상태 변경 */
    void updatePublicState(
            @Param("pageId") String pageId, @Param("isPublic") String isPublic, @Param("modifierId") String modifierId);

    /** 노출 기간 수정 */
    void updateDisplayPeriod(
            @Param("pageId") String pageId,
            @Param("beginningDate") String beginningDate,
            @Param("expiredDate") String expiredDate,
            @Param("modifierId") String modifierId);

    /** 다음 이력 버전 번호 조회 */
    int getNextVersion(@Param("pageId") String pageId);

    /** 이력 스냅샷 INSERT (SPW_CMS_PAGE → SPW_CMS_PAGE_HISTORY) */
    void insertHistory(@Param("pageId") String pageId, @Param("version") int version);

    /** 이력 목록 조회 (버전 역순) */
    List<CmsApprovalHistoryResponse> findHistoryList(@Param("pageId") String pageId);

    /** 특정 버전 이력 조회 (롤백용 PAGE_HTML, FILE_PATH) */
    CmsApprovalRollbackHistoryResponse findHistoryByVersion(
            @Param("pageId") String pageId, @Param("version") int version);

    /** 롤백 — 지정 버전으로 복원 후 APPROVE_STATE → WORK */
    void rollback(
            @Param("pageId") String pageId,
            @Param("pageHtml") String pageHtml,
            @Param("filePath") String filePath,
            @Param("modifierId") String modifierId);
}
