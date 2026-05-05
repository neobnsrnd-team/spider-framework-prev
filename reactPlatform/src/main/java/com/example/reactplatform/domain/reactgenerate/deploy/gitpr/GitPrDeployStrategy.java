/**
 * @file GitPrDeployStrategy.java
 * @description {@code react.deploy.mode: git-pr} 일 때 사용하는 배포 전략.
 *     승인된 React UI 컴포넌트와 Container scaffold를 GitHub 레포에 PR로 자동 생성한다.
 *
 * <p>PR 생성 흐름:
 * <ol>
 *   <li>base 브랜치의 최신 커밋 SHA 조회</li>
 *   <li>{@code reactplatform/{codeId}} 브랜치 생성</li>
 *   <li>UI 컴포넌트 파일({component-path}/{ComponentName}.tsx) 커밋</li>
 *   <li>Container scaffold 파일({container-path}/{ComponentName}Container.tsx) 커밋</li>
 *   <li>PR 생성</li>
 * </ol>
 *
 * <p>GitHub API 호출 실패는 예외를 던지지 않고 {@link DeployResult#failure}로 반환한다.
 * DB 승인은 이미 커밋된 상태이므로 배포 실패가 트랜잭션에 영향을 주지 않는다.
 */
package com.example.reactplatform.domain.reactgenerate.deploy.gitpr;

import com.example.reactplatform.domain.reactgenerate.deploy.ContainerScaffoldGenerator;
import com.example.reactplatform.domain.reactgenerate.deploy.DeployResult;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployProperties;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GitPrDeployStrategy implements ReactDeployStrategy {

    private final GitPrApiClient gitPrApiClient;
    private final ReactDeployProperties properties;
    private final ContainerScaffoldGenerator scaffoldGenerator;

    @Override
    public DeployResult deploy(String codeId, String reactCode, String componentName) {
        if (reactCode == null || reactCode.isBlank()) {
            log.warn("[git-pr] React 코드가 비어 있어 PR 생성을 건너뜁니다. codeId={}", codeId);
            return DeployResult.failure("React 코드가 비어 있습니다.");
        }
        // DB 저장값이 null/공백인 경우 방어 — 파일명 "null.tsx" 생성 방지
        if (componentName == null || componentName.isBlank()) {
            log.warn("[git-pr] componentName이 비어 있어 기본값 사용. codeId={}", codeId);
            componentName = "GeneratedComponent";
        }

        ReactDeployProperties.GitPr gitPr = properties.getGitPr();
        String branchName = "reactplatform/" + codeId;

        try {
            // 1. base 브랜치 최신 SHA 조회
            String baseSha = gitPrApiClient.getBaseSha(gitPr.getBaseBranch());

            // 2. feature 브랜치 생성
            gitPrApiClient.createBranch(branchName, baseSha);

            // 3. UI 컴포넌트 파일 커밋 ({ComponentName}.tsx)
            String componentFilePath = gitPr.getComponentPath() + "/" + componentName + ".tsx";
            gitPrApiClient.createOrUpdateFile(
                    branchName, componentFilePath, reactCode, "feat: React UI 컴포넌트 추가 — " + componentName);

            // 4. Container scaffold 파일 커밋
            // Container에서 UI 컴포넌트를 import할 상대 경로: container → component 방향
            String importPrefix = resolveImportPrefix(gitPr.getComponentPath(), gitPr.getContainerPath());
            String scaffoldCode = scaffoldGenerator.generate(componentName, reactCode, importPrefix);
            String scaffoldFileName = scaffoldGenerator.resolveFileName(componentName);
            String containerFilePath = gitPr.getContainerPath() + "/" + scaffoldFileName;
            gitPrApiClient.createOrUpdateFile(
                    branchName, containerFilePath, scaffoldCode, "feat: Container scaffold 추가 — " + scaffoldFileName);

            // 5. 기존 열린 PR이 있으면 재사용, 없으면 신규 생성
            String prTitle = "feat: [React 코드 배포] " + componentName + " — " + codeId;
            String prBody = buildPrBody(componentName, codeId, componentFilePath, containerFilePath);
            String existingPrUrl = gitPrApiClient.findOpenPrUrl(branchName);
            String prUrl = existingPrUrl != null
                    ? existingPrUrl
                    : gitPrApiClient.createPullRequest(branchName, gitPr.getBaseBranch(), prTitle, prBody);

            log.info("[git-pr] 배포 완료 — codeId={}, pr={}", codeId, prUrl);
            return DeployResult.success(prUrl);

        } catch (Exception e) {
            // GitHub API 실패는 비치명적 — DB 승인은 이미 커밋됨
            log.error("[git-pr] PR 생성 실패 — codeId={}, branch={}", codeId, branchName, e);
            return DeployResult.failure(e.getMessage());
        }
    }

    /**
     * Container 파일에서 UI 컴포넌트를 import할 상대 경로를 계산한다.
     *
     * <p>
     * 예: componentPath={@code src/generated}, containerPath={@code src/containers}
     * → 같은 부모({@code src})를 공유하므로 {@code ../generated} 반환
     */
    private String resolveImportPrefix(String componentPath, String containerPath) {
        try {
            // OS 경로 구분자와 무관하게 동작하도록 Path.of() 전에 \ → / 로 정규화
            java.nio.file.Path comp =
                    java.nio.file.Path.of(componentPath.replace("\\", "/")).normalize();
            java.nio.file.Path cont =
                    java.nio.file.Path.of(containerPath.replace("\\", "/")).normalize();
            String relative = cont.relativize(comp).toString().replace("\\", "/");
            // 경로가 동일할 때 relative는 "" → "./" 반환 시 import 경로에 이중 슬래시가 생기므로 "." 반환
            return relative.isEmpty() ? "." : (relative.startsWith(".") ? relative : "./" + relative);
        } catch (Exception e) {
            log.warn("[git-pr] import 경로 계산 실패 — fallback '../generated' 사용");
            return "../generated";
        }
    }

    /** PR 본문을 마크다운으로 구성한다. */
    private String buildPrBody(String componentName, String codeId, String componentPath, String containerPath) {
        return String.format(
                """
                        ## 개요
                        AI가 생성하고 승인된 React 컴포넌트를 배포합니다.

                        ## 파일 구성
                        | 역할 | 파일 |
                        |------|------|
                        | UI 컴포넌트 (수정 금지) | `%s` |
                        | Container scaffold (비즈니스 로직 추가) | `%s` |

                        ## 개발자 작업
                        1. `%s` 파일을 열어 `// TODO:` 영역에 비즈니스 로직을 구현합니다.
                        2. 상태 관리, API 호출, 이벤트 핸들러를 추가하고 `%s`에 props로 연결합니다.
                        3. 작업 완료 후 이 PR에 커밋을 추가하고 리뷰를 요청합니다.

                        ---
                        > **codeId**: `%s`
                        > UI 컴포넌트는 AI 생성 코드이므로 직접 수정하지 말고 Container를 통해 사용하세요.
                        """,
                componentPath, containerPath, containerPath, componentName, codeId);
    }
}
