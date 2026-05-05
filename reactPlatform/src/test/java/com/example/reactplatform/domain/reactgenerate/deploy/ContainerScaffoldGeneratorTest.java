/**
 * @file ContainerScaffoldGeneratorTest.java
 * @description ContainerScaffoldGenerator 단위 테스트.
 *     generate() scaffold 구조(import 경로, TODO 주석, 파일명)와
 *     resolveFileName() 파일명 생성을 검증한다.
 *     컴포넌트명은 DB 저장값을 직접 전달받으므로 코드 파싱 로직 테스트는 포함하지 않는다.
 * @see ContainerScaffoldGenerator
 */
package com.example.reactplatform.domain.reactgenerate.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContainerScaffoldGeneratorTest {

    private final ContainerScaffoldGenerator generator = new ContainerScaffoldGenerator();

    private static final String COMPONENT_NAME = "LoginPage";
    private static final String REACT_CODE = "export default function LoginPage() { return <div/>; }";

    // ========== generate() ==========

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("componentName으로 Container 클래스명(LoginPageContainer)이 생성된다")
        void generate_withComponentName_createsContainerName() {
            String scaffold = generator.generate(COMPONENT_NAME, REACT_CODE, "../generated");

            assertThat(scaffold).contains("LoginPageContainer");
            assertThat(scaffold).contains("LoginPage");
        }

        @Test
        @DisplayName("생성된 scaffold에 importPrefix와 componentName을 조합한 import 경로가 포함된다")
        void generate_importPath_containsPrefixAndComponentName() {
            String scaffold = generator.generate(COMPONENT_NAME, REACT_CODE, "../generated");

            assertThat(scaffold).contains("'../generated/LoginPage'");
        }

        @Test
        @DisplayName("생성된 scaffold에 개발자 작업을 위한 TODO 주석이 포함된다")
        void generate_containsTodoComments() {
            String scaffold = generator.generate(COMPONENT_NAME, REACT_CODE, "../generated");

            assertThat(scaffold).contains("TODO");
        }

        @Test
        @DisplayName("생성된 scaffold에 JSDoc 파일 주석(@file, @description)이 포함된다")
        void generate_containsJsDocFileComment() {
            String scaffold = generator.generate(COMPONENT_NAME, REACT_CODE, "../generated");

            assertThat(scaffold).contains("@file");
            assertThat(scaffold).contains("@description");
        }

        @Test
        @DisplayName("fallback 이름(GeneratedComponent)을 componentName으로 전달하면 GeneratedComponentContainer가 생성된다")
        void generate_withFallbackName_createsGeneratedComponentContainer() {
            String scaffold = generator.generate("GeneratedComponent", REACT_CODE, "../generated");

            assertThat(scaffold).contains("GeneratedComponentContainer");
            assertThat(scaffold).contains("GeneratedComponent");
        }

        @Test
        @DisplayName("./ 시작 importPrefix도 import 경로에 올바르게 반영된다")
        void generate_dotSlashImportPrefix_includedInImportPath() {
            String scaffold = generator.generate(COMPONENT_NAME, REACT_CODE, "./generated");

            assertThat(scaffold).contains("'./generated/LoginPage'");
        }
    }

    // ========== resolveFileName() ==========

    @Nested
    @DisplayName("resolveFileName()")
    class ResolveFileName {

        @Test
        @DisplayName("componentName을 받아 {ComponentName}Container.tsx를 반환한다")
        void resolveFileName_withComponentName_returnsContainerFileName() {
            assertThat(generator.resolveFileName("LoginPage")).isEqualTo("LoginPageContainer.tsx");
        }

        @Test
        @DisplayName("fallback 이름(GeneratedComponent)을 전달하면 GeneratedComponentContainer.tsx를 반환한다")
        void resolveFileName_withFallbackName_returnsFallback() {
            assertThat(generator.resolveFileName("GeneratedComponent")).isEqualTo("GeneratedComponentContainer.tsx");
        }

        @Test
        @DisplayName("다양한 컴포넌트명을 올바르게 처리한다")
        void resolveFileName_variousComponentNames_parsedCorrectly() {
            assertThat(generator.resolveFileName("DashboardPage")).isEqualTo("DashboardPageContainer.tsx");
            assertThat(generator.resolveFileName("MyComponent")).isEqualTo("MyComponentContainer.tsx");
        }
    }
}
