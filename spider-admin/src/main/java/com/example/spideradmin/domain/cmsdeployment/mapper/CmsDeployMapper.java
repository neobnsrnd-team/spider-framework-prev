package com.example.spideradmin.domain.cmsdeployment.mapper;

import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployHistoryRequest;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployHistoryResponse;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployPageRequest;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployPageResponse;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsServerInstanceResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** CMS 배포 관리 Mapper */
public interface CmsDeployMapper {

    // ── 배포 대상 페이지 목록 (APPROVED) ──────────────────────────────────────

    List<CmsDeployPageResponse> findApprovedPageList(
            @Param("req") CmsDeployPageRequest req, @Param("offset") long offset, @Param("endRow") long endRow);

    long countApprovedPageList(@Param("req") CmsDeployPageRequest req);

    // ── 배포 이력 ─────────────────────────────────────────────────────────────

    List<CmsDeployHistoryResponse> findHistoryList(
            @Param("req") CmsDeployHistoryRequest req, @Param("offset") long offset, @Param("endRow") long endRow);

    long countHistoryList(@Param("req") CmsDeployHistoryRequest req);

    // ── 배포 실행 지원 ────────────────────────────────────────────────────────

    /**
     * APPROVE_STATE = 'APPROVED' 인 페이지 HTML 조회.
     * CLOB을 String으로 직접 반환해 커넥션이 열린 상태에서 변환한다.
     * 페이지 미존재 시 null 반환.
     */
    String findApprovedPageHtml(@Param("pageId") String pageId);

    // ── 만료수동처리 ────────────────────────────────────────────────────────

    /**
     * 만료 처리 대상 검증 및 FILE_PATH 반환.
     * 조건: EXPIRED_DATE &lt; SYSDATE AND IS_PUBLIC='Y' AND USE_YN='Y'
     * 조건 미충족 시 null 반환 → 서비스에서 NotFoundException 처리.
     */
    String findExpirableFilePath(@Param("pageId") String pageId);

    /**
     * pageId에 대한 최신 배포 FILE_ID 반환 (예: PAGE001_v3.html).
     * 만료 FILE_ID 조합 시 버전 추출 용도.
     * 이력 없으면 null 반환.
     */
    String findLatestDeployedFileId(@Param("pageId") String pageId);

    /** ALIVE_YN='Y' 서버 인스턴스 목록 조회 — 만료 배포 전송 대상 */
    List<CmsServerInstanceResponse> findAliveServerInstances();

    /**
     * FWK_CMS_FILE_SEND_HIS UPSERT (MERGE ON INSTANCE_ID + FILE_ID).
     * MATCHED → UPDATE, NOT MATCHED → INSERT
     */
    void upsertFileSendHis(
            @Param("instanceId") String instanceId,
            @Param("fileId") String fileId,
            @Param("fileSize") long fileSize,
            @Param("crcValue") String crcValue,
            @Param("userId") String userId);

    /**
     * 만료 처리 완료 후 페이지 상태 업데이트.
     * IS_PUBLIC='N', FILE_PATH_BACK=FILE_PATH, LAST_MODIFIER_ID=userId
     */
    void expirePage(@Param("pageId") String pageId, @Param("userId") String userId);
}
