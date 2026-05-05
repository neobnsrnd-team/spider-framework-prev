/**
 * @file ReactDeployConfig.java
 * @description {@code react.deploy.mode} 값에 따라 {@link ReactDeployStrategy} 구현체를 Bean으로 등록한다.
 *
 * <ul>
 *   <li>{@code mode: local} (기본값) → {@link local.LocalFileDeployStrategy}</li>
 *   <li>{@code mode: git-pr} → {@link gitpr.GitPrDeployStrategy}</li>
 * </ul>
 */
package com.example.reactplatform.domain.reactgenerate.deploy;

import com.example.reactplatform.domain.reactgenerate.deploy.gitpr.GitPrApiClient;
import com.example.reactplatform.domain.reactgenerate.deploy.gitpr.GitPrDeployStrategy;
import com.example.reactplatform.domain.reactgenerate.deploy.local.LocalFileDeployStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReactDeployConfig {

    /**
     * local 모드 전략 Bean.
     * {@code react.deploy.mode}가 {@code local}이거나 설정되지 않은 경우 활성화된다.
     */
    @Bean
    @ConditionalOnProperty(name = "react.deploy.mode", havingValue = "local", matchIfMissing = true)
    public ReactDeployStrategy localFileDeployStrategy(
            ReactDeployProperties properties, ContainerScaffoldGenerator scaffoldGenerator) {
        return new LocalFileDeployStrategy(properties, scaffoldGenerator);
    }

    /**
     * git-pr 모드 전략 Bean.
     * {@code react.deploy.mode: git-pr} 설정 시에만 활성화된다.
     *
     * <p>GitHub API 호출용 RestTemplate은 Figma/Claude 전용 클라이언트와 별도로 구성한다 —
     * 타임아웃 등 설정이 다를 수 있고, 불필요한 공유 상태를 방지한다.
     */
    @Bean
    @ConditionalOnProperty(name = "react.deploy.mode", havingValue = "git-pr")
    public ReactDeployStrategy gitPrDeployStrategy(
            ReactDeployProperties properties,
            ContainerScaffoldGenerator scaffoldGenerator,
            RestTemplateBuilder restTemplateBuilder) {
        ReactDeployProperties.GitPr gitPr = properties.getGitPr();
        GitPrApiClient apiClient =
                new GitPrApiClient(restTemplateBuilder.build(), gitPr.getToken(), gitPr.getOwner(), gitPr.getRepo());
        return new GitPrDeployStrategy(apiClient, properties, scaffoldGenerator);
    }
}
