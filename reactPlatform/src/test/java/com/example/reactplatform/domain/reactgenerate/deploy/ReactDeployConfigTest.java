/**
 * @file ReactDeployConfigTest.java
 * @description ReactDeployConfig 단위 테스트.
 *     팩토리 메서드가 mode에 따라 올바른 전략 구현체 인스턴스를 반환하는지 검증한다.
 *     @ConditionalOnProperty 조건 평가는 Spring 컨텍스트 없이 팩토리 메서드 직접 호출로 대체한다.
 * @see ReactDeployConfig
 */
package com.example.reactplatform.domain.reactgenerate.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactplatform.domain.reactgenerate.deploy.gitpr.GitPrDeployStrategy;
import com.example.reactplatform.domain.reactgenerate.deploy.local.LocalFileDeployStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class ReactDeployConfigTest {

    private final ReactDeployConfig config = new ReactDeployConfig();
    private final ContainerScaffoldGenerator scaffoldGenerator = new ContainerScaffoldGenerator();

    @Test
    @DisplayName("localFileDeployStrategy 팩토리 메서드는 LocalFileDeployStrategy 인스턴스를 반환한다")
    void localFileDeployStrategy_returnsLocalFileDeployStrategyInstance() {
        ReactDeployStrategy strategy = config.localFileDeployStrategy(new ReactDeployProperties(), scaffoldGenerator);

        assertThat(strategy).isInstanceOf(LocalFileDeployStrategy.class);
    }

    @Test
    @DisplayName("gitPrDeployStrategy 팩토리 메서드는 GitPrDeployStrategy 인스턴스를 반환한다")
    void gitPrDeployStrategy_returnsGitPrDeployStrategyInstance() {
        ReactDeployProperties props = new ReactDeployProperties();
        ReactDeployProperties.GitPr gitPr = new ReactDeployProperties.GitPr();
        gitPr.setToken("ghp_test");
        gitPr.setOwner("test-owner");
        gitPr.setRepo("test-repo");
        props.setGitPr(gitPr);

        ReactDeployStrategy strategy = config.gitPrDeployStrategy(props, scaffoldGenerator, new RestTemplateBuilder());

        assertThat(strategy).isInstanceOf(GitPrDeployStrategy.class);
    }

    @Test
    @DisplayName("두 팩토리 메서드는 서로 다른 타입의 전략을 반환한다")
    void twoFactoryMethods_returnDifferentStrategyTypes() {
        ReactDeployProperties localProps = new ReactDeployProperties();
        ReactDeployStrategy localStrategy = config.localFileDeployStrategy(localProps, scaffoldGenerator);

        ReactDeployProperties gitPrProps = new ReactDeployProperties();
        ReactDeployProperties.GitPr gitPr = new ReactDeployProperties.GitPr();
        gitPr.setToken("token");
        gitPr.setOwner("owner");
        gitPr.setRepo("repo");
        gitPrProps.setGitPr(gitPr);
        ReactDeployStrategy gitPrStrategy =
                config.gitPrDeployStrategy(gitPrProps, scaffoldGenerator, new RestTemplateBuilder());

        assertThat(localStrategy).isNotInstanceOf(GitPrDeployStrategy.class);
        assertThat(gitPrStrategy).isNotInstanceOf(LocalFileDeployStrategy.class);
    }
}
