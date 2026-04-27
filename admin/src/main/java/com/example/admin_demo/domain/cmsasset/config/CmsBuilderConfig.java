package com.example.admin_demo.domain.cmsasset.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * CMS Builder 호출용 RestClient 빈 구성 — Issue #65.
 *
 * <p>별도 RestClient 로 분리한 이유: 타임아웃과 baseUrl 이 다른 CMS 호출과 격리되어야 하고,
 * 업로드 호출의 독립적인 타임아웃을 관리한다.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CmsBuilderProperties.class)
public class CmsBuilderConfig {

    private final CmsBuilderProperties properties;

    /**
     * CMS Builder 전용 RestClient — 업로드·삭제 호출에 사용 (대용량 업로드를 고려한 long read-timeout).
     * CMS 엔드포인트는 x-deploy-token 헤더 인증을 요구하므로 defaultHeader로 고정 주입한다.
     */
    @Bean
    public RestClient cmsBuilderRestClient() {
        return buildClient(properties.getConnectTimeoutSeconds(), properties.getReadTimeoutSeconds())
                .mutate()
                .defaultHeader("x-deploy-token", properties.getDeploySecret())
                .build();
    }

    /**
     * 배포(deploy) 전용 RestClient — Issue #55.
     *
     * <p>파일 이동은 CMS 내부 I/O 라 수 초 내 완료되어야 하므로 업로드용(60초) 보다 훨씬 짧은
     * read-timeout 을 적용한다. 지연 시 Admin 의 HTTP 스레드와 사용자 UI 가 오래 묶이는 것을 방지.
     * CMS deploy 엔드포인트는 x-deploy-token 헤더 인증을 요구하므로 defaultHeader 로 고정 주입한다.
     */
    @Bean
    public RestClient cmsBuilderDeployRestClient() {
        return buildClient(properties.getDeployConnectTimeoutSeconds(), properties.getDeployReadTimeoutSeconds())
                .mutate()
                .defaultHeader("x-deploy-token", properties.getDeploySecret())
                .build();
    }

    /** baseUrl 공유 + 타임아웃만 다르게 RestClient 를 생성. */
    private RestClient buildClient(int connectTimeoutSec, int readTimeoutSec) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSec));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSec));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
