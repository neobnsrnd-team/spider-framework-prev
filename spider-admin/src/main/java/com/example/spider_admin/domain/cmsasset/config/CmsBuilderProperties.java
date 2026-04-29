package com.example.spider_admin.domain.cmsasset.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CMS Builder(미승인 이미지 업로드) 호출 설정 — Issue #65.
 *
 * <p>Spider Admin 은 파일을 직접 저장하지 않고 CMS Builder 서버로 포워딩한다.
 * 내부망 서버-투-서버 호출 전제이며, 인증 토큰은 현재 없음.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cms.builder")
public class CmsBuilderProperties {

    /** CMS Builder 서버 기본 URL (예: http://133.186.135.23) — nginx 80 → Next.js 3000 업스트림 프록시 */
    private String baseUrl;

    /** 업로드 엔드포인트 경로 (예: /cms/api/builder/upload) */
    private String uploadPath;

    /** 연결 타임아웃 (초) */
    private int connectTimeoutSeconds = 5;

    /** 읽기 타임아웃 (초). 대용량 이미지 업로드를 고려해 넉넉히 잡는다. */
    private int readTimeoutSeconds = 60;

    /**
     * 배포(deploy) 호출 전용 연결 타임아웃 (초).
     * 업로드처럼 대용량 전송이 없어 짧게 잡는다 (Issue #55).
     */
    private int deployConnectTimeoutSeconds = 5;

    /**
     * 배포(deploy) 호출 전용 읽기 타임아웃 (초).
     * 파일 이동은 CMS 내부 디스크 I/O 로 수 초 내 완료되어야 한다. 60초는 너무 길어
     * Admin HTTP 스레드·사용자 UI 대기 시간을 과다하게 잡기 때문에 짧게 둔다 (Issue #55, Gemini 리뷰 반영).
     */
    private int deployReadTimeoutSeconds = 10;

    /** x-deploy-token 헤더값. 페이지 배포(cms.deploy.secret)와 동일한 CMS DEPLOY_SECRET 을 공유한다. */
    private String deploySecret;
}
