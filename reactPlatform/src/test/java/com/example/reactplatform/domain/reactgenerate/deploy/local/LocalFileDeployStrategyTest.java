/**
 * @file LocalFileDeployStrategyTest.java
 * @description LocalFileDeployStrategy 단위 테스트.
 *     @TempDir로 실제 파일 I/O를 검증하고, DeployResult 반환값,
 *     설정 누락·예외 발생 시 처리 방식을 확인한다.
 * @see LocalFileDeployStrategy
 */
package com.example.reactplatform.domain.reactgenerate.deploy.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.reactplatform.domain.reactgenerate.deploy.ContainerScaffoldGenerator;
import com.example.reactplatform.domain.reactgenerate.deploy.DeployResult;
import com.example.reactplatform.domain.reactgenerate.deploy.ReactDeployProperties;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalFileDeployStrategyTest {

    @TempDir
    Path tempDir;

    @Mock
    private ContainerScaffoldGenerator scaffoldGenerator;

    private static final String REACT_CODE = "export default function LoginPage() { return <div/>; }";
    private static final String SCAFFOLD_CODE = "// scaffold content";
    private static final String SCAFFOLD_FILE = "LoginPageContainer.tsx";
    private static final String COMPONENT_NAME = "LoginPage";

    // ========== 정상 배포 ==========

    @Test
    @DisplayName("정상 배포 시 UI 컴포넌트({ComponentName}.tsx)와 Container scaffold 파일이 생성된다")
    void deploy_success_writesBothFiles() throws Exception {
        Path compDir = tempDir.resolve("generated");
        Path contDir = tempDir.resolve("containers");
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);

        DeployResult result =
                strategy(compDir.toString(), contDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrUrl()).isNull(); // local 모드는 PR URL 없음
        assertThat(compDir.resolve("LoginPage.tsx")).exists().hasContent(REACT_CODE);
        assertThat(contDir.resolve(SCAFFOLD_FILE)).exists().hasContent(SCAFFOLD_CODE);
    }

    @Test
    @DisplayName("존재하지 않는 중첩 디렉토리도 자동으로 생성된다")
    void deploy_deepDirectoryAutoCreated() throws Exception {
        Path compDir = tempDir.resolve("deep/nested/generated");
        Path contDir = tempDir.resolve("deep/nested/containers");
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);

        DeployResult result =
                strategy(compDir.toString(), contDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isTrue();
        assertThat(compDir.resolve("LoginPage.tsx")).exists();
        assertThat(contDir.resolve(SCAFFOLD_FILE)).exists();
    }

    @Test
    @DisplayName("UI 컴포넌트 파일명은 componentName 파라미터 기반의 {ComponentName}.tsx 형식이다")
    void deploy_componentFileName_isComponentNameDotTsx() throws Exception {
        Path compDir = tempDir.resolve("generated");
        Path contDir = tempDir.resolve("containers");
        when(scaffoldGenerator.generate(anyString(), anyString(), anyString())).thenReturn(SCAFFOLD_CODE);
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn("TransferFormContainer.tsx");

        // componentName을 deploy()에 직접 전달 — 코드 파싱 없이 파일명 결정
        strategy(compDir.toString(), contDir.toString()).deploy("code-99", REACT_CODE, "TransferForm");

        assertThat(compDir.resolve("TransferForm.tsx")).exists();
    }

    // ========== import prefix 계산 ==========

    @Test
    @DisplayName("Container → Component 상대 경로(import prefix)가 올바르게 계산되어 scaffoldGenerator에 전달된다")
    void deploy_importPrefix_relativePathPassedToGenerator() throws Exception {
        // containers → generated : ../generated
        Path compDir = tempDir.resolve("generated");
        Path contDir = tempDir.resolve("containers");
        when(scaffoldGenerator.resolveFileName(anyString())).thenReturn(SCAFFOLD_FILE);

        // generate(componentName, reactCode, importPrefix) — 3번째 인자(importPrefix) 캡처
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        when(scaffoldGenerator.generate(anyString(), anyString(), prefixCaptor.capture()))
                .thenReturn(SCAFFOLD_CODE);

        strategy(compDir.toString(), contDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        // 상대 경로에 "generated" 디렉토리명이 포함되어야 한다
        assertThat(prefixCaptor.getValue()).contains("generated");
    }

    // ========== 빈 코드 처리 ==========

    @Test
    @DisplayName("React 코드가 빈 문자열이면 파일을 생성하지 않고 FAILED 결과를 반환한다")
    void deploy_emptyCode_returnsFailure() {
        DeployResult result = strategy(tempDir.toString(), tempDir.toString()).deploy("code-01", "", COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
        assertThat(tempDir.toFile().listFiles()).isEmpty();
    }

    @Test
    @DisplayName("React 코드가 null이면 파일을 생성하지 않고 FAILED 결과를 반환한다")
    void deploy_nullCode_returnsFailure() {
        DeployResult result = strategy(tempDir.toString(), tempDir.toString()).deploy("code-01", null, COMPONENT_NAME);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    // ========== 설정 누락 처리 ==========

    @Test
    @DisplayName("component-dir이 null이면 FAILED 결과를 반환하고 예외를 던지지 않는다")
    void deploy_nullComponentDir_returnsFailure() {
        DeployResult result = strategy(null, tempDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        assertThatNoException()
                .isThrownBy(() -> strategy(null, tempDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    @Test
    @DisplayName("container-dir이 null이어도 UI 컴포넌트는 정상 생성되고 SUCCESS를 반환한다")
    void deploy_nullContainerDir_writesComponentAndReturnsSuccess() throws Exception {
        Path compDir = tempDir.resolve("generated");
        // containerDir=null → generate/resolveFileName 미호출

        DeployResult result = strategy(compDir.toString(), null).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        assertThat(result.isSuccess()).isTrue();
        assertThat(compDir.resolve("LoginPage.tsx")).exists();
        assertThat(tempDir.resolve(SCAFFOLD_FILE)).doesNotExist();
    }

    @Test
    @DisplayName("component-dir이 빈 문자열이면 FAILED 결과를 반환하고 예외를 던지지 않는다")
    void deploy_blankComponentDir_returnsFailure() {
        DeployResult result = strategy("", tempDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME);

        assertThatNoException()
                .isThrownBy(() -> strategy("", tempDir.toString()).deploy("code-01", REACT_CODE, COMPONENT_NAME));
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).isNotBlank();
    }

    // ========== helpers ==========

    /** 지정된 component-dir, container-dir으로 전략 인스턴스를 생성한다. */
    private LocalFileDeployStrategy strategy(String componentDir, String containerDir) {
        ReactDeployProperties props = new ReactDeployProperties();
        ReactDeployProperties.Local local = new ReactDeployProperties.Local();
        local.setComponentDir(componentDir);
        local.setContainerDir(containerDir);
        props.setLocal(local);
        return new LocalFileDeployStrategy(props, scaffoldGenerator);
    }
}
