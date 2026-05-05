/**
 * @file GitPrApiClientTest.java
 * @description GitPrApiClient 단위 테스트.
 *     MockRestServiceServer로 GitHub REST API 호출을 가로채어
 *     요청 형식(인증 헤더, JSON 본문, Base64 인코딩)과 응답 파싱을 검증한다.
 * @see GitPrApiClient
 */
package com.example.reactplatform.domain.reactgenerate.deploy.gitpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.reactplatform.global.exception.InternalException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GitPrApiClientTest {

    private MockRestServiceServer server;
    private GitPrApiClient client;

    private static final String GITHUB_API = "https://api.github.com";
    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";
    private static final String TOKEN = "ghp_testtoken";
    private static final String BASE_URL = GITHUB_API + "/repos/" + OWNER + "/" + REPO;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new GitPrApiClient(restTemplate, TOKEN, OWNER, REPO);
    }

    // ========== getBaseSha ==========

    @Test
    @DisplayName("getBaseSha: main 브랜치의 SHA를 정상적으로 파싱하여 반환한다")
    void getBaseSha_success_returnsParsedSha() {
        server.expect(requestTo(BASE_URL + "/git/ref/heads/main"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(
                        "{\"ref\":\"refs/heads/main\",\"object\":{\"sha\":\"abc123sha\"}}",
                        MediaType.APPLICATION_JSON));

        String sha = client.getBaseSha("main");

        assertThat(sha).isEqualTo("abc123sha");
        server.verify();
    }

    @Test
    @DisplayName("getBaseSha: 브랜치를 찾을 수 없으면 InternalException이 발생한다")
    void getBaseSha_notFound_throwsInternalException() {
        server.expect(requestTo(BASE_URL + "/git/ref/heads/missing")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getBaseSha("missing")).isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("getBaseSha: 인증 실패(401) 시 InternalException이 발생한다")
    void getBaseSha_unauthorized_throwsInternalException() {
        server.expect(requestTo(BASE_URL + "/git/ref/heads/main")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getBaseSha("main")).isInstanceOf(InternalException.class);
    }

    // ========== createBranch ==========

    @Test
    @DisplayName("createBranch: 올바른 ref와 sha가 JSON 본문에 포함되어 전송된다")
    void createBranch_success_sendsCorrectPayload() {
        server.expect(requestTo(BASE_URL + "/git/refs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.ref").value("refs/heads/reactplatform/code-01"))
                .andExpect(jsonPath("$.sha").value("abc123"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.createBranch("reactplatform/code-01", "abc123");
        server.verify();
    }

    @Test
    @DisplayName("createBranch: 이미 존재하는 브랜치(422)면 resetBranch를 호출하여 정상 완료된다")
    void createBranch_alreadyExists_resetsExistingBranch() {
        // 1st: POST → 422 (branch already exists)
        server.expect(requestTo(BASE_URL + "/git/refs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        // 2nd: PATCH → 200 (reset branch)
        server.expect(requestTo(BASE_URL + "/git/refs/heads/reactplatform/code-01"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.sha").value("abc123"))
                .andExpect(jsonPath("$.force").value(true))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatNoException().isThrownBy(() -> client.createBranch("reactplatform/code-01", "abc123"));
        server.verify();
    }

    // ========== resetBranch ==========

    @Test
    @DisplayName("resetBranch: 올바른 SHA와 force:true가 PATCH 본문에 포함되어 전송된다")
    void resetBranch_success_sendsCorrectPayload() {
        server.expect(requestTo(BASE_URL + "/git/refs/heads/reactplatform/code-01"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.sha").value("newsha"))
                .andExpect(jsonPath("$.force").value(true))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatNoException().isThrownBy(() -> client.resetBranch("reactplatform/code-01", "newsha"));
        server.verify();
    }

    @Test
    @DisplayName("resetBranch: 실패 시 InternalException이 발생한다")
    void resetBranch_failure_throwsInternalException() {
        server.expect(requestTo(BASE_URL + "/git/refs/heads/reactplatform/code-01"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.resetBranch("reactplatform/code-01", "newsha"))
                .isInstanceOf(InternalException.class);
    }

    // ========== createOrUpdateFile ==========

    @Test
    @DisplayName("createOrUpdateFile: 파일 내용이 Base64로 인코딩되어 전송된다")
    void createOrUpdateFile_success_encodesContentAsBase64() {
        String content = "export default function Foo() {}";
        String expectedEncoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

        // RestTemplate URI 템플릿 치환 시 경로의 '/'가 '%2F'로 인코딩된다
        server.expect(requestTo(BASE_URL + "/contents/src%2Fgenerated%2Ffoo.tsx"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.content").value(expectedEncoded))
                .andExpect(jsonPath("$.branch").value("reactplatform/01"))
                .andExpect(jsonPath("$.message").value("feat: add foo"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.createOrUpdateFile("reactplatform/01", "src/generated/foo.tsx", content, "feat: add foo");
        server.verify();
    }

    @Test
    @DisplayName("createOrUpdateFile: 파일 커밋 실패 시 InternalException이 발생한다")
    void createOrUpdateFile_failure_throwsInternalException() {
        server.expect(requestTo(BASE_URL + "/contents/src%2Fgenerated%2Ffoo.tsx"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() ->
                        client.createOrUpdateFile("reactplatform/01", "src/generated/foo.tsx", "content", "commit msg"))
                .isInstanceOf(InternalException.class);
    }

    // ========== createPullRequest ==========

    @Test
    @DisplayName("createPullRequest: PR 생성 성공 시 html_url을 반환한다")
    void createPullRequest_success_returnsPrUrl() {
        server.expect(requestTo(BASE_URL + "/pulls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.head").value("reactplatform/01"))
                .andExpect(jsonPath("$.base").value("main"))
                .andExpect(jsonPath("$.title").value("feat: PR title"))
                .andRespond(withSuccess(
                        "{\"html_url\":\"https://github.com/test-owner/test-repo/pull/1\"}",
                        MediaType.APPLICATION_JSON));

        String url = client.createPullRequest("reactplatform/01", "main", "feat: PR title", "PR body");

        assertThat(url).isEqualTo("https://github.com/test-owner/test-repo/pull/1");
        server.verify();
    }

    @Test
    @DisplayName("createPullRequest: PR 생성 실패(422) 시 InternalException이 발생한다")
    void createPullRequest_unprocessableEntity_throwsInternalException() {
        server.expect(requestTo(BASE_URL + "/pulls")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.createPullRequest("reactplatform/01", "main", "title", "body"))
                .isInstanceOf(InternalException.class);
    }

    // ========== findOpenPrUrl ==========

    @Test
    @DisplayName("findOpenPrUrl: 열린 PR이 있으면 html_url을 반환한다")
    void findOpenPrUrl_found_returnsUrl() {
        server.expect(requestTo(BASE_URL + "/pulls?state=open&head=test-owner:reactplatform/code-01"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(
                        "[{\"html_url\":\"https://github.com/test-owner/test-repo/pull/5\"}]",
                        MediaType.APPLICATION_JSON));

        String url = client.findOpenPrUrl("reactplatform/code-01");

        assertThat(url).isEqualTo("https://github.com/test-owner/test-repo/pull/5");
        server.verify();
    }

    @Test
    @DisplayName("findOpenPrUrl: 열린 PR이 없으면 null을 반환한다")
    void findOpenPrUrl_notFound_returnsNull() {
        server.expect(requestTo(BASE_URL + "/pulls?state=open&head=test-owner:reactplatform/code-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String url = client.findOpenPrUrl("reactplatform/code-01");

        assertThat(url).isNull();
        server.verify();
    }

    // ========== X-GitHub-Api-Version 헤더 ==========

    @Test
    @DisplayName("모든 요청에 GitHub API 버전 헤더가 포함된다")
    void allRequests_containGitHubApiVersionHeader() {
        server.expect(requestTo(BASE_URL + "/git/ref/heads/main"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess("{\"object\":{\"sha\":\"sha123\"}}", MediaType.APPLICATION_JSON));

        client.getBaseSha("main");
        server.verify();
    }
}
