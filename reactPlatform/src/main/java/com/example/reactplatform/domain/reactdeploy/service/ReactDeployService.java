/**
 * @file ReactDeployService.java
 * @description React 코드 배포 관리 서비스.
 *     배포 실행 및 이력 기록, 배포 가능 목록/이력 조회를 담당한다.
 *
 * <p>배포 흐름:
 * <ol>
 *   <li>승인 시 {@link #deployAndRecord}가 {@code afterCommit()}에서 호출된다.</li>
 *   <li>재배포 시 {@link #redeploy}가 컨트롤러에서 호출된다.</li>
 *   <li>두 경로 모두 배포 결과를 {@code FWK_REACT_DEPLOY_HIS}에 INSERT한다.</li>
 * </ol>
 */
package com.example.reactplatform.domain.reactdeploy.service;

import com.example.reactplatform.domain.reactdeploy.dto.ReactDeployHistoryResponse;
import com.example.reactplatform.domain.reactdeploy.dto.ReactDeployListResponse;
import com.example.reactplatform.domain.reactdeploy.dto.ReactRedeployResponse;
import com.example.reactplatform.domain.reactdeploy.mapper.ReactDeployMapper;
import com.example.reactplatform.domain.reactgenerate.deploy.DeployResult;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployProperties;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployStrategy;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.mapper.ReactGenerateMapper;
import com.example.reactplatform.global.exception.InvalidInputException;
import com.example.reactplatform.global.exception.NotFoundException;
import com.example.reactplatform.global.util.SqlUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactDeployService {

    private final ReactDeployMapper reactDeployMapper;
    private final ReactGenerateMapper reactGenerateMapper;
    private final ReactDeployStrategy deployStrategy;
    private final ReactDeployProperties deployProperties;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 진행 중인 재배포 codeId 집합 — 동일 codeId의 동시 요청을 방지한다.
     * 단일 인스턴스 내에서만 유효하며, 다중 인스턴스 수평 확장 시 Redis 분산 락으로 대체해야 한다.
     * TODO: 다중 인스턴스 환경 전환 시 Redisson 또는 Spring Integration Lock 적용 검토
     */
    private final Set<String> inProgressCodeIds = ConcurrentHashMap.newKeySet();

    /**
     * 배포를 실행하고 결과를 {@code FWK_REACT_DEPLOY_HIS}에 기록한다.
     *
     * <p>승인 {@code afterCommit()}에서 호출된다. 배포 실패는 비치명적이므로
     * 예외를 상위로 전파하지 않고 이력에만 기록한다.
     *
     * @param codeId    승인된 코드 ID
     * @param reactCode 배포할 React TSX 코드
     * @param userId    배포 실행자 ID (승인자)
     */
    public void deployAndRecord(String codeId, String reactCode, String userId) {
        DeployResult result = deployStrategy.deploy(codeId, reactCode);
        recordHistory(codeId, result, userId);
        log.info("[deploy] 배포 이력 기록 완료 — codeId={}, status={}", codeId, result.getStatus());
    }

    /**
     * 재배포를 실행한다.
     *
     * <p>APPROVED 상태 코드만 재배포 가능하다.
     *
     * @param codeId 재배포할 코드 ID
     * @param userId 재배포 실행자 ID
     * @return 재배포 결과
     * @throws NotFoundException     코드가 존재하지 않을 때
     * @throws InvalidInputException APPROVED 상태가 아닐 때
     */
    public ReactRedeployResponse redeploy(String codeId, String userId) {
        ReactGenerateResponse code = reactGenerateMapper.selectById(codeId);
        if (code == null) {
            throw new NotFoundException("배포 대상 코드를 찾을 수 없습니다. codeId=" + codeId);
        }
        if (!"APPROVED".equals(code.getStatus())) {
            throw new InvalidInputException("APPROVED 상태인 코드만 재배포할 수 있습니다. 현재 상태: " + code.getStatus());
        }

        // 동일 codeId의 중복 요청 차단 — ConcurrentHashMap.newKeySet()으로 원자적으로 점유
        if (!inProgressCodeIds.add(codeId)) {
            throw new InvalidInputException("이미 배포가 진행 중입니다. codeId=" + codeId);
        }
        try {
            DeployResult result = deployStrategy.deploy(codeId, code.getReactCode());
            recordHistory(codeId, result, userId);

            String message = result.isSuccess() ? "배포가 완료되었습니다." : "배포 중 오류가 발생했습니다. (" + result.getFailReason() + ")";
            return ReactRedeployResponse.builder()
                    .success(result.isSuccess())
                    .message(message)
                    .prUrl(result.getPrUrl())
                    .build();
        } finally {
            inProgressCodeIds.remove(codeId);
        }
    }

    /**
     * 배포 가능 목록(APPROVED 코드 + 최근 배포 이력 LEFT JOIN)을 페이지네이션으로 반환한다.
     *
     * @param page   페이지 번호 (1-based)
     * @param size   페이지당 건수
     * @param search 코드 ID 또는 요청자 ID 검색 키워드
     * @return list, totalCount, page, size
     */
    public Map<String, Object> findDeployList(int page, int size, String search) {
        int offset = (page - 1) * size;
        int endRow = offset + size;
        String escaped = SqlUtils.escapeLike(nullIfBlank(search));
        List<ReactDeployListResponse> list = reactDeployMapper.selectDeployList(offset, endRow, escaped);
        int totalCount = reactDeployMapper.selectDeployListCount(escaped);
        return Map.of("list", list, "totalCount", totalCount, "page", page, "size", size);
    }

    /**
     * 전체 배포 이력을 페이지네이션으로 반환한다 (하단 이력 테이블용).
     *
     * @param page   페이지 번호 (1-based)
     * @param size   페이지당 건수
     * @param search 코드 ID 또는 실행자 ID 검색 키워드
     * @param userId 실행자 ID 일치 필터 — null이면 전체 조회
     * @return list, totalCount, page, size
     */
    public Map<String, Object> findAllHistoryList(int page, int size, String search, String userId) {
        int offset = (page - 1) * size;
        int endRow = offset + size;
        String escaped = SqlUtils.escapeLike(nullIfBlank(search));
        List<ReactDeployHistoryResponse> list = reactDeployMapper.selectAllHistoryList(offset, endRow, escaped, userId);
        int totalCount = reactDeployMapper.selectAllHistoryCount(escaped, userId);
        return Map.of("list", list, "totalCount", totalCount, "page", page, "size", size);
    }

    /**
     * 특정 코드의 배포 이력을 반환한다 (모달용).
     *
     * @param codeId 조회할 코드 ID
     * @param page   페이지 번호 (1-based)
     * @param size   페이지당 건수
     * @return list, totalCount, page, size
     */
    public Map<String, Object> findHistoryByCodeId(String codeId, int page, int size) {
        int offset = (page - 1) * size;
        int endRow = offset + size;
        List<ReactDeployHistoryResponse> list = reactDeployMapper.selectHistoryByCodeId(codeId, offset, endRow);
        int totalCount = reactDeployMapper.selectHistoryCountByCodeId(codeId);
        return Map.of("list", list, "totalCount", totalCount, "page", page, "size", size);
    }

    /**
     * FAIL_REASON DB 저장 최대 길이.
     * CLOB는 길이 제한이 없으나, 예외 메시지에 긴 응답 바디가 포함될 경우를 대비해 제한한다.
     */
    private static final int FAIL_REASON_MAX_LENGTH = 2000;

    /** 배포 결과를 FWK_REACT_DEPLOY_HIS에 INSERT한다. */
    private void recordHistory(String codeId, DeployResult result, String userId) {
        reactDeployMapper.insert(
                UUID.randomUUID().toString(),
                codeId,
                deployProperties.getMode(),
                result.getStatus(),
                truncate(result.getFailReason(), FAIL_REASON_MAX_LENGTH),
                result.getPrUrl(),
                LocalDateTime.now().format(FORMATTER),
                userId
        );
    }

    /** 문자열을 maxLength 이하로 자른다. null이면 null을 반환한다. */
    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private static String nullIfBlank(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }
}
