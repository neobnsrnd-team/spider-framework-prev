# CMS 배포/정적 서빙 후속 이슈

## 목표

Issue 8에서 확인한 CMS 리소스 라이프사이클 정책은 유지하되, 실제 배포 실행에서 발견된 배포 수신 URL, 인증 secret, 배포 HTML 서빙 경로, 미리보기 URL 불일치를 후속 이슈로 분리해 해결한다.

이 문서는 `cms-admin-migration-issues.md`의 Issue 9 배포 관리 이관을 진행할 때 함께 처리할 세부 작업 기준이다.

## 현재 확인된 문제

Issue 8 검증 중 다음 문제가 확인됐다.

- `spider-admin`의 배포 요청이 처음에는 `http://133.186.135.23:3001/api/deploy/receive`로 전송되어 `404 Not Found`가 발생했다.
- CMS Next는 `basePath=/cms`로 동작하므로 receive API는 `/cms/api/deploy/receive` 경로 기준으로 호출되어야 한다.
- receive URL을 `/cms/api/deploy/receive`로 맞춘 뒤에는 `401 Unauthorized`가 발생했다.
- CMS Next receive API는 `DEPLOY_SECRET`과 요청 헤더 `x-deploy-token`을 비교한다.
- `spider-admin`은 `cms.deploy.secret` 값을 `x-deploy-token`으로 보내므로 두 서비스의 secret 설정이 같아야 한다.
- secret을 맞춘 뒤 배포 이력과 미리보기 URL 표시는 확인됐지만, `/cms/deployed/{pageId}.html` 접근 시에도 "페이지를 찾을 수 없습니다"가 표시됐다.
- 결론적으로 배포 수신, 인증, 미리보기 URL, 배포 HTML 정적 서빙 경로가 아직 하나의 계약으로 정렬되지 않았다.

## 범위

### 포함

- `spider-admin` 배포 receive URL 설정 정리
- `spider-admin` `cms.deploy.secret`과 CMS Next `DEPLOY_SECRET` 운영 설정 정렬
- 배포 HTML 물리 저장 위치와 브라우저 접근 URL 정렬
- 배포 미리보기 URL 생성 정책 수정
- CMS Next route 또는 nginx 정적 서빙 중 최종 서빙 방식 결정
- 배포 성공 후 이력 생성과 미리보기 URL 접근 검증
- 배포 HTML 내부 이미지/정적 리소스 URL 정책 정리
- 정적 리소스 백업/교체/rollback 운영 책임 경계 정리

### 제외

- 제작자 이미지 승인 요청 화면/API 구현
- 관리자 이미지 승인/반려 화면/API 구현
- CMS 편집기에서 승인된 이미지만 선택하도록 바꾸는 작업
- `/api/builder/upload` 즉시 삽입 차단 작업
- A/B 관리와 통계 조회 기능 이관

제외 항목 중 이미지 승인 관련 작업은 `docs/cms-image-approval-issues.md`에서 별도 관리한다. A/B와 통계는 `docs/cms-admin-migration-issues.md`의 Issue 9 본문 범위에서 진행한다.

## Issue 1. 배포 receive URL과 secret 설정 정렬

### 진행 시점

- Issue 8 완료 직후 진행한다.
- Issue 9의 배포 관리 기능 이관을 시작할 때 가장 먼저 처리한다.
- 배포 버튼 수동 검증이나 Issue 10 E2E 작성 전에 완료되어야 한다.

### 작업

- [ ] `spider-admin`의 `cms.deploy.receive-url` 기본값과 운영 설정을 CMS Next `basePath=/cms` 기준으로 정리한다.
  - 후보: `http://{host}:{port}/cms/api/deploy/receive`
- [ ] CMS Next receive API의 실제 운영 경로가 `/cms/api/deploy/receive`인지 확인한다.
- [ ] `spider-admin`의 `cms.deploy.secret`과 CMS Next의 `DEPLOY_SECRET` 설정 이름, 주입 방식, 운영 반영 위치를 문서화한다.
- [ ] 두 서비스의 secret이 다를 때 배포 실패 사유가 관리자 화면에서 확인 가능한지 검토한다.
- [ ] 설정 누락 시 테스트 또는 시작 로그에서 발견 가능한 방식으로 보완할지 결정한다.

### 확인 파일

```text
spider-admin:
- src/main/resources/application.yml
- src/main/java/com/example/admin_demo/domain/cmsdeployment/service/CmsDeployService.java
- src/test/java/com/example/admin_demo/domain/cmsdeployment/service/CmsDeployServiceTest.java

cms-1-innova-next:
- src/app/api/deploy/receive/route.ts
- next.config.ts
- .env.prod.example
- docker-compose-prod.yml
```

### 완료 조건

- `spider-admin` 배포 요청이 `404 Not Found` 없이 CMS Next receive API에 도달한다.
- secret이 맞으면 `401 Unauthorized`가 발생하지 않는다.
- secret이 틀리거나 없을 때의 실패 원인이 로그 또는 화면에서 추적 가능하다.

## Issue 2. 배포 HTML 저장 위치와 브라우저 서빙 경로 확정

### 진행 시점

- Issue 1 완료 후 바로 진행한다.
- Issue 9의 배포 push/history 검증 전에 완료되어야 한다.
- Issue 10 프록시/E2E 시나리오 작성 전에 최종 경로가 확정되어야 한다.

### 작업

- [ ] 배포 HTML의 물리 파일 저장 위치를 확정한다.
  - 정책 기준: `/data/deployed/{pageId}.html`
- [ ] CMS Next receive API가 실제로 `public/deployed/{pageId}.html` 또는 `/data/deployed/{pageId}.html`에 파일을 생성하는지 확인한다.
- [ ] 브라우저 접근 URL을 하나로 확정한다.
  - 후보 1: `/deployed/{pageId}.html`
  - 후보 2: `/cms/deployed/{pageId}.html`
- [ ] CMS Next `basePath=/cms` 적용 시 `public/deployed` 정적 파일이 어떤 URL로 서빙되는지 확인한다.
- [ ] 필요한 경우 CMS Next에 명시적 HTML 서빙 route를 추가한다.
- [ ] 필요한 경우 nginx가 `/deployed/**` 또는 `/cms/deployed/**`를 `/data/deployed/**`로 직접 서빙하도록 설정한다.
- [ ] `spider-admin` 배포 미리보기 URL 생성 로직을 최종 브라우저 접근 URL과 맞춘다.

### 확인 파일

```text
spider-admin:
- src/main/resources/mapper/oracle/cmsdeployment/CmsDeployMapper.xml
- src/main/java/com/example/admin_demo/domain/cmsdeployment/service/CmsDeployService.java
- src/main/resources/templates/pages/cms-deployment/*
- src/test/java/com/example/admin_demo/domain/cmsdeployment/controller/CmsDeployControllerTest.java
- src/test/java/com/example/admin_demo/domain/cmsdeployment/service/CmsDeployServiceTest.java

cms-1-innova-next:
- src/app/api/deploy/receive/route.ts
- next.config.ts
- public/deployed
- nginx/nginx.conf
- docker-compose-prod.yml
```

### 완료 조건

- 배포 성공 후 HTML 파일이 실제 저장 위치에 생성된다.
- `spider-admin`의 미리보기 URL을 브라우저에서 열면 배포 HTML이 `200`으로 열린다.
- "페이지를 찾을 수 없습니다"가 표시되지 않는다.
- 미리보기 URL 형식이 문서와 화면, DB 이력에서 모두 동일하다.

## Issue 3. 배포 HTML 내부 이미지/정적 리소스 URL 정책 정리

### 진행 시점

- Issue 2 이후 진행한다.
- 이미지 승인 분리 이슈에서 승인 이미지 URL과 저장 위치가 확정된 뒤 구현하는 것이 안전하다.
- 배포 HTML은 열리지만 이미지가 깨지는 경우 Issue 9 안에서 우선순위를 올려 처리한다.

### 작업

- [ ] `spider-admin` 배포 흐름에서 `PAGE_HTML`을 그대로 전송할지, URL 치환 후 전송할지 결정한다.
- [ ] `/cms/uploads/**`, `/uploads/**`, `/static/**`, `/api/assets/**`가 배포 HTML 안에 들어갈 때 운영에서 접근 가능한지 확인한다.
- [ ] 승인 완료 이미지를 `/data/deployed`로 복사하는 시점과 HTML URL 치환 시점을 정한다.
- [ ] 배포 HTML이 결재 전 저장소(`/data/uploads`)에 직접 의존하지 않도록 정책을 정한다.
- [ ] 기존 CMS Next `/api/deploy/push`에 있던 URL 치환 로직을 `spider-admin` 배포 흐름으로 옮길지 결정한다.
- [ ] 배포 후 브라우저 Network 탭에서 이미지 요청 URL과 HTTP 상태를 검증한다.

### 완료 조건

- 배포 HTML 안의 이미지가 운영 URL에서 `200`으로 열린다.
- 미승인 이미지 또는 결재 전 저장소 전용 URL이 배포 HTML에 남지 않는다.
- 이미지 승인 분리 이슈의 "승인된 이미지만 사용" 정책과 충돌하지 않는다.

## Issue 4. 배포 이력, 작업이력, 실패 메시지 검증

### 진행 시점

- Issue 1, 2로 배포 성공 경로가 열린 뒤 진행한다.
- Issue 9의 배포 관리 화면 마무리 단계에서 처리한다.

### 작업

- [ ] 배포 성공 시 `FWK_CMS_FILE_SEND_HIS`에 이력이 생성되는지 확인한다.
- [ ] 배포 실패 시 실패 사유가 이력 또는 로그에서 추적 가능한지 확인한다.
- [ ] 배포 작업이 `FWK_USER_ACCESS_HIS` 관리자 작업이력에 남는지 확인한다.
- [ ] receive URL 오류, secret 오류, 파일 저장 오류, HTML 서빙 오류를 구분해 표시할 수 있는지 검토한다.

### 완료 조건

- 운영자가 `spider-admin`에서 배포 성공/실패 이력을 확인할 수 있다.
- 배포 실패 시 원인을 재현 가능한 수준으로 추적할 수 있다.
- 관리자 배포 작업이 작업이력 로그에 남는다.

## Issue 5. 정적 리소스 백업/교체/rollback 운영 기준 확정

### 진행 시점

- Issue 2, 3으로 배포 HTML과 이미지 경로가 안정화된 뒤 진행한다.
- 운영 반영 전 또는 운영 배포 프로세스 정의 시점에 진행한다.

### 작업

- [ ] `/data/deployed`의 HTML 파일 교체 방식 결정
  - 덮어쓰기
  - 버전별 파일 생성
  - 임시 파일 생성 후 원자적 교체
- [ ] 배포 실패 시 이전 HTML 유지 여부 결정
- [ ] 승인 이미지 복사 실패 시 페이지 배포를 중단할지 결정
- [ ] 정적 리소스 삭제/보관 기간/정리 배치 책임 주체 결정
- [ ] rollback이 DB의 승인 이력만 되돌리는지, 정적 HTML/이미지도 함께 되돌리는지 결정

### 완료 조건

- 운영 배포 중 실패해도 기존 대고객 HTML과 이미지가 깨지지 않는다.
- rollback 시 DB 상태와 정적 파일 상태의 불일치 처리 기준이 문서화된다.

## 권장 진행 순서

1. Issue 1에서 receive URL과 secret 설정을 먼저 맞춘다.
2. Issue 2에서 배포 HTML 파일 생성 위치와 브라우저 URL을 확정한다.
3. Issue 9의 배포 push/history 화면 검증을 진행한다.
4. 이미지 승인 분리 이슈에서 승인 이미지 URL과 저장소 정책을 확정한다.
5. Issue 3에서 배포 HTML 내부 이미지 URL 치환/복사 정책을 구현한다.
6. Issue 4에서 배포 이력과 관리자 작업이력을 검증한다.
7. Issue 5에서 운영 백업/교체/rollback 기준을 확정한다.
8. Issue 10에서 제작자 요청부터 관리자 승인, 배포, 브라우저 접근까지 E2E로 묶어 검증한다.

## `cms-admin-migration-issues.md` 기준 진행 위치

- Issue 8은 정책 정리와 현재 코드/환경 불일치 확인까지로 완료한다.
- 이 문서의 Issue 1, 2, 4는 `cms-admin-migration-issues.md`의 Issue 9 배포 관리 이관 중 먼저 처리한다.
- 이 문서의 Issue 3은 `docs/cms-image-approval-issues.md`의 승인 이미지 선택/저장 정책이 확정된 뒤 처리한다.
- 이 문서의 Issue 5는 Issue 9 배포 관리 기능이 동작하고, 운영 배포 절차를 정리할 때 처리한다.
- 이 문서의 결과가 정리된 뒤 `cms-admin-migration-issues.md`의 Issue 10 로컬 프록시/E2E 시나리오를 최종 갱신한다.
