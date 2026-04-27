# CMS 현황 개요

> 이 문서는 `docs/` 하위 CMS 관련 문서 8개의 핵심 내용을 현재 구현 기준으로 정리한 단일 참조 문서다.
> 상세 설계·이슈 추적은 각 원본 문서를 참조한다.

---

## 1. 시스템 구조

```
spider-admin (localhost:8080)
  - 로그인, 세션, 메뉴, 권한
  - CMS 관리자/결재자 화면 (/cms-admin/**)
  - 승인/반려/배포/이미지 관리/통계/A-B 테스트
  - 관리자 작업이력 (FWK_USER_ACCESS_HIS)

cms-1-innova-next (localhost:3000, basePath=/cms)
  - 페이지 제작자 dashboard (/cms/dashboard)
  - ContentBuilder 편집기 (/cms/edit)
  - 제작자 페이지 생성/편집/승인 요청

공통 DB (Oracle)
  - 두 서비스가 동일한 DB를 사용하므로 데이터 이관 없이 API owner만 전환
```

**로컬 통합 테스트 환경**

| 서비스 | URL |
| --- | --- |
| spider-admin | http://localhost:8080 |
| cms-1-innova-next | http://localhost:3000 |
| Nginx proxy (통합) | http://localhost:9000 |

```bash
# proxy 실행 (통합 테스트 시 필요)
docker compose --profile admin-proxy up -d admin-proxy
```

통합 테스트 시 브라우저는 `http://localhost:9000/login` 사용. `localhost:8080/login` 사용 시 `/cms/**` 라우팅이 proxy를 우회한다.

---

## 2. 역할·권한 정책

| 권한 | 대상 | 주요 허용 동작 |
| --- | --- | --- |
| `CMS:R` | 페이지 제작자 (예: `cmsUser01`) | 본인 페이지 생성/편집/저장/삭제/승인 요청, `/cms/dashboard` 접근 |
| `CMS:W` | 관리자/결재자 (예: `cmsAdmin01`, `userAdmin01`) | 전체 페이지 관리, 승인/반려/롤백/배포/이미지 관리, `/cms-admin/**` 접근 |

- `CMS:W`는 `CMS:R` 권한을 포함한다 (R+W).
- `CMS:R` 사용자는 본인이 생성한 페이지에만 접근할 수 있다.
- `PENDING` 상태 페이지는 재승인 요청 불가. 승인 요청 가능 상태: `WORK`, `REJECTED`, `APPROVED`.
- 권한 제어는 화면 버튼 + 서버 API 양쪽에서 모두 검증한다.

**권한 매핑 (`menu-resource-permissions.yml`)**

```yaml
v3_cms_manage / v3_cms_edit:
  R: CMS:R
  W: CMS:W

v3_cms_dashboard:
  R: CMS:R

v3_cms_admin_approvals / v3_cms_admin_asset_requests / v3_cms_admin_asset_approvals /
v3_cms_admin_deployments / v3_cms_admin_statistics / v3_cms_admin_ab_tests / v3_cms_admin_components:
  W: CMS:W
```

**사용자별 메뉴 권한**

| 사용자 유형 | 메뉴 권한 |
| --- | --- |
| `cmsUser01` (제작자) | `v3_cms_manage W`, `v3_cms_dashboard R`, `v3_cms_edit W` |
| `cmsAdmin01` (관리자) | `v3_cms_manage W`, `v3_cms_admin_approvals W`, `v3_cms_admin_deployments W`, `v3_cms_admin_statistics W`, `v3_cms_admin_ab_tests W`, `v3_cms_admin_components W` |

---

## 3. 화면 URL 현황

### spider-admin 화면 (구현 완료)

| URL | 화면 | 비고 |
| --- | --- | --- |
| `/cms-admin/approvals` | 페이지 승인 관리 | 목록/검색/승인/반려/롤백/공개상태/노출기간 |
| `/cms-admin/asset-approvals` | 이미지 승인 관리 | 이미지 목록/승인/반려/노출여부 |
| `/cms-admin/asset-requests` | 제작자 이미지 업로드 요청 | 이미지 업로드 요청 및 관리자 직접 업로드 |
| `/cms-admin/deployments` | 배포 관리 | 배포 대상 조회/배포 실행/이력 조회 |
| `/cms-admin/ab-tests` | A/B 테스트 관리 | 그룹 관리/가중치/승격/삭제 |
| `/cms-admin/statistics` | 통계 | 조회/클릭 통계 |
| `/cms-admin/components` | 컴포넌트 관리 | 스켈레톤 |
| `/cms-admin/pages` | — | `/cms-admin/approvals`로 redirect |
| `/cms-admin/files` | — | `/cms-admin/approvals`로 redirect |

### cms-1-innova-next 화면 (유지)

| URL | 화면 |
| --- | --- |
| `/cms/edit` | ContentBuilder 편집기 |

### spider-admin 제작자 화면

| URL | 화면 | 비고 |
| --- | --- | --- |
| `/cms/dashboard` | 페이지 제작자 dashboard | spider-admin Thymeleaf 구현 (`pages/cms-dashboard/`). `/api/cms-dashboard/*` API 사용 |

### 라우팅 규칙

```
/cms, /cms/ 진입 시:
  - 제작자 계정 → /cms/dashboard
  - 관리자/결재자 계정 → /cms-admin/approvals
```

---

## 4. API 목록 (spider-admin 구현 완료)

### 페이지 승인 관리 (`domain.cmsapproval`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-admin/approvals` | `CMS:W` | 승인 대기 목록 |
| POST | `/api/cms-admin/pages/{pageId}/approval/approve` | `CMS:W` | 승인 |
| POST | `/api/cms-admin/pages/{pageId}/approval/reject` | `CMS:W` | 반려 |
| GET | `/api/cms-admin/pages/{pageId}/approval-history` | `CMS:W` | 승인 이력 |
| POST | `/api/cms-admin/pages/{pageId}/rollback` | `CMS:W` | 롤백 |
| PATCH | `/api/cms-admin/pages/{pageId}/public-state` | `CMS:W` | 공개 상태 변경 |
| PATCH | `/api/cms-admin/pages/{pageId}/display-period` | `CMS:W` | 노출 기간 수정 |

### 이미지 승인 관리 (`domain.cmsasset`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-admin/asset-approvals` | `CMS:R` | 이미지 목록 |
| GET | `/api/cms-admin/asset-approvals/{assetId}` | `CMS:R` | 이미지 상세 |
| GET | `/api/cms-admin/asset-approvals/{assetId}/image` | `CMS:R` | 이미지 파일 조회 |
| POST | `/api/cms-admin/asset-approvals/{assetId}/approve` | `CMS:W` | 이미지 승인 |
| POST | `/api/cms-admin/asset-approvals/{assetId}/reject` | `CMS:W` | 이미지 반려 |
| POST | `/api/cms-admin/asset-approvals/{assetId}/visibility` | `CMS:W` | 노출 여부 변경 |
| POST | `/api/cms-admin/asset-approvals/upload` | `CMS:W` | 관리자 직접 업로드 (즉시 APPROVED 처리, `CmsAssetApprovalController`) |
| GET | `/api/cms-admin/asset-requests` | `CMS:R` | 제작자 업로드 요청 목록 |
| POST | `/api/cms-admin/asset-requests/{assetId}/request` | `CMS:W` | 승인 요청 |
| POST | `/api/cms-admin/asset-uploads` | `CMS:W` | 현업 관리자 업로드 (WORK 상태로 CMS 포워딩, `CmsAssetUploadController`) |

> **이미지 업로드 인증**: Admin 백엔드 → CMS(`/cms/api/builder/upload`)로 포워딩 시 `x-deploy-token` 헤더 사용 (Issue #177). `cms.builder.deploy-secret`과 CMS의 `DEPLOY_SECRET`이 일치해야 한다.
>
> **주의**: Admin `.env`의 `CMS_USER_URL`이 `:8080`(운영 서버, `SERVER_MODE=operation`)으로 설정된 경우 업로드가 차단된다. `CmsBuilderClient`의 업로드 대상 URL은 반드시 CMS 관리자 서버(포트 80, `basePath=/cms`)를 가리켜야 한다.

### 배포 관리 (`domain.cmsdeployment`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-admin/deployments/pages` | `CMS:R` | 배포 대상 페이지 목록 |
| GET | `/api/cms-admin/deployments` | `CMS:R` | 배포 이력 |
| POST | `/api/cms-admin/deployments/push` | `CMS:W` | 배포 실행 |
| POST | `/api/cms-admin/deployments/push-expired` | `CMS:W` | 만료 페이지 배포 |

### A/B 테스트 (`domain.cmsabtest`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-admin/ab-tests` | `CMS:R` | 그룹 목록 |
| GET | `/api/cms-admin/ab-tests/{groupId}` | `CMS:R` | 그룹 상세 |
| POST | `/api/cms-admin/ab-tests` | `CMS:W` | 그룹 생성 |
| PATCH | `/api/cms-admin/ab-tests/{groupId}/weights` | `CMS:W` | 가중치 수정 |
| POST | `/api/cms-admin/ab-tests/{groupId}/promote` | `CMS:W` | 승격 |
| DELETE | `/api/cms-admin/ab-tests?groupId=` | `CMS:W` | 그룹 삭제 |
| DELETE | `/api/cms-admin/ab-tests?pageId=` | `CMS:W` | 페이지 A/B 해제 |

### 통계 (`domain.cmsstatistics`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-admin/statistics` | `CMS:R` | 통계 목록 |
| GET | `/api/cms-admin/statistics/detail` | `CMS:R` | 통계 상세 |

### 제작자 Dashboard (`domain.cmsdashboard`)

| Method | URL | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/cms-dashboard/templates` | `CMS:R` | 템플릿 목록 |
| GET | `/api/cms-dashboard/pages` | `CMS:R` | 본인 페이지 목록 |
| POST | `/api/cms-dashboard/pages` | `CMS:R` | 페이지 생성 |
| DELETE | `/api/cms-dashboard/pages/{pageId}` | `CMS:R` | 페이지 삭제 |
| PATCH | `/api/cms-dashboard/pages/{pageId}/approve-request` | `CMS:R` | 승인 요청 |

---

## 5. DB 소유권 현황

| 테이블 | Write Owner | 비고 |
| --- | --- | --- |
| `SPW_CMS_PAGE` | 기능별 분리 | 생성/편집/승인 요청: CMS. 승인/반려/공개상태/노출기간/롤백: spider-admin |
| `SPW_CMS_PAGE_HISTORY` | 기능별 분리 | 승인 처리(APPROVED 전환 시 이력 생성): CMS. 승인 이력 조회·롤백: spider-admin |
| `SPW_CMS_ASSET` | 기능별 분리 | 파일 저장 + INSERT: CMS Builder. 승인/반려/노출상태 변경: spider-admin |
| `SPW_CMS_PAGE_VIEW_LOG` | tracker/runtime (write), spider-admin (read) | 통계 조회는 spider-admin |
| `FWK_CMS_FILE_SEND_HIS` | spider-admin | 배포 이력 |
| `FWK_CMS_SERVER_INSTANCE` | spider-admin | 배포 대상 서버 |

---

## 6. 관리자 작업이력 기록 대상

다음 spider-admin write API는 `FWK_USER_ACCESS_HIS`에 자동 기록된다 (`RequestTraceInterceptor` → `RdbAccessLogListener` 흐름).

- 페이지 승인 / 반려 / 공개 상태 변경 / 노출 기간 수정 / 롤백
- 이미지 승인 / 반려 / 노출 여부 변경 / 관리자 직접 업로드
- 배포 push
- A/B 그룹 생성 / 가중치 수정 / 승격 / 삭제

---

## 7. 구현 완료 항목

| 항목 | 관련 이슈/PR |
| --- | --- |
| 기능 경계·URL·권한 설계 | migration Issue 1 |
| CMS 편집 라우팅 책임 경계 확정 | migration Issue 2 |
| 데이터/API 소유권 전략 | migration Issue 3 |
| spider-admin CMS 관리 화면 골격 | migration Issue 4 |
| `/cms/dashboard → /cms/edit` 기존 라우팅 유지 확인 | migration Issue 5 |
| 페이지 승인 관리 (목록/승인/반려/롤백/공개상태/노출기간) | migration Issue 7 |
| CMS 메뉴·권한 분리 | migration Issue 11 |
| 승인 요청 시 노출 시작일/종료일 설정 | migration Issue 12 |
| 이미지 업로드 요청·승인·반려·관리자 즉시 업로드 | Issue #65, #88, #55 |
| 이미지 업로드 x-deploy-token 서버 간 인증 | Issue #177 |
| 배포 관리 (push/history/push-expired) | Issue 9 일부 |
| A/B 테스트 관리 | Issue 9 일부 |
| 통계 조회 | Issue 9 일부 |

---

## 8. 미완료·후속 이슈

| 항목 | 문서 | 비고 |
| --- | --- | --- |
| CMS 앱 관리자성 화면/API 정리 (deprecate/redirect) | migration Issue 6 | html-cms `/api/manage/*`, `/api/assets/*` deprecate 마킹 미수행 |
| 배포 이력 실패 메시지 저장 | `cms-deploy-serving-followup-issues.md` Issue 4 | `FWK_CMS_FILE_SEND_HIS`에 실패 메시지 컬럼 없음 |
| 정적 리소스 rollback 운영 기준 | `cms-deploy-serving-followup-issues.md` Issue 5 | 버전·백업은 구현됨. 자동 rollback API 미구현 |
| 기존 CMS `/api/manage/*`, `/api/assets/*` 처리 방향 | `cms-image-approval-issues.md` Issue 4 | redirect/deprecate 없이 API 유지 중 |
| 로컬 proxy·E2E 시나리오 갱신 | migration Issue 10 | E2E 테스트는 있으나 nginx proxy 설정 미발견 |

---

## 9. 참고 문서

| 문서 | 내용 |
| --- | --- |
| `cms-admin-boundary-design.md` | 기능 경계·URL 정책·권한·DB 소유권 확정 산출물 (migration Issue 1) |
| `cms-admin-migration-issues.md` | Issue 1~12 전체 이관 이슈 추적 |
| `cms-data-api-ownership-strategy.md` | API owner·호출 방향·중복 write 방지·rollback 기준 (migration Issue 3) |
| `cms-edit-popup-contract.md` | CMS 편집 라우팅 책임 경계 (migration Issue 2) |
| `cms-integration-guide.md` | 로컬 통합 환경 설정·권한 모델·CMS 설정 가이드 |
| `cms-role-policy.md` | CMS:R / CMS:W 권한별 허용 동작·페이지 소유권·승인 상태 정책 |
| `cms-deploy-serving-followup-issues.md` | 배포 receive URL·서빙 경로·이미지 URL 후속 이슈 |
| `cms-image-approval-issues.md` | 이미지 승인 분리 이슈 (제작자 요청·편집기 분리·API 처리 방향) |
