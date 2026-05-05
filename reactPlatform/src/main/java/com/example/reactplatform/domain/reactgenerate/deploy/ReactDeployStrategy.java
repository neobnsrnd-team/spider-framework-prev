/**
 * @file ReactDeployStrategy.java
 * @description 승인된 React 코드를 배포하는 전략 인터페이스.
 *     {@code react.deploy.mode} 설정값에 따라 {@link local.LocalFileDeployStrategy} 또는
 *     {@link gitpr.GitPrDeployStrategy} 구현체가 주입된다.
 */
package com.example.reactplatform.domain.reactgenerate.deploy;

public interface ReactDeployStrategy {

    /**
     * 승인된 React UI 컴포넌트와 Container scaffold를 배포한다.
     *
     * @param codeId        승인된 코드 ID (파일명·브랜치명 등에 사용)
     * @param reactCode     배포할 React TSX 코드
     * @param componentName UI 컴포넌트 함수명 (DB 저장값 — 코드 파싱 없이 파일명 결정에 사용)
     * @return 배포 결과 — 성공 여부, PR URL(git-pr 모드), 실패 사유를 포함
     */
    DeployResult deploy(String codeId, String reactCode, String componentName);
}
