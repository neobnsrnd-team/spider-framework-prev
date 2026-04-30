package com.example.spideradmin.domain.cmsasset.service;

import com.example.spideradmin.domain.cmsasset.client.CmsBuilderClient;
import com.example.spideradmin.domain.cmsasset.client.dto.CmsBuilderUploadApiResponse;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetApprovalListRequest;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetDetailResponse;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetRequestListRequest;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetUploadResponse;
import com.example.spideradmin.domain.cmsasset.mapper.CmsAssetMapper;
import com.example.spideradmin.domain.cmsasset.validator.AssetUploadValidator;
import com.example.spideradmin.domain.code.dto.CodeResponse;
import com.example.spideradmin.domain.code.service.CodeService;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.ErrorType;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.InvalidStateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.exception.base.BaseException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * CMS 자산 업로드, 승인, 공개 여부 변경을 담당하는 서비스.
 *
 * <p>이미지 자산은 단순 파일 저장으로 끝나지 않고 승인 상태 전이, CMS Builder 연동,
 * 업로더 권한 검증, 배포 실패 보상 처리까지 함께 다뤄야 한다.
 * 이 서비스는 Admin의 자산 운영 규칙과 외부 CMS 연동을 한곳에 모아 상태 일관성을 유지하기 위해 필요하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsAssetService {

    public static final String ASSET_CATEGORY_CODE_GROUP_ID = "CMS00001";
    public static final String DEFAULT_BUSINESS_CATEGORY = "COMMON";

    private static final String STATE_WORK = "WORK";
    private static final String STATE_PENDING = "PENDING";
    private static final String STATE_APPROVED = "APPROVED";
    private static final String STATE_REJECTED = "REJECTED";
    private static final String USE_Y = "Y";
    private static final String USE_N = "N";
    private static final int REJECTED_REASON_MAX_CHARS = 1000;
    private static final int REJECTED_REASON_MAX_BYTES = 1000;

    /** ASSET_NAME VARCHAR2(200) — Oracle UTF-8 기준 한글 1자 = 3바이트이므로 200바이트 제한 */
    private static final int ASSET_NAME_MAX_BYTES = 200;

    private final CmsAssetMapper cmsAssetMapper;
    private final CodeService codeService;
    private final CmsBuilderClient cmsBuilderClient;
    private final AssetUploadValidator assetUploadValidator;
    private final TransactionTemplate transactionTemplate;

    /** 현재 사용자가 등록한 자산 요청 목록을 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<CmsAssetListResponse> findMyRequestList(
            String currentUserId, CmsAssetRequestListRequest req, PageRequest pageRequest) {

        req.setCreateUserId(currentUserId);

        long total = cmsAssetMapper.countMyList(req);
        List<CmsAssetListResponse> list =
                cmsAssetMapper.findMyList(req, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 승인 담당자가 검토할 자산 승인 목록을 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<CmsAssetListResponse> findApprovalList(
            CmsAssetApprovalListRequest req, PageRequest pageRequest) {

        long total = cmsAssetMapper.countApprovalList(req);
        List<CmsAssetListResponse> list =
                cmsAssetMapper.findApprovalList(req, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 업로드 화면에서 사용할 활성 자산 카테고리 코드만 반환한다. */
    @Transactional(readOnly = true)
    public List<CodeResponse> getAssetCategoryCodes() {
        // 코드그룹의 USE_YN='Y' 항목을 그대로 노출. 카테고리 추가·폐기는 코드그룹 관리로 일원화한다.
        return codeService.getCodesByCodeGroupId(ASSET_CATEGORY_CODE_GROUP_ID).stream()
                .filter(code -> USE_Y.equalsIgnoreCase(code.getUseYn()))
                .toList();
    }

    /** 자산 상세 모달에 필요한 메타데이터를 조회한다. */
    @Transactional(readOnly = true)
    public CmsAssetDetailResponse findById(String assetId) {
        CmsAssetDetailResponse detail = cmsAssetMapper.findDetailById(assetId);
        if (detail == null) {
            throw new NotFoundException("이미지를 찾을 수 없습니다. assetId=" + assetId);
        }
        return detail;
    }

    /** 작업 중인 자산을 승인 대기 상태로 전환한다. */
    @Transactional
    public void requestApproval(String assetId, String modifierId, String modifierName) {
        assertTransition(assetId, STATE_WORK, STATE_PENDING);
        int updated = cmsAssetMapper.updateState(assetId, STATE_WORK, STATE_PENDING, null, modifierId, modifierName);
        if (updated != 1) {
            throw new InvalidStateException("이미 처리된 이미지입니다. assetId=" + assetId);
        }
        log.info("CMS 이미지 승인 요청: assetId={}, modifierId={}", assetId, modifierId);
    }

    /**
     * 승인 — PENDING → APPROVED (결재자) + CMS 파일 배포 (Issue #55).
     *
     * <p>Saga 패턴으로 DB 와 CMS 를 조율한다. CMS 배포 API 가 호출 시점에 DB 의 {@code ASSET_STATE}
     * 가 이미 {@code APPROVED} 여야 하므로, 단일 {@code @Transactional} 로 감싸면 CMS 가 커밋 전
     * PENDING 만 보게 되어 항상 거절된다.
     *
     * <h4>흐름</h4>
     * <ol>
     *   <li>선검증 + UPDATE(PENDING→APPROVED) — {@code TransactionTemplate} 으로 독립 TX 커밋.</li>
     *   <li>{@link CmsBuilderClient#deployAsset(String)} 호출.</li>
     *   <li>성공 → 정상 종료.</li>
     *   <li>실패 → 보상 UPDATE(APPROVED→PENDING) 을 또 다른 독립 TX 로 커밋 후, 원 {@link BaseException}
     *       을 그대로 재던져 502 를 전파. 사용자 관점에서는 "둘 다 실패" 로 보인다.</li>
     * </ol>
     *
     * <h4>한계</h4>
     * <ul>
     *   <li>메인 TX 커밋과 CMS 호출 사이의 짧은 윈도우에 다른 결재자 화면은 {@code APPROVED} 를 볼 수 있다.</li>
     *   <li>보상 UPDATE 자체가 실패하는 초희귀 케이스는 error 로그만 남기고 수동 복구 대상으로 삼는다.</li>
     * </ul>
     */
    public void approve(String assetId, String modifierId, String modifierName) {
        // Step 1: 선검증 + 상태 UPDATE 를 독립 TX 로 커밋. CMS 가 APPROVED 를 읽어야 deploy 가 성공한다.
        transactionTemplate.executeWithoutResult(status -> {
            assertTransition(assetId, STATE_PENDING, STATE_APPROVED);
            int updated =
                    cmsAssetMapper.updateState(assetId, STATE_PENDING, STATE_APPROVED, null, modifierId, modifierName);
            if (updated != 1) {
                // 선검증 통과 후 race 실패 — 예외 던지면 TransactionTemplate 이 롤백.
                throw new InvalidStateException("이미 처리된 이미지입니다. assetId=" + assetId);
            }
        });

        // Step 2: CMS 파일 배포 시도.
        try {
            cmsBuilderClient.deployAsset(assetId);
            log.info("CMS 이미지 승인 및 배포 완료: assetId={}, modifierId={}", assetId, modifierId);
        } catch (BaseException deployEx) {
            // Step 3: 배포 실패 — 승인 상태를 PENDING 으로 보상 롤백.
            log.error("CMS 이미지 배포 실패로 승인 상태 롤백 시도: assetId={}, modifierId={}", assetId, modifierId, deployEx);
            try {
                transactionTemplate.executeWithoutResult(status -> cmsAssetMapper.updateState(
                        assetId, STATE_APPROVED, STATE_PENDING, null, modifierId, modifierName));
            } catch (RuntimeException revertEx) {
                // 보상 실패는 데이터-파일 불일치 상태를 남기므로 수동 복구 알림 차원의 error 로깅.
                log.error("승인 롤백 실패. 수동 확인 필요: assetId={}", assetId, revertEx);
            }
            throw deployEx;
        }
    }

    /**
     * 일반 자산 업로드를 수행하고 CMS Builder가 발급한 자산 ID와 URL을 반환한다.
     *
     * <p>상태 전이는 하지 않고 파일 검증, 카테고리 정규화, 외부 업로드 연동만 담당한다.
     */
    public CmsAssetUploadResponse uploadAsset(
            MultipartFile file,
            String assetName,
            String businessCategory,
            String assetDesc,
            String uploaderId,
            String uploaderName) {

        validateAssetNameBytes(assetName);
        assetUploadValidator.validate(file);
        String normalizedCategory = normalizeBusinessCategory(businessCategory);
        CmsBuilderUploadApiResponse cmsResponse =
                cmsBuilderClient.upload(file, assetName, uploaderId, uploaderName, normalizedCategory, assetDesc);
        return CmsAssetUploadResponse.builder()
                .assetId(cmsResponse.getAssetId())
                .url(cmsResponse.getUrl())
                .build();
    }

    /**
     * 관리자가 업로드 즉시 승인까지 끝내는 자산 등록 흐름을 처리한다.
     *
     * <p>운영자가 별도 승인 단계를 생략해야 할 때 업로드 후 바로 APPROVED 전이와 배포까지 연속 수행한다.
     */
    public CmsAssetUploadResponse uploadApprovedAsset(
            MultipartFile file,
            String assetName,
            String businessCategory,
            String assetDesc,
            String uploaderId,
            String uploaderName) {

        CmsAssetUploadResponse response =
                uploadAsset(file, assetName, businessCategory, assetDesc, uploaderId, uploaderName);

        transactionTemplate.executeWithoutResult(status -> {
            assertTransition(response.getAssetId(), STATE_WORK, STATE_APPROVED);
            int updated = cmsAssetMapper.updateState(
                    response.getAssetId(), STATE_WORK, STATE_APPROVED, null, uploaderId, uploaderName);
            if (updated != 1) {
                throw new InvalidStateException("이미 처리된 이미지입니다. assetId=" + response.getAssetId());
            }
        });

        try {
            cmsBuilderClient.deployAsset(response.getAssetId());
            log.info("CMS 관리자 이미지 즉시 승인 업로드 완료: assetId={}, uploaderId={}", response.getAssetId(), uploaderId);
            return response;
        } catch (BaseException deployEx) {
            log.error(
                    "CMS 관리자 이미지 즉시 승인 배포 실패. WORK 복구 시도: assetId={}, uploaderId={}",
                    response.getAssetId(),
                    uploaderId,
                    deployEx);
            try {
                transactionTemplate.executeWithoutResult(status -> cmsAssetMapper.updateState(
                        response.getAssetId(), STATE_APPROVED, STATE_WORK, null, uploaderId, uploaderName));
            } catch (RuntimeException revertEx) {
                log.error("관리자 업로드 롤백 실패. 수동 확인 필요: assetId={}", response.getAssetId(), revertEx);
            }
            throw deployEx;
        }
    }

    /**
     * 본인이 업로드한 자산만 삭제한다.
     *
     * <p>승인 흐름에 들어간 자산이 임의로 제거되지 않도록 WORK 또는 REJECTED 상태만 삭제를 허용한다.
     */
    public void deleteMyAsset(String assetId, String userId) {
        String createUserId = cmsAssetMapper.findCreateUserIdByAssetId(assetId);
        if (createUserId == null) {
            throw new NotFoundException("이미지를 찾을 수 없습니다. assetId=" + assetId);
        }
        if (!createUserId.equals(userId)) {
            log.warn("CMS 이미지 삭제 권한 없음. assetId={}, owner={}, requester={}", assetId, createUserId, userId);
            throw new BaseException(ErrorType.FORBIDDEN, "본인이 업로드한 이미지만 삭제할 수 있습니다.");
        }

        String currentState = cmsAssetMapper.findAssetStateById(assetId);
        if (!STATE_WORK.equals(currentState) && !STATE_REJECTED.equals(currentState)) {
            throw new InvalidStateException(
                    String.format("현재 상태(%s)에서는 삭제할 수 없습니다. 허용 상태는 WORK 또는 REJECTED 입니다.", currentState));
        }

        cmsBuilderClient.delete(assetId, userId);
        log.info("CMS 이미지 삭제 요청 완료: assetId={}, prevState={}, userId={}", assetId, currentState, userId);
    }

    /** 승인 대기 자산을 반려하고 사유를 함께 기록한다. */
    @Transactional
    public void reject(String assetId, String rejectedReason, String modifierId, String modifierName) {
        assertTransition(assetId, STATE_PENDING, STATE_REJECTED);
        String reason = normalizeReason(rejectedReason);
        int updated =
                cmsAssetMapper.updateState(assetId, STATE_PENDING, STATE_REJECTED, reason, modifierId, modifierName);
        if (updated != 1) {
            throw new InvalidStateException("이미 처리된 이미지입니다. assetId=" + assetId);
        }
        log.info("CMS 이미지 반려 완료: assetId={}, modifierId={}", assetId, modifierId);
    }

    /** 자산의 실제 노출 가능 여부를 변경한다. */
    @Transactional
    public void updateVisibility(String assetId, String useYn, String modifierId, String modifierName) {
        ensureAssetExists(assetId);
        String normalizedUseYn = normalizeUseYn(useYn);
        int updated = cmsAssetMapper.updateUseYn(assetId, normalizedUseYn, modifierId, modifierName);
        if (updated != 1) {
            throw new InvalidStateException("이미 처리된 이미지입니다. assetId=" + assetId);
        }
        log.info("CMS 이미지 노출 여부 변경: assetId={}, useYn={}, modifierId={}", assetId, normalizedUseYn, modifierId);
    }

    private void assertTransition(String assetId, String expectedFrom, String target) {
        String current = cmsAssetMapper.findAssetStateById(assetId);
        if (current == null) {
            throw new NotFoundException("이미지를 찾을 수 없습니다. assetId=" + assetId);
        }
        if (!expectedFrom.equals(current)) {
            throw new InvalidStateException(
                    String.format("현재 상태(%s)에서는 %s 전이를 수행할 수 없습니다. 필요 상태=%s", current, target, expectedFrom));
        }
    }

    private void ensureAssetExists(String assetId) {
        if (cmsAssetMapper.existsByAssetId(assetId) != 1) {
            throw new NotFoundException("이미지를 찾을 수 없습니다. assetId=" + assetId);
        }
    }

    /**
     * 이미지명의 바이트 길이를 검증한다.
     *
     * <p>Oracle VARCHAR2(200)은 바이트 단위 제한이므로, 한글 1자 = 3바이트 기준으로
     * 200바이트를 초과하면 ORA-12899가 발생한다. 서버에서 사전 차단한다.
     */
    private void validateAssetNameBytes(String assetName) {
        if (assetName == null) return;
        if (assetName.getBytes(StandardCharsets.UTF_8).length > ASSET_NAME_MAX_BYTES) {
            throw new InvalidInputException(
                    "이미지명이 너무 깁니다. UTF-8 기준 " + ASSET_NAME_MAX_BYTES + "바이트 이하로 입력해 주세요. (한글 약 66자)");
        }
    }

    private String normalizeReason(String rejectedReason) {
        if (rejectedReason == null) {
            return null;
        }
        String trimmed = rejectedReason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > REJECTED_REASON_MAX_CHARS) {
            throw new InvalidInputException("반려 사유는 " + REJECTED_REASON_MAX_CHARS + "자 이하로 입력해 주세요.");
        }
        if (trimmed.getBytes(StandardCharsets.UTF_8).length > REJECTED_REASON_MAX_BYTES) {
            throw new InvalidInputException("반려 사유는 UTF-8 기준 " + REJECTED_REASON_MAX_BYTES + "바이트 이하로 입력해 주세요.");
        }
        return trimmed;
    }

    private String normalizeBusinessCategory(String businessCategory) {
        String normalized = (businessCategory == null || businessCategory.isBlank())
                ? DEFAULT_BUSINESS_CATEGORY
                : businessCategory.trim();

        boolean exists = getAssetCategoryCodes().stream().anyMatch(code -> normalized.equals(code.getCode()));
        if (!exists) {
            throw new InvalidInputException("유효하지 않은 이미지 카테고리입니다.");
        }
        return normalized;
    }

    private String normalizeUseYn(String useYn) {
        if (useYn == null || useYn.isBlank()) {
            throw new InvalidInputException("노출 여부 값이 필요합니다.");
        }
        String normalized = useYn.trim().toUpperCase();
        if (!USE_Y.equals(normalized) && !USE_N.equals(normalized)) {
            throw new InvalidInputException("유효하지 않은 노출 여부 값입니다.");
        }
        return normalized;
    }
}
