package com.example.admin_demo.domain.reactcmsadmindeployment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * React CMS 로컬 배포 경로 설정.
 *
 * <p>application.yml의 react.deploy.local 섹션에서 읽어옵니다.
 * 경로는 admin 프로젝트 루트 기준 상대 경로로 지정하며, 절대 경로도 허용합니다.</p>
 */
@Component
@ConfigurationProperties(prefix = "react.deploy.local")
@Getter
@Setter
public class ReactDeployLocalProperties {

    /** 생성된 JSX 컴포넌트 파일을 저장할 디렉토리 — PAGE_DESC 내용을 {pageId}.tsx로 저장 */
    private String componentDir = "../demo/front/src/reactcms/generated";

    /** demo/front 라우팅용 컨테이너 파일을 저장할 디렉토리 — {pageId}.tsx re-export 래퍼 */
    private String containerDir = "../demo/front/src/reactcms/containers";

    /** 배포 이력 기록에 사용할 FWK_CMS_SERVER_INSTANCE.INSTANCE_ID */
    private String instanceId = "demo-local-react";
}
