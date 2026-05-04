package com.example.spideradmin.domain.reactcmsadminapproval.service;

import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalApproveRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalHistoryResponse;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalListRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalListResponse;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalPublicStateRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalRejectRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalRollbackRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.mapper.ReactCmsAdminApprovalMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * React CMS Admin 승인 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactCmsAdminApprovalService {

    private final ReactCmsAdminApprovalMapper reactCmsAdminApprovalMapper;

    /** 승인 관리 목록 조회 */
    public PageResponse<ReactCmsAdminApprovalListResponse> findPageList(
            ReactCmsAdminApprovalListRequest req, PageRequest pageRequest) {

        long total = reactCmsAdminApprovalMapper.countPageList(req);
        List<ReactCmsAdminApprovalListResponse> list =
                reactCmsAdminApprovalMapper.findPageList(req, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 승인 확정 — APPROVE_STATE: PENDING → APPROVED
     * 상태를 먼저 변경한 뒤 변경 후 상태를 이력 스냅샷으로 저장한다.
     */
    @Transactional
    public void approve(String pageId, ReactCmsAdminApprovalApproveRequest req, String modifierId) {
        checkPageExists(pageId);
        validateDisplayPeriod(req.getBeginningDate(), req.getExpiredDate());
        reactCmsAdminApprovalMapper.approve(pageId, req.getBeginningDate(), req.getExpiredDate(), modifierId);
        int version = reactCmsAdminApprovalMapper.getNextVersion(pageId);
        reactCmsAdminApprovalMapper.insertHistory(pageId, version);
        log.info("React CMS Admin 페이지 승인 완료: pageId={}, version={}, modifierId={}", pageId, version, modifierId);
    }

    /**
     * 반려 — APPROVE_STATE: PENDING → REJECTED
     * 상태를 먼저 변경한 뒤 변경 후 상태를 이력 스냅샷으로 저장한다.
     */
    @Transactional
    public void reject(String pageId, ReactCmsAdminApprovalRejectRequest req, String modifierId) {
        checkPageExists(pageId);
        reactCmsAdminApprovalMapper.reject(pageId, req.getRejectedReason(), modifierId);
        int version = reactCmsAdminApprovalMapper.getNextVersion(pageId);
        reactCmsAdminApprovalMapper.insertHistory(pageId, version);
        log.info("React CMS Admin 페이지 반려 완료: pageId={}, version={}, modifierId={}", pageId, version, modifierId);
    }

    /** 공개 상태 변경 */
    @Transactional
    public void updatePublicState(String pageId, ReactCmsAdminApprovalPublicStateRequest req, String modifierId) {
        checkPageExists(pageId);
        reactCmsAdminApprovalMapper.updatePublicState(pageId, req.getIsPublic(), modifierId);
    }

    /** 승인 이력 목록 조회 */
    public List<ReactCmsAdminApprovalHistoryResponse> findHistoryList(String pageId) {
        checkPageExists(pageId);
        return reactCmsAdminApprovalMapper.findHistoryList(pageId);
    }

    /**
     * 롤백 — 지정 버전 이력으로 SPW_CMS_PAGE 복원, APPROVE_STATE → WORK
     */
    @Transactional
    public void rollback(String pageId, ReactCmsAdminApprovalRollbackRequest req, String modifierId) {
        checkPageExists(pageId);
        Map<String, Object> history = reactCmsAdminApprovalMapper.findHistoryByVersion(pageId, req.getVersion());
        if (history == null) {
            throw new NotFoundException("해당 버전의 이력을 찾을 수 없습니다. pageId=" + pageId + ", version=" + req.getVersion());
        }
        String pageHtml = (String) history.get("PAGE_HTML");
        String filePath = (String) history.get("FILE_PATH");
        reactCmsAdminApprovalMapper.rollback(pageId, pageHtml, filePath, modifierId);
        log.info(
                "React CMS Admin 페이지 롤백 완료: pageId={}, version={}, modifierId={}",
                pageId,
                req.getVersion(),
                modifierId);
    }

    private void checkPageExists(String pageId) {
        if (reactCmsAdminApprovalMapper.existsByPageId(pageId) == 0) {
            throw new NotFoundException("페이지를 찾을 수 없습니다. pageId=" + pageId);
        }
    }

    /**
     * 노출 기간 유효성 검사 — 날짜는 선택값
     *
     * <p>승인 요청 시 날짜를 지정하지 않을 수 있으므로 null/blank는 허용한다.
     * 두 날짜가 모두 입력된 경우에만 순서(종료일 >= 시작일)를 검사한다.
     */
    private void validateDisplayPeriod(String beginningDate, String expiredDate) {
        boolean hasBeginning = beginningDate != null && !beginningDate.isBlank();
        boolean hasExpired = expiredDate != null && !expiredDate.isBlank();

        if (hasBeginning && hasExpired) {
            LocalDate beginning = parseDate(beginningDate, "노출 시작일");
            LocalDate expired = parseDate(expiredDate, "노출 종료일");
            if (expired.isBefore(beginning)) {
                throw new InvalidInputException("노출 종료일은 시작일보다 빠를 수 없습니다.");
            }
        }
    }

    private LocalDate parseDate(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidInputException(label + "은 YYYY-MM-DD 형식이어야 합니다.");
        }
    }
}
