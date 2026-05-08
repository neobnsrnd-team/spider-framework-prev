package com.example.spideradmin.domain.cmsdeployment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CMS 배포 설정
 *
 * <p>application.yml의 cms.deploy 섹션에서 설정값을 읽어옵니다.</p>
 */
@Component
@ConfigurationProperties(prefix = "cms.deploy")
@Getter
@Setter
public class CmsDeployProperties {

    /** CMS push 엔드포인트 URL — HTML 조립·파일 저장·이력 기록을 CMS가 담당 */
    private String pushUrl;

    /** 배포 인증 토큰 (x-deploy-token 헤더) */
    private String secret;

    /**
     * 배포된 파일 URL 구성 시 사용할 프로토콜 (기본값: http)
     * 운영 환경이 HTTPS인 경우 https로 설정
     */
    private String deployedProtocol = "http";

    /**
     * 배포된 파일 URL 구성 시 사용할 경로 접두사 (기본값: /deployed)
     * 운영 환경에 따라 경로가 다를 경우 변경
     */
    private String deployedPathPrefix = "/deployed";

    /**
     * 만료 배포 시 전송할 page-expired.html URL
     * CMS Next.js 서버의 public/system/page-expired.html을 HTTP로 읽어온다.
     * Admin 내부에 파일을 복사하지 않아 단일 소스를 유지한다.
     */
    private String expiredHtmlUrl;

    /**
     * CMS 배포 API base URL — pushUrl에서 마지막 경로 세그먼트('/push')를 제거한 값.
     * 긴급차단·복구 API URL 조립 시 사용한다.
     * 예: http://localhost:3000/cms/api/deploy/push → http://localhost:3000/cms/api/deploy
     */
    public String getDeployBaseUrl() {
        if (pushUrl == null) return "";
        int idx = pushUrl.lastIndexOf('/');
        return idx > 0 ? pushUrl.substring(0, idx) : pushUrl;
    }
}
