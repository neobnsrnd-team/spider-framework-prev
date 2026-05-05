/**
 * @file GitPrDeployStrategyTest.java
 * @description GitPrDeployStrategy 단위 테스트.
 *     GitPrApiClient를 모킹하여 브랜치 생성 → 파일 커밋 → PR 생성 호출 순서,
 *     브랜치명 규칙, DeployResult 반환값, 비치명적 오류 처리를 검증한다.
 * @see GitPrDeployStrategy
 */
package com.example.reactplatform.domain.reactgenerate.deploy.gitpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.reactplatform.domain.reactgenerate.deploy.ContainerScaffoldGenerator;
import com.example.reactplatform.domain.reactgenerate.deploy.DeployResult;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployProperties;
import com.example.reactplatform.global.exception.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitPrDeployStrategyTest {

    @Mock
    private GitPrApiClient gitPrApiClient;

    @Mock
    private ContainerScaffoldGenerator scaffoldGenerator;

    private GitPrDeployStrategy strategy;

    private static final String CODE_ID = "code-abc";
    private static final String REACT_CODE = "export default function LoginPage() { return <div/>; }";
    private static final String BASE_SHA = "abc123sha456";
    private static final String SCAFFOLD_CODE = "// scaffold";
    private static final String SCAFFOLD_FILE = "LoginPageContainer.tsx";
    private static final String PR_URL = "https://github.com/owner/repo/pull/1";

    // deploy()에 직접 전달하는 컴포넌트명 (DB 저장값 — 코드 파싱 없이 파일명 결정)
    private static final String COMPONENT_NAME = "LoginPage";

    @BeforeEach
    void setUp() {
        ReactDeployProperties props = new ReactDeployProperties();
        ReactDeployProperties.GitPr gitPr = new ReactDeployProperties.GitPr();
        gitPr.setToken("ghp_test");
        gitPr.setOwner("test-owner");
        gitPr.setRepo("test-repo");
        gitPr.setBaseBranch("main");
        gitPr.setComponentPath("src/generated");
        gitPr.setContainerPath("src/containers");
        props.setGitPr(gitPr);
        strategy = new GitPrDeployStrategy(gitPrApiClient, props, scaffoldGenerator);
    }

    // ========== 성공 경로 ==========

    @Test
    @DisplayName(
            "정상 배포 시 getBaseSha → createBranch → createOrUpdateFile × 2 → findOpenPrUrl → createPullRequest 순서로 호출된다")
    void deploy_success_callsApiInCorrectOrder() {
        stubSuccessfulDeploy();

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        String branch = "reactplatform/" + CODE_ID;
        InOrder order = inOrder(gitPrApiClient);
        order.verify(gitPrApiClient).getBaseSha("main");
        order.verify(gitPrApiClient).createBranch(eq(branch), eq(BASE_SHA));
        order.verify(gitPrApiClient)
                .createOrUpdateFile(eq(branch), eq("src/generated/LoginPage.tsx"), anyString(), anyString());
        order.verify(gitPrApiClient)
                .createOrUpdateFile(eq(branch), eq("src/containers/" + SCAFFOLD_FILE), anyString(), anyString());
        order.verify(gitPrApiClient).findOpenPrUrl(eq(branch));
        order.verify(gitPrApiClient).createPullRequest(eq(branch), eq("main"), anyString(), anyString());
    }

    @Test
    @DisplayName("정상 배포 시 SUCCESS 결과와 PR URL을 반환한다")
    void deploy_success_returnsSuccessResultWithPrUrl() {
        stubSuccessfulDeploy();

        DeployResult result = strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrUrl()).isEqualTo(PR_URL);
        assertThat(result.getFailReason()).isNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("브랜치명은 reactplatform/{codeId} 형식이다")
    void deploy_branchName_followsNamingConvention() {
        stubSuccessfulDeploy();

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        verify(gitPrApiClient).createBranch(eq("reactplatform/" + CODE_ID), eq(BASE_SHA));
    }

    @Test
    @DisplayName("UI 컴포넌트 파일은 {component-path}/{ComponentName}.tsx 경로로 커밋된다")
    void deploy_componentFilePath_usesComponentName() {
        stubSuccessfulDeploy();

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        verify(gitPrApiClient)
                .createOrUpdateFile(
                        eq("reactplatform/" + CODE_ID), eq("src/generated/LoginPage.tsx"), eq(REACT_CODE), anyString());
    }

    @Test
    @DisplayName("Container scaffold 파일은 {container-path}/{FileName}Container.tsx 경로로 커밋된다")
    void deploy_scaffoldFilePath_usesContainerPath() {
        stubSuccessfulDeploy();

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        verify(gitPrApiClient)
                .createOrUpdateFile(
                        eq("reactplatform/" + CODE_ID),
                        eq("src/containers/" + SCAFFOLD_FILE),
                        eq(SCAFFOLD_CODE),
                        anyString());
    }

    @Test
    @DisplayName("PR 본문에 개발자 TODO 안내 항목과 codeId가 포함된다")
    void deploy_prBody_containsTodoGuideAndCodeId() {
        when(gitPrApiClient.getBaseSha("main")).thenReturn(BASE_SHA);
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        when(gitPrApiClient.createPullRequest(anyString(), anyString(), anyString(), bodyCaptor.capture()))
                .thenReturn(PR_URL);

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        String body = bodyCaptor.getValue();
        assertThat(body).contains("TODO");
        assertThat(body).contains(CODE_ID);
    }

    @Test
    @DisplayName("PR은 base 브랜치(main)를 대상으로 생성된다")
    void deploy_pr_targetsBaseBranch() {
        stubSuccessfulDeploy();

        strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        verify(gitPrApiClient).createPullRequest(eq("reactplatform/" + CODE_ID), eq("main"), anyString(), anyString());
    }

    @Test
    @DisplayName("해당 브랜치에 열린 PR이 이미 있으면 createPullRequest를 호출하지 않고 기존 URL을 반환한다")
    void deploy_existingOpenPr_reusesExistingPrUrl() {
        String existingPrUrl = "https://github.com/owner/repo/pull/99";
        when(gitPrApiClient.getBaseSha("main")).thenReturn(BASE_SHA);
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);
        when(gitPrApiClient.findOpenPrUrl(anyString())).thenReturn(existingPrUrl);

        DeployResult result = strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrUrl()).isEqualTo(existingPrUrl);
        verify(gitPrApiClient, never()).createPullRequest(anyString(), anyString(), anyString(), anyString());
    }

    // ========== 빈 코드 처리 ==========

    @Test
    @DisplayName("React 코드가 빈 문자열이면 GitHub API를 호출하지 않고 FAILED 결과를 반환한다")
    void deploy_emptyCode_returnsFailureAndNoApiCall() {
        DeployResult result = strategy.deploy(CODE_ID, "", COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
        verifyNoInteractions(gitPrApiClient);
    }

    @Test
    @DisplayName("React 코드가 null이면 GitHub API를 호출하지 않고 FAILED 결과를 반환한다")
    void deploy_nullCode_returnsFailureAndNoApiCall() {
        DeployResult result = strategy.deploy(CODE_ID, null, COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
        verifyNoInteractions(gitPrApiClient);
    }

    // ========== 비치명적 오류 처리 ==========

    @Test
    @DisplayName("getBaseSha 실패 시 예외를 던지지 않고 FAILED 결과를 반환한다")
    void deploy_getBaseShaFails_returnsFailureNonFatal() {
        when(gitPrApiClient.getBaseSha(anyString())).thenThrow(new InternalException("API 오류"));

        DeployResult result = strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        assertThatNoException().isThrownBy(() -> strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    @Test
    @DisplayName("createBranch 실패 시 예외를 던지지 않고 FAILED 결과를 반환한다")
    void deploy_createBranchFails_returnsFailureNonFatal() {
        when(gitPrApiClient.getBaseSha(anyString())).thenReturn(BASE_SHA);
        doThrow(new InternalException("브랜치 생성 실패")).when(gitPrApiClient).createBranch(anyString(), anyString());

        DeployResult result = strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    @Test
    @DisplayName("PR 생성 실패 시 예외를 던지지 않고 FAILED 결과를 반환한다")
    void deploy_createPrFails_returnsFailureNonFatal() {
        when(gitPrApiClient.getBaseSha(anyString())).thenReturn(BASE_SHA);
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);
        when(gitPrApiClient.createPullRequest(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new InternalException("PR 생성 실패"));

        DeployResult result = strategy.deploy(CODE_ID, REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    // ========== helpers ==========

    /** 성공 경로에 필요한 모든 Mock 스텁을 설정한다. findOpenPrUrl은 null(기존 PR 없음)을 반환하도록 설정한다. */
    private void stubSuccessfulDeploy() {
        when(gitPrApiClient.getBaseSha("main")).thenReturn(BASE_SHA);
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);
        when(gitPrApiClient.findOpenPrUrl(anyString())).thenReturn(null);
        when(gitPrApiClient.createPullRequest(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(PR_URL);
    }
}
