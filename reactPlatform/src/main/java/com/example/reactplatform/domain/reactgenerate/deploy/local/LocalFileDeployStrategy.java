/**
 * @file LocalFileDeployStrategy.java
 * @description {@code react.deploy.mode: local} 일 때 사용하는 배포 전략.
 *     승인된 React UI 컴포넌트와 Container scaffold를 서버 로컬 파일시스템에 기록한다.
 *
 * <p>저장 경로:
 * <ul>
 *   <li>UI 컴포넌트: {@code {local.component-dir}/{ComponentName}.tsx}</li>
 *   <li>Container scaffold: {@code {local.container-dir}/{ComponentName}Container.tsx}</li>
 * </ul>
 *
 * <p>UI 컴포넌트 파일 쓰기 실패는 {@link DeployResult#failure}로 반환한다.
 * Container scaffold 실패는 비치명적으로 처리한다 — 컴포넌트는 이미 배포되었으므로
 * scaffold 누락만 로그에 남기고 SUCCESS를 반환한다.
 */
package com.example.reactplatform.domain.reactgenerate.deploy.local;

import com.example.reactplatform.domain.reactgenerate.deploy.ContainerScaffoldGenerator;
import com.example.reactplatform.domain.reactgenerate.deploy.DeployResult;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployProperties;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployStrategy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalFileDeployStrategy implements ReactDeployStrategy {

    private final ReactDeployProperties properties;
    private final ContainerScaffoldGenerator scaffoldGenerator;

    @Override
    public DeployResult deploy(String codeId, String reactCode, String componentName) {
        if (reactCode == null || reactCode.isBlank()) {
            log.warn("[local] React 코드가 비어 있어 파일 생성을 건너뜁니다. codeId={}", codeId);
            return DeployResult.failure("React 코드가 비어 있습니다.");
        }

        ReactDeployProperties.Local local = properties.getLocal();

        try {
            writeComponent(codeId, componentName, reactCode, local.getComponentDir());
        } catch (Exception e) {
            log.error("[local] UI 컴포넌트 파일 생성 실패 — codeId={}", codeId, e);
            return DeployResult.failure(e.getMessage());
        }

        // Container scaffold 실패는 비치명적 — 컴포넌트는 이미 배포 완료
        writeContainerScaffold(codeId, componentName, reactCode, local.getComponentDir(), local.getContainerDir());
        return DeployResult.success(null);
    }

    /**
     * UI 컴포넌트 파일({ComponentName}.tsx)을 component-dir에 기록한다.
     *
     * @throws IllegalStateException component-dir이 설정되지 않은 경우
     * @throws IOException           파일 쓰기 실패 시
     */
    private void writeComponent(String codeId, String componentName, String reactCode, String componentDir)
            throws IOException {
        if (isBlank(componentDir)) {
            throw new IllegalStateException("component-dir이 설정되지 않았습니다. codeId=" + codeId);
        }
        writeFile(Path.of(componentDir).resolve(componentName + ".tsx"), reactCode, "UI 컴포넌트", codeId);
    }

    /** Container scaffold 파일을 container-dir에 기록한다. 실패 시 로그만 남기고 반환한다. */
    private void writeContainerScaffold(
            String codeId, String componentName, String reactCode, String componentDir, String containerDir) {
        if (isBlank(containerDir)) {
            log.warn("[local] container-dir이 설정되지 않아 Container scaffold 생성을 건너뜁니다. codeId={}", codeId);
            return;
        }

        try {
            // Container에서 UI 컴포넌트를 import할 때 사용하는 상대 경로 계산
            String importPrefix = resolveImportPrefix(componentDir, containerDir);
            String scaffoldCode = scaffoldGenerator.generate(componentName, reactCode, importPrefix);
            String fileName = scaffoldGenerator.resolveFileName(componentName);
            writeFile(Path.of(containerDir).resolve(fileName), scaffoldCode, "Container scaffold", codeId);
        } catch (IOException e) {
            log.error("[local] Container scaffold 파일 생성 실패 — codeId={}", codeId, e);
        }
    }

    /** 파일을 생성하고 내용을 UTF-8로 기록한다. */
    private void writeFile(Path target, String content, String label, String codeId) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
        log.info("[local] {} 파일 생성 완료 — path={}, codeId={}", label, target.toAbsolutePath(), codeId);
    }

    /**
     * Container에서 UI 컴포넌트를 import할 때 사용하는 상대 경로를 계산한다.
     *
     * <p>예: componentDir={@code ../demo/front/src/generated},
     *      containerDir={@code ../demo/front/src/containers}
     *     → {@code ../generated}
     *
     * <p>두 경로가 같은 부모 디렉토리를 공유하지 않는 복잡한 경우에는 절대 경로를 반환한다.
     */
    private String resolveImportPrefix(String componentDir, String containerDir) {
        try {
            Path compPath = Path.of(componentDir).toAbsolutePath().normalize();
            Path contPath = Path.of(containerDir).toAbsolutePath().normalize();
            // container → component 로의 상대 경로 계산
            Path relative = contPath.relativize(compPath);
            String prefix = relative.toString().replace("\\", "/");
            // 상대 경로가 ".."로 시작하지 않으면 "./" 접두사 추가
            return prefix.startsWith(".") ? prefix : "./" + prefix;
        } catch (Exception e) {
            // 경로 계산 실패 시 fallback — 런타임에 개발자가 직접 수정 가능
            log.warn("[local] import 경로 계산 실패 — fallback '../generated' 사용. componentDir={}, containerDir={}",
                    componentDir, containerDir);
            return "../generated";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
