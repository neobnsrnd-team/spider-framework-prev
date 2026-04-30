package com.example.spideradmin.domain.reactcmsadmindeployment.mapper;

import com.example.spideradmin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryRequest;
import com.example.spideradmin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryResponse;
import com.example.spideradmin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageRequest;
import com.example.spideradmin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** React CMS Admin 배포 관리 Mapper */
public interface ReactCmsAdminDeployMapper {

    // ── 배포 대상 페이지 목록 (PAGE_TYPE='REACT', APPROVED) ──────────────────────

    List<ReactCmsAdminDeployPageResponse> findApprovedPageList(
            @Param("req") ReactCmsAdminDeployPageRequest req,
            @Param("offset") long offset,
            @Param("endRow") long endRow);

    long countApprovedPageList(@Param("req") ReactCmsAdminDeployPageRequest req);

    // ── 배포 이력 ─────────────────────────────────────────────────────────────

    List<ReactCmsAdminDeployHistoryResponse> findHistoryList(
            @Param("req") ReactCmsAdminDeployHistoryRequest req,
            @Param("offset") long offset,
            @Param("endRow") long endRow);

    long countHistoryList(@Param("req") ReactCmsAdminDeployHistoryRequest req);

    // ── 배포 실행 지원 ────────────────────────────────────────────────────────

    /**
     * PAGE_TYPE='REACT', APPROVE_STATE='APPROVED' 인 페이지 존재 여부 확인.
     * CLOB 전체 조회 대신 COUNT 쿼리를 사용해 대용량 데이터 로드를 방지한다.
     * 0이면 미존재, 1이면 존재.
     */
    int existsApprovedPage(@Param("pageId") String pageId);

    /**
     * 승인된 React 페이지의 PAGE_DESC(사전 생성된 JSX 코드) 조회.
     * 로컬 파일 배포 시 이 코드를 {pageId}.tsx 파일로 저장한다.
     */
    String findPageDescById(@Param("pageId") String pageId);

    /**
     * 특정 pageId에 대한 최대 배포 버전 번호 조회.
     * FILE_ID 패턴 {pageId}_v{n}.(html|tsx) 에서 n의 최댓값을 반환하며, 이력이 없으면 0을 반환한다.
     */
    int findMaxDeployVersion(@Param("pageId") String pageId);

    /** 배포 이력을 FWK_CMS_FILE_SEND_HIS 에 INSERT */
    void insertDeployHistory(
            @Param("instanceId") String instanceId,
            @Param("fileId") String fileId,
            @Param("fileSize") long fileSize,
            @Param("fileCrcValue") String fileCrcValue,
            @Param("lastModifierId") String lastModifierId);
}
