/**
 * @file GitPrApiClient.java
 * @description GitHub REST API v3를 호출하여 브랜치 생성, 파일 커밋, PR 생성을 수행하는 클라이언트.
 *
 * <p>인증은 {@code Authorization: Bearer {token}} 헤더로 처리한다.
 *
 * <p>호출 순서:
 * <ol>
 *   <li>{@link #getBaseSha} — base 브랜치의 최신 커밋 SHA 조회</li>
 *   <li>{@link #createBranch} — 신규 feature 브랜치 생성</li>
 *   <li>{@link #createOrUpdateFile} — 파일 커밋 (여러 번 호출 가능)</li>
 *   <li>{@link #createPullRequest} — PR 생성</li>
 * </ol>
 */
package com.example.reactplatform.domain.reactgenerate.deploy.gitpr;

import com.example.reactplatform.global.exception.InternalException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class GitPrApiClient {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate;
    private final String token;
    private final String owner;
    private final String repo;

    public GitPrApiClient(RestTemplate restTemplate, String token, String owner, String repo) {
        this.restTemplate = restTemplate;
        this.token = token;
        this.owner = owner;
        this.repo = repo;
    }

    /**
     * base 브랜치의 최신 커밋 SHA를 조회한다.
     *
     * @param baseBranch 조회할 브랜치명 (예: {@code main})
     * @return 최신 커밋 SHA 문자열
     * @throws InternalException 조회 실패 시
     */
    @SuppressWarnings("unchecked")
    public String getBaseSha(String baseBranch) {
        String url = GITHUB_API_BASE + "/repos/{owner}/{repo}/git/ref/heads/{branch}";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class, owner, repo, baseBranch);
            Map<String, Object> body = response.getBody();
            // 응답 구조: { "object": { "sha": "..." } }
            Map<String, Object> object = (Map<String, Object>) body.get("object");
            return (String) object.get("sha");
        } catch (HttpClientErrorException e) {
            // 응답 바디는 debug 레벨로만 기록 — Authorization 헤더나 토큰이 포함된 cause를 상위로 전파하지 않는다
            log.debug("[git-pr] getBaseSha 오류 응답 바디 — {}", e.getResponseBodyAsString());
            throw new InternalException(
                    "GitHub base SHA 조회 실패 — branch=" + baseBranch + ", status=" + e.getStatusCode());
        }
    }

    /**
     * 지정한 SHA를 기점으로 새 브랜치를 생성한다.
     * 브랜치가 이미 존재하면(422) base SHA로 강제 리셋하여 이전 배포 내용을 덮어쓴다.
     *
     * @param branchName 생성할 브랜치명 (예: {@code reactplatform/{codeId}})
     * @param sha        브랜치의 시작점이 될 커밋 SHA
     * @throws InternalException 브랜치 생성 및 리셋 모두 실패 시
     */
    public void createBranch(String branchName, String sha) {
        String url = GITHUB_API_BASE + "/repos/{owner}/{repo}/git/refs";
        Map<String, String> body = Map.of("ref", "refs/heads/" + branchName, "sha", sha);
        try {
            restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, jsonAuthHeaders()), Map.class, owner, repo);
            log.info("[git-pr] 브랜치 생성 완료 — branch={}", branchName);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            // 브랜치가 이미 존재하는 경우 — base SHA로 강제 리셋하여 덮어쓴다
            log.info("[git-pr] 브랜치 이미 존재함, base SHA로 리셋 — branch={}", branchName);
            resetBranch(branchName, sha);
        } catch (HttpClientErrorException e) {
            log.debug("[git-pr] createBranch 오류 응답 바디 — {}", e.getResponseBodyAsString());
            throw new InternalException("GitHub 브랜치 생성 실패 — branch=" + branchName + ", status=" + e.getStatusCode());
        }
    }

    /**
     * 기존 브랜치를 지정한 SHA로 강제 리셋한다 ({@code force: true}).
     *
     * @param branchName 리셋할 브랜치명
     * @param sha        리셋할 목표 커밋 SHA
     * @throws InternalException 리셋 실패 시
     */
    public void resetBranch(String branchName, String sha) {
        // 브랜치명의 '/'가 URI 템플릿 치환 시 '%2F'로 인코딩되어 GitHub API가 404를 반환하는 것을 방지
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/git/refs/heads/" + branchName;
        Map<String, Object> body = Map.of("sha", sha, "force", true);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, jsonAuthHeaders()), Map.class);
            log.info("[git-pr] 브랜치 리셋 완료 — branch={}", branchName);
        } catch (HttpClientErrorException e) {
            log.debug("[git-pr] resetBranch 오류 응답 바디 — {}", e.getResponseBodyAsString());
            throw new InternalException("GitHub 브랜치 리셋 실패 — branch=" + branchName + ", status=" + e.getStatusCode());
        }
    }

    /**
     * 해당 브랜치에 이미 열린 PR이 있으면 그 URL을 반환하고, 없으면 {@code null}을 반환한다.
     *
     * @param head 소스 브랜치명 (예: {@code reactplatform/{codeId}})
     * @return 열린 PR의 html_url, 없으면 null
     */
    public String findOpenPrUrl(String head) {
        // head 파라미터는 "{owner}:{branch}" 형식이어야 한다
        String url = GITHUB_API_BASE + "/repos/{owner}/{repo}/pulls?state=open&head={owner}:{head}";
        try {
            // ParameterizedTypeReference로 제네릭 타입 정보를 유지하여 타입 안전성 확보
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    owner,
                    repo,
                    owner,
                    head);
            List<Map<String, Object>> list = response.getBody();
            if (list != null && !list.isEmpty()) {
                String prUrl = (String) list.get(0).get("html_url");
                log.info("[git-pr] 기존 열린 PR 발견 — head={}, url={}", head, prUrl);
                return prUrl;
            }
            return null;
        } catch (HttpClientErrorException e) {
            // PR 조회 실패는 치명적이지 않으므로 null 반환 후 신규 PR 생성으로 진행
            log.warn("[git-pr] 기존 PR 조회 실패 — head={}, status={}", head, e.getStatusCode());
            return null;
        }
    }

    /**
     * 지정한 브랜치에 파일을 생성(또는 업데이트)한다.
     *
     * @param branch      커밋 대상 브랜치명
     * @param path        레포 내 파일 경로 (예: {@code src/generated/abc123.tsx})
     * @param content     파일 내용 (UTF-8 문자열, 내부에서 Base64 인코딩)
     * @param commitMessage 커밋 메시지
     * @throws InternalException 파일 커밋 실패 시
     */
    public void createOrUpdateFile(String branch, String path, String content, String commitMessage) {
        String url = GITHUB_API_BASE + "/repos/{owner}/{repo}/contents/{path}";
        // GitHub Contents API는 파일 내용을 Base64로 인코딩하여 전달해야 한다
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, String> body = Map.of(
                "message", commitMessage,
                "content", encoded,
                "branch", branch);
        try {
            restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(body, jsonAuthHeaders()), Map.class, owner, repo, path);
            log.info("[git-pr] 파일 커밋 완료 — branch={}, path={}", branch, path);
        } catch (HttpClientErrorException e) {
            log.debug("[git-pr] createOrUpdateFile 오류 응답 바디 — {}", e.getResponseBodyAsString());
            throw new InternalException("GitHub 파일 커밋 실패 — path=" + path + ", status=" + e.getStatusCode());
        }
    }

    /**
     * base 브랜치로 향하는 PR을 생성하고 PR URL을 반환한다.
     *
     * @param head       PR의 소스 브랜치명
     * @param base       PR의 merge 대상 브랜치명
     * @param title      PR 제목
     * @param body       PR 본문 (Markdown)
     * @return 생성된 PR의 HTML URL
     * @throws InternalException PR 생성 실패 시
     */
    @SuppressWarnings("unchecked")
    public String createPullRequest(String head, String base, String title, String body) {
        String url = GITHUB_API_BASE + "/repos/{owner}/{repo}/pulls";
        Map<String, String> requestBody = Map.of(
                "title", title,
                "head", head,
                "base", base,
                "body", body);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(requestBody, jsonAuthHeaders()), Map.class, owner, repo);
            String htmlUrl = (String) response.getBody().get("html_url");
            log.info("[git-pr] PR 생성 완료 — url={}", htmlUrl);
            return htmlUrl;
        } catch (HttpClientErrorException e) {
            log.debug("[git-pr] createPullRequest 오류 응답 바디 — {}", e.getResponseBodyAsString());
            throw new InternalException("GitHub PR 생성 실패 — head=" + head + ", status=" + e.getStatusCode());
        }
    }

    /** GET 요청용 인증 헤더 (Content-Type 없음) */
    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    /** POST/PUT 요청용 인증 헤더 (Content-Type: application/json 포함) */
    private HttpHeaders jsonAuthHeaders() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
