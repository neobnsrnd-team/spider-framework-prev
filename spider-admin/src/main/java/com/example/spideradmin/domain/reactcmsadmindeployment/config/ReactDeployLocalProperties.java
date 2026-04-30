package com.example.spideradmin.domain.reactcmsadmindeployment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * React CMS 로컬 배포 설정.
 *
 * <p>application.yml의 react.deploy.local 섹션에서 설정값을 읽어옵니다.</p>
 *
 * <pre>{@code
 * react:
 *   deploy:
 *     local:
 *       component-dir: ../demo/front/src/reactcms/generated
 *       container-dir: ../demo/front/src/reactcms/containers
 *       instance-id: demo-local-react
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "react.deploy.local")
@Getter
@Setter
public class ReactDeployLocalProperties {

    /** 생성된 JSX 컴포넌트 파일을 저장할 로컬 디렉토리 경로 */
    private String componentDir;

    /** demo/front 라우팅용 컨테이너 파일을 저장할 로컬 디렉토리 경로 */
    private String containerDir;

    /** 배포 이력 기록에 사용할 FWK_CMS_SERVER_INSTANCE.INSTANCE_ID */
    private String instanceId;
}
