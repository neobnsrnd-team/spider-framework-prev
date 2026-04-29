package com.example.spider_admin.domain.cmsasset.mapper;

import com.example.spider_admin.domain.cmsasset.dto.CmsAssetApprovalListRequest;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetDetailResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetRequestListRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CMS 이미지(에셋) 승인 관리 Mapper (SPW_CMS_ASSET).
 *
 * <p>상태 전이({@code WORK → PENDING → APPROVED / REJECTED})는
 * 서비스 계층에서 선검증하고, {@link #updateState}의 WHERE 가드로 이중 방어한다.
 */
@Mapper
public interface CmsAssetMapper {

    /** 현업 본인 업로드 이미지 목록 (페이징) */
    List<CmsAssetListResponse> findMyList(
            @Param("req") CmsAssetRequestListRequest req, @Param("offset") int offset, @Param("endRow") int endRow);

    /** 현업 본인 업로드 이미지 건수 */
    long countMyList(@Param("req") CmsAssetRequestListRequest req);

    /** 결재자 승인 관리 목록 (페이징) */
    List<CmsAssetListResponse> findApprovalList(
            @Param("req") CmsAssetApprovalListRequest req, @Param("offset") int offset, @Param("endRow") int endRow);

    /** 결재자 승인 관리 목록 건수 */
    long countApprovalList(@Param("req") CmsAssetApprovalListRequest req);

    /** 이미지 존재 여부 (USE_YN='Y') */
    int existsByAssetId(@Param("assetId") String assetId);

    /** 현재 ASSET_STATE 조회 (USE_YN='Y'). null 반환 시 미존재 또는 논리 삭제 */
    String findAssetStateById(@Param("assetId") String assetId);

    /** 업로더(소유자) ID 조회 (USE_YN='Y'). 소유자 검증용 경량 PK 조회 */
    String findCreateUserIdByAssetId(@Param("assetId") String assetId);

    /** 상세 조회 — 모달 프리뷰용 */
    CmsAssetDetailResponse findDetailById(@Param("assetId") String assetId);

    /**
     * 상태 전이 UPDATE — request / approve / reject 공통.
     *
     * <p>WHERE 절에 {@code expectedFrom}을 포함해 동시 실행 race 방지.
     * 0행 반환 시 서비스가 {@code InvalidStateException}을 던진다.
     *
     * @param assetId         대상 이미지 ID
     * @param expectedFrom    허용 현재 상태 (WORK / PENDING 등)
     * @param target          전이 목표 상태
     * @param rejectedReason  REJECTED 전이 시에만 사용 (그 외 null)
     * @param modifierId      최종 수정자 ID
     * @param modifierName    최종 수정자 이름
     * @return 업데이트된 row 수 (정상: 1, race 실패: 0)
     */
    int updateState(
            @Param("assetId") String assetId,
            @Param("expectedFrom") String expectedFrom,
            @Param("target") String target,
            @Param("rejectedReason") String rejectedReason,
            @Param("modifierId") String modifierId,
            @Param("modifierName") String modifierName);

    int updateUseYn(
            @Param("assetId") String assetId,
            @Param("useYn") String useYn,
            @Param("modifierId") String modifierId,
            @Param("modifierName") String modifierName);
}
