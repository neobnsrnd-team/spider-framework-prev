/**
 * @file ReactApprovalService.java
 * @description React 코드 승인 워크플로우를 전담하는 서비스.
 *     승인 대기 목록 조회, 승인, 반려 세 가지 오퍼레이션을 제공한다.
 *     승인 시 요청자 본인 여부를 서버에서 검증하여 클라이언트 우회를 방지한다.
 *     승인 완료 후 {@link com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployStrategy}에
 *     배포를 위임한다 ({@code react.deploy.mode} 설정으로 local / git-pr 중 선택).
 */
package com.example.reactplatform.domain.reactapproval.service;

import com.example.reactplatform.domain.reactdeploy.service.ReactDeployService;
import com.example.reactplatform.domain.reactgenerate.dto.ReactApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateHistoryResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.enums.ReactGenerateStatus;
import com.example.reactplatform.domain.reactgenerate.mapper.ReactGenerateMapper;
import com.example.reactplatform.global.exception.InvalidInputException;
import com.example.reactplatform.global.exception.NotFoundException;
import com.example.reactplatform.global.util.SqlUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactApprovalService {

    private final ReactGenerateMapper reactGenerateMapper;
    private final ReactDeployService reactDeployService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** yyyyMMdd 형식 검증 패턴 — 날짜 파라미터를 SQL에 전달하기 전 형식을 검증한다. */
    private static final Pattern YYYYMMDD = Pattern.compile("^\\d{8}$");

    /** 승인 이력 조회에서 허용되는 상태 값 — APPROVED 또는 REJECTED 외의 값은 거부한다. */
    private static final Set<String> ALLOWED_HISTORY_STATUSES = Set.of("APPROVED", "REJECTED");

    /**
     * 승인 대기(PENDING_APPROVAL) 목록과 전체 건수를 조회한다.
     *
     * @param page 페이지 번호 (1-based)
     * @param size 페이지당 건수
     * @return list(목록), totalCount(전체 건수), page, size
     */
    public Map<String, Object> getPendingList(int page, int size) {
        int offset = (page - 1) * size;
        int endRow = offset + size;
        List<ReactApprovalResponse> list = reactGenerateMapper.selectPendingList(offset, endRow);
        int totalCount = reactGenerateMapper.selectPendingCount();
        return Map.of("list", list, "totalCount", totalCount, "page", page, "size", size);
    }

    /**
     * 코드를 승인한다.
     *
     * <p>승인 조건:
     * <ul>
     *   <li>STATUS가 PENDING_APPROVAL인 코드만 승인 가능</li>
     *   <li>코드 요청자 본인은 승인 불가 — 클라이언트 우회 방지를 위해 서버에서 재검증</li>
     * </ul>
     *
     * <p>Race Condition 방지: WHERE 조건에 STATUS = PENDING_APPROVAL을 포함한 조건부 UPDATE를 사용하여
     * 동시 요청이 들어와도 DB 레벨에서 단 하나의 요청만 처리되도록 보장한다.
     *
     * @param id             승인할 코드 ID
     * @param approverUserId 승인자 ID (로그인 사용자)
     * @throws NotFoundException     해당 codeId가 존재하지 않을 때
     * @throws InvalidInputException PENDING_APPROVAL이 아닌 상태이거나, 요청자 본인이거나, 동시 요청으로 선점 실패 시
     */
    @Transactional
    public ReactGenerateApprovalResponse approve(String id, String approverUserId) {
        ReactGenerateResponse existing = requirePendingApproval(id);

        // 코드 요청자와 승인자가 동일한 경우 거부 — 클라이언트 우회 방지
        if (approverUserId.equals(existing.getCreateUserId())) {
            throw new InvalidInputException("코드 요청자는 승인할 수 없습니다.");
        }

        String now = LocalDateTime.now().format(FORMATTER);
        // PENDING_APPROVAL 상태인 경우에만 UPDATE (동시 승인 Race Condition 방지)
        // 두 요청이 동시에 들어와도 DB가 WHERE 조건으로 하나만 처리하고 나머지는 0행을 반환
        int updated = reactGenerateMapper.updateStatusConditional(
                id,
                ReactGenerateStatus.APPROVED.name(),
                approverUserId,
                now,
                null, // 승인 시 failReason은 null로 초기화
                ReactGenerateStatus.PENDING_APPROVAL.name());
        if (updated == 0) {
            // 선점 실패: 동시 요청이 먼저 상태를 변경함
            throw new InvalidInputException("이미 처리된 승인 요청입니다.");
        }
        log.info("승인 완료 — codeId: {}, approver: {}", id, approverUserId);

        // 트랜잭션 커밋 성공 후 배포를 실행하고 결과를 FWK_REACT_DEPLOY_HIS에 기록한다.
        // afterCommit에서 실행하여 DB 롤백 시 배포가 수행되지 않도록 보장한다.
        String reactCode = existing.getReactCode();
        String componentName = existing.getComponentName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reactDeployService.deployAndRecord(id, reactCode, componentName, approverUserId);
            }
        });

        return ReactGenerateApprovalResponse.builder()
                .codeId(id)
                .status(ReactGenerateStatus.APPROVED.name())
                .approvalUserId(approverUserId)
                .approvalDtime(now)
                .build();
    }

    /**
     * 코드를 반려한다.
     *
     * <p>반려는 상태에 관계없이 어느 단계에서든 가능하다.
     * (PENDING_APPROVAL, GENERATED, APPROVED 모두 반려 가능)
     *
     * @param id             반려할 코드 ID
     * @param rejectorUserId 반려자 ID (로그인 사용자)
     * @param reason         반려 사유 (nullable, 최대 500자)
     * @throws NotFoundException 해당 codeId가 존재하지 않을 때
     */
    public ReactGenerateApprovalResponse reject(String id, String rejectorUserId, String reason) {
        requireExists(id);

        String now = LocalDateTime.now().format(FORMATTER);
        // 반려 사유를 FAIL_REASON 컬럼에 저장
        reactGenerateMapper.updateStatus(id, ReactGenerateStatus.REJECTED.name(), rejectorUserId, now, reason);
        log.info("반려 완료 — codeId: {}, rejector: {}", id, rejectorUserId);

        return ReactGenerateApprovalResponse.builder()
                .codeId(id)
                .status(ReactGenerateStatus.REJECTED.name())
                .approvalUserId(rejectorUserId)
                .approvalDtime(now)
                .build();
    }

    /**
     * 승인 이력(APPROVED / REJECTED) 목록과 전체 건수를 조회한다.
     *
     * @param page           페이지 번호 (1-based)
     * @param size           페이지당 건수
     * @param status         상태 필터 (null/빈 문자열이면 전체)
     * @param title          화면 제목 부분 일치 검색 (null/빈 문자열이면 미적용)
     * @param componentName  컴포넌트명 부분 일치 검색 (null/빈 문자열이면 미적용)
     * @param approvalUserId 처리자 ID 부분 일치 검색
     * @param createUserId   요청자 ID 부분 일치 검색
     * @param fromDate       처리일시 시작 (yyyyMMdd)
     * @param toDate         처리일시 종료 (yyyyMMdd)
     * @return list(목록), totalCount(전체 건수), page, size
     */
    public Map<String, Object> getHistory(
            int page,
            int size,
            String status,
            String title,
            String componentName,
            String approvalUserId,
            String createUserId,
            String fromDate,
            String toDate) {
        // 날짜 형식 검증 — SQL DTIME 비교 파라미터이므로 형식 불일치 시 DB 오류 가능
        validateDateFormat(fromDate, "처리일시 시작");
        validateDateFormat(toDate, "처리일시 종료");

        int offset = (page - 1) * size;
        int endRow = offset + size;
        // 빈 문자열은 null로 통일하여 mapper의 전체 조회 분기를 타도록 한다
        // status: APPROVED/REJECTED 외의 값이 들어오면 거부 (이력 API 범위 제한)
        String s  = validateAndNormalizeStatus(nullIfBlank(status));
        // LIKE 검색 파라미터: %, _, \ 를 이스케이프하여 의도치 않은 와일드카드 동작 방지
        String ti = SqlUtils.escapeLike(nullIfBlank(title));
        String cn = SqlUtils.escapeLike(nullIfBlank(componentName));
        String au = SqlUtils.escapeLike(nullIfBlank(approvalUserId));
        String cu = SqlUtils.escapeLike(nullIfBlank(createUserId));
        String fd = nullIfBlank(fromDate);
        String td = nullIfBlank(toDate);

        // 날짜 범위 역전 검증 — fromDate가 toDate보다 이후이면 빈 결과 대신 오류 반환
        if (fd != null && td != null && fd.compareTo(td) > 0) {
            throw new InvalidInputException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }
        List<ReactGenerateHistoryResponse> list =
                reactGenerateMapper.selectApprovalHistory(offset, endRow, s, ti, cn, au, cu, fd, td);
        int totalCount = reactGenerateMapper.selectApprovalHistoryCount(s, ti, cn, au, cu, fd, td);
        return Map.of("list", list, "totalCount", totalCount, "page", page, "size", size);
    }

    /** null이거나 공백만 있으면 null, 아니면 원본 문자열 반환. */
    private static String nullIfBlank(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }

    /**
     * 날짜 문자열이 yyyyMMdd 형식인지 검증한다.
     * 빈 문자열과 null은 허용하며, 형식 불일치 시 {@link InvalidInputException}을 던진다.
     *
     * @param date      검증할 날짜 문자열
     * @param fieldName 오류 메시지에 표시할 필드명
     */
    private static void validateDateFormat(String date, String fieldName) {
        if (date != null && !date.isBlank() && !YYYYMMDD.matcher(date).matches()) {
            throw new InvalidInputException(fieldName + " 날짜 형식이 올바르지 않습니다. (yyyyMMdd 형식으로 입력하세요)");
        }
    }

    /**
     * 승인 이력 status 파라미터를 검증하고 정규화한다.
     * null이면 null 반환 (전체 조회), APPROVED/REJECTED 외의 값이면 예외를 던진다.
     *
     * @param status 검증할 상태 값 (nullIfBlank 처리 후 전달)
     * @return 유효한 상태 값 또는 null
     */
    private static String validateAndNormalizeStatus(String status) {
        if (status == null) return null;
        if (!ALLOWED_HISTORY_STATUSES.contains(status)) {
            throw new InvalidInputException("상태 값이 올바르지 않습니다. APPROVED 또는 REJECTED만 허용됩니다.");
        }
        return status;
    }

    /** CODE_ID로 이력을 조회하고, PENDING_APPROVAL 상태가 아니면 예외를 던진다. */
    private ReactGenerateResponse requirePendingApproval(String id) {
        ReactGenerateResponse response = reactGenerateMapper.selectById(id);
        if (response == null) {
            throw new NotFoundException("생성 결과를 찾을 수 없습니다. codeId=" + id);
        }
        if (!ReactGenerateStatus.PENDING_APPROVAL.name().equals(response.getStatus())) {
            throw new InvalidInputException("승인 대기 상태인 코드만 승인할 수 있습니다. 현재 상태: " + response.getStatus());
        }
        return response;
    }

    /** CODE_ID로 이력을 조회하고, 없으면 NotFoundException을 던진다. */
    private void requireExists(String id) {
        if (reactGenerateMapper.selectById(id) == null) {
            throw new NotFoundException("생성 결과를 찾을 수 없습니다. codeId=" + id);
        }
    }

}
