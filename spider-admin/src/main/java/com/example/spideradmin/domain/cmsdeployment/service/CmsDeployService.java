package com.example.spideradmin.domain.cmsdeployment.service;

import com.example.spideradmin.domain.cmsdeployment.config.CmsDeployProperties;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployHistoryRequest;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployHistoryResponse;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployPageRequest;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsDeployPageResponse;
import com.example.spideradmin.domain.cmsdeployment.dto.CmsServerInstanceResponse;
import com.example.spideradmin.domain.cmsdeployment.mapper.CmsDeployMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/** CMS 배포 관리 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsDeployService {

    private final CmsDeployMapper cmsDeployMapper;
    private final RestTemplate restTemplate;
    private final CmsDeployProperties deployProperties;

    /**
     * 자기 자신의 프록시 참조 — self-invocation 문제 해결용.
     * {@code this.saveExpiredResult()} 직접 호출 시 AOP 프록시가 우회돼 {@code @Transactional}이
     * 적용되지 않으므로, {@code @Lazy}로 주입받은 프록시를 통해 호출한다.
     */
    @Lazy
    @Autowired
    private CmsDeployService self;

    /** 배포 대상 페이지 목록 조회 (APPROVE_STATE = 'APPROVED') */
    public PageResponse<CmsDeployPageResponse> findApprovedPageList(CmsDeployPageRequest req, PageRequest pageRequest) {
        long total = cmsDeployMapper.countApprovedPageList(req);
        List<CmsDeployPageResponse> list =
                cmsDeployMapper.findApprovedPageList(req, pageRequest.getOffset(), pageRequest.getEndRow());
        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 배포 이력 목록 조회 (모달용, pageId 필터) */
    public PageResponse<CmsDeployHistoryResponse> findHistoryList(
            CmsDeployHistoryRequest req, PageRequest pageRequest) {
        long total = cmsDeployMapper.countHistoryList(req);
        List<CmsDeployHistoryResponse> list =
                cmsDeployMapper.findHistoryList(req, pageRequest.getOffset(), pageRequest.getEndRow());
        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 배포 실행
     *
     * <p>HTML 조립(ContentBuilder CSS·런타임 인라인, 에셋 경로 치환)·파일 저장·이력 기록은 CMS push API가 담당한다.
     * Admin은 APPROVED 상태 사전 검증 후 CMS push API를 호출한다.
     */
    public void push(String pageId, String userId) {
        // APPROVED 상태 사전 검증 — CMS가 실패하기 전에 빠른 피드백 제공
        String html = cmsDeployMapper.findApprovedPageHtml(pageId);
        if (html == null) {
            throw new NotFoundException("승인된 페이지를 찾을 수 없습니다. pageId=" + pageId);
        }
        // HTML 조립·파일 저장·이력 기록은 CMS push API가 담당
        callCmsPush(pageId, userId);
        log.info("CMS 배포 요청 완료: pageId={}, userId={}", pageId, userId);
    }

    /**
     * 만료수동처리 배포.
     *
     * <p>CMS 서버에서 page-expired.html을 읽어와 ALIVE_YN='Y' 인 모든 서버에 순차 전송한다.
     * 전송 성공 후 FWK_CMS_FILE_SEND_HIS에 이력을 기록하고 IS_PUBLIC='N' 으로 상태를 변경한다.</p>
     *
     * <p>만료 FILE_ID 패턴: {pageId}_v{version}_expired.html
     * — 정상 배포 이력({pageId}_v{n}.html)을 덮어쓰지 않도록 별도 키 사용</p>
     *
     * <p>클래스 레벨 {@code readOnly=true} 트랜잭션을 중단(NOT_SUPPORTED)하고 비트랜잭션 상태로
     * HTTP 통신을 수행한다. DB 쓰기는 {@code self.saveExpiredResult()}를 프록시로 호출해
     * 별도 쓰기 트랜잭션을 시작한다 (self-invocation 우회).</p>
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pushExpired(String pageId, String userId) {
        // 1. 만료 처리 대상 검증 (EXPIRED_DATE < SYSDATE AND IS_PUBLIC='Y' AND USE_YN='Y')
        String result = cmsDeployMapper.findExpirableFilePath(pageId);
        if (result == null) {
            throw new NotFoundException("만료 처리 대상이 아닙니다. pageId=" + pageId);
        }

        // 2. 최신 배포 FILE_ID에서 버전 추출 → 만료 전용 FILE_ID 조합
        //    예: PAGE001_v3.html → PAGE001_v3_expired.html
        //    이력 없으면 v0 폴백 사용
        String latestFileId = cmsDeployMapper.findLatestDeployedFileId(pageId);
        String expiredFileId;
        if (latestFileId != null && latestFileId.endsWith(".html")) {
            expiredFileId = latestFileId.replace(".html", "_expired.html");
        } else {
            expiredFileId = pageId + "_v0_expired.html";
        }

        // 3. CMS 서버에서 page-expired.html 취득 (트랜잭션 외부 — HTTP 통신)
        String expiredHtml = fetchExpiredHtml();
        // UTF-8 바이트 길이로 실제 파일 크기 계산 (멀티바이트 문자 포함 HTML 대비)
        long htmlByteSize = expiredHtml.getBytes(StandardCharsets.UTF_8).length;

        // 4. ALIVE_YN='Y' 서버 목록 조회
        List<CmsServerInstanceResponse> servers = cmsDeployMapper.findAliveServerInstances();
        if (servers.isEmpty()) {
            throw new InternalException("배포 가능한 서버 인스턴스가 없습니다.");
        }

        // 5. 각 서버에 순차 전송 (트랜잭션 외부 — HTTP 통신)
        for (CmsServerInstanceResponse server : servers) {
            callReceiveApi(server, pageId, expiredHtml);
        }

        // 6. 전송 완료 후 DB 반영 — self 프록시를 통해 쓰기 트랜잭션 시작 (self-invocation 우회)
        self.saveExpiredResult(pageId, expiredFileId, htmlByteSize, userId);
        log.info("만료 배포 완료: pageId={}, expiredFileId={}, userId={}", pageId, expiredFileId, userId);
    }

    /**
     * 만료 배포 완료 후 DB 반영 — 이력 UPSERT + 페이지 상태 업데이트.
     * HTTP 통신과 분리하여 DB 커넥션 점유를 최소화한다.
     */
    @Transactional
    public void saveExpiredResult(String pageId, String expiredFileId, long htmlByteSize, String userId) {
        List<CmsServerInstanceResponse> servers = cmsDeployMapper.findAliveServerInstances();
        for (CmsServerInstanceResponse server : servers) {
            cmsDeployMapper.upsertFileSendHis(
                    server.getInstanceId(),
                    expiredFileId,
                    htmlByteSize,
                    ".", // 만료 HTML은 CRC 계산 불필요 — NOT NULL 제약 대응 플레이스홀더
                    userId);
        }
        // IS_PUBLIC='N', FILE_PATH_BACK=FILE_PATH
        cmsDeployMapper.expirePage(pageId, userId);
    }

    /** CMS 서버에서 page-expired.html을 HTTP GET으로 취득 */
    private String fetchExpiredHtml() {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(deployProperties.getExpiredHtmlUrl(), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new InternalException("page-expired.html 응답이 비어있습니다.");
            }
            return body;
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("page-expired.html 취득 실패: " + e.getMessage(), e);
        }
    }

    /** /cms/api/deploy/receive 호출 — 만료 HTML을 서버 인스턴스에 전송 */
    private void callReceiveApi(CmsServerInstanceResponse server, String pageId, String html) {
        String url = "http://" + server.getInstanceIp() + ":" + server.getInstancePort() + "/cms/api/deploy/receive";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-deploy-token", deployProperties.getSecret());

        Map<String, Object> body = new HashMap<>();
        body.put("pageId", pageId);
        body.put("html", html);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new InternalException("만료 배포 서버(" + server.getInstanceId() + ")에서 빈 응답을 반환했습니다.");
            }
            if (!Boolean.TRUE.equals(responseBody.get("ok"))) {
                Object error = responseBody.get("error");
                throw new InternalException("만료 배포 서버(" + server.getInstanceId() + ") 오류: " + error);
            }
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("만료 배포 서버(" + server.getInstanceId() + ") 전송 오류: " + e.getMessage(), e);
        }
    }

    /** CMS push API 호출 — x-deploy-token 서버 간 인증 사용 */
    @SuppressWarnings("unchecked")
    private void callCmsPush(String pageId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-deploy-token", deployProperties.getSecret());

        Map<String, Object> body = new HashMap<>();
        body.put("pageId", pageId);
        body.put("userId", userId);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    deployProperties.getPushUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new InternalException("CMS 배포 서버에서 빈 응답을 반환했습니다.");
            }
            if (!Boolean.TRUE.equals(responseBody.get("ok"))) {
                Object error = responseBody.get("error");
                throw new InternalException("CMS 배포 서버 오류: " + error);
            }
        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException("CMS 배포 서버 오류: " + e.getMessage(), e);
        }
    }
}
