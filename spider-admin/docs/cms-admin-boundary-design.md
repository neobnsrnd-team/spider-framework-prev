# CMS 관리자 기능 경계와 URL/권한 설계

이 문서는 `docs/cms-admin-migration-issues.md`의 Issue 1 확정 산출물이다. 후속 이슈는 이 기준을 기본 계약으로 사용한다.

## 기본 원칙

- `spider-admin`은 로그인, 세션, 메뉴, 권한, 관리자/결재자 화면, 관리자 작업이력, 승인/반려, 배포/파일/통계 같은 운영 기능의 owner다.
- `cms-1-innova-next`는 페이지 제작자 dashboard(`/cms/dashboard`), ContentBuilder 편집 화면, 편집기 런타임, 페이지 생성/편집/승인 요청에 필요한 CMS 제작자 기능을 owner로 남긴다.
- `/cms` basePath는 `cms-1-innova-next`의 런타임 경계로 유지한다. `spider-admin`의 최종 CMS 관리자 화면은 `/cms/**`를 쓰지 않는다.
- 현재 전환 기간의 CMS 메뉴 클릭은 페이지 제작자 화면인 `/cms/dashboard`로 리다이렉트한다. 관리자/결재자는 `/cms/approve`가 아니라 `spider-admin`의 CMS 승인/운영 화면으로 진입한다.
- 이관 후 CMS 관리자/결재자 화면은 `spider-admin` 로그인과 `spider-admin` 라우트만으로 접근 가능해야 한다. `/cms/edit`은 CMS 제작자 dashboard에서 진입하는 CMS Next 내부 편집 화면이다.
- 관리자성 write 작업은 `spider-admin` API에서 수행하고, 기존 관리자 작업이력 기록 흐름에 남긴다.

## URL 정책

| 구분 | URL | Owner | 비고 |
| --- | --- | --- | --- |
| CMS 제작자 dashboard | `/cms/dashboard` | `cms-1-innova-next` | `cmsUser01` 같은 페이지 제작자가 페이지 생성, 편집, 승인 요청을 수행 |
| CMS 루트 전환 진입 | `/cms`, `/cms/` | `spider-admin` | 현재 CMS 메뉴 클릭 시 `/cms/dashboard`로 리다이렉트. 관리자/결재자 진입은 `spider-admin` 메뉴 사용 |
| CMS 편집 화면 | `/cms/edit` | `cms-1-innova-next` | ContentBuilder 런타임. `/cms/dashboard`에서 기존 CMS Next 라우팅으로 진입 |
| CMS Next API | `/cms/api/**` | `cms-1-innova-next` | 편집 load/save와 편집기 런타임 최소 API만 유지 |
| Java admin API | `/api/**` | `spider-admin` | 현재 사용자 조회는 `/api/auth/me`; CMS 관리 API는 후속 이슈에서 `/api/cms-admin/**` 아래로 추가 |
| 최종 CMS 관리자 화면 | `/cms-admin/**` | `spider-admin` | `/cms/**`와 충돌하지 않도록 별도 prefix 사용 |

최종 관리자 화면 URL은 아래처럼 고정한다.

| 기능 | 최종 `spider-admin` 화면 URL | 최종 `spider-admin` API prefix |
| --- | --- | --- |
| 관리자/결재자 승인 관리, 승인/반려/공개 상태/노출 기간/롤백 | `/cms-admin/approvals` | `/api/cms-admin/pages/{pageId}/approval` |
| 리소스 검토/참조 | `/cms-admin/files` | `/api/cms-admin/files` |
| A/B 관리 | `/cms-admin/ab-tests` | `/api/cms-admin/ab-tests` |
| 배포 이력/배포 push | `/cms-admin/deployments` | `/api/cms-admin/deployments` |
| 통계 | `/cms-admin/statistics` | `/api/cms-admin/statistics` |
| 시스템/컴포넌트 관리 | `/cms-admin/components` | `/api/cms-admin/components` |
| 관리자 작업이력 조회 | 기존 `/admin-histories` | 기존 admin history API와 mapper 재사용 |

전환 기간의 `/cms/dashboard`는 페이지 제작자 작업 화면으로 유지한다. 기존 `/cms/approve`, `/cms/files`, `/cms/ab` 같은 CMS 관리자성 화면은 원본 동작 확인용 또는 단계적 이관용 임시 경로이며, `spider-admin` 화면이 생기면 redirect, read-only, deprecated 처리 중 하나를 선택한다.

## 권한 정책

첫 이관 단계에서는 세부 권한을 새로 만들지 않고 기존 `spider-admin` 권한 모델에 아래 두 권한만 추가한다.

```text
CMS:R = CMS 읽기/접근
CMS:W = CMS 페이지 제작/편집/승인 요청
```

`menu-resource-permissions.yml`의 CMS 메뉴 매핑은 다음 정책을 따른다.

```yaml
v3_cms_manage:
  R: CMS:R
  W: CMS:W
v3_cms_dashboard:
  R: CMS:R
  W: CMS:W
v3_cms_edit:
  R: CMS:R
  W: CMS:W
v3_cms_admin_approvals:
  R: CMS:R
  W: CMS:W
v3_cms_admin_ab_tests:
  R: CMS:R
  W: CMS:W
v3_cms_admin_deployments:
  R: CMS:R
  W: CMS:W
v3_cms_admin_statistics:
  R: CMS:R
  W: CMS:W
v3_cms_admin_components:
  R: CMS:R
  W: CMS:W
```

권한 세분화는 후속 이슈에서 실제 화면과 API가 분리될 때 검토한다. 후보는 `CMS_APPROVAL:W`, `CMS_DEPLOY:W`, `CMS_FILE:W`, `CMS_AB:W`, `CMS_STATS:R`이지만 Issue 1 범위에서는 도입하지 않는다.

읽기 화면과 조회 API는 `CMS:R` 또는 `CMS:W`를 허용한다. `/cms/dashboard`의 페이지 생성, 편집, 승인 요청은 `CMS:W`를 요구한다. 관리자/결재자 화면의 승인, 반려, 롤백, 공개 상태 변경, 노출 기간 수정, 배포 push, A/B 승격은 `spider-admin`의 관리자 권한 정책을 따른다.

## DB 소유권

| DB 객체 | 이관 후 owner | 비고 |
| --- | --- | --- |
| `SPW_CMS_PAGE`, `SPW_CMS_PAGE_HISTORY` | 기능별 분리 | 제작자 페이지 생성/편집/승인 요청은 CMS Next, 관리자/결재자 승인/상태/이력/롤백 관리는 `spider-admin` |
| `SPW_CMS_PAGE_VIEW_LOG` | `spider-admin` | 통계 조회 API owner |
| `SPW_CMS_ASSET` | 보류 | Docker 정적 리소스 정책과 충돌 여부를 Issue 8에서 확정 |
| `FWK_CMS_FILE_SEND_HIS` | `spider-admin` | 배포 이력 관리 |
| `FWK_CMS_SERVER_INSTANCE` | `spider-admin` | 배포 대상 서버 관리 |
| 편집 저장용 페이지/컴포넌트 매핑 | 1단계 `cms-1-innova-next`, 2단계 검토 | `SPW_CMS_PAGE`와 페이지-컴포넌트 매핑 테이블 등 실제 객체명은 Issue 3에서 CMS 원본 SQL(`page.sql.ts`, `component-map.sql.ts`) 기준으로 확인 후 확정 |

관리자성 write API가 `spider-admin`으로 이관되면 `FWK_USER_ACCESS_HIS` 관리자 작업이력에 남아야 한다. 현재 `RequestTraceInterceptor`, `RdbAccessLogListener`, `AdminActionLogMapper.xml` 흐름을 재사용한다.

## 기능별 Owner와 구현 매핑

| 기능 | CMS 원본 참고 | 최종 `spider-admin` 구현 위치 | 권한 |
| --- | --- | --- | --- |
| 작업자 대시보드 | `src/app/[userId]/page.tsx`, `components/dashboard/DashboardClient.tsx` | CMS 유지: `/cms/dashboard` | 제작자 조회 `CMS:R`, 생성/편집/승인 요청 `CMS:W` |
| 승인 요청 모달 | `components/dashboard/ApprovalRequestModal.tsx` | CMS 유지: `/cms/dashboard` | `CMS:W` |
| 반려 사유 확인 | `components/dashboard/RejectedReasonModal.tsx` | CMS 유지: `/cms/dashboard` | `CMS:R` |
| 승인 관리 | `src/app/approve/page.tsx`, `components/approve/ApproveClient.tsx` | `domain.cms.approval`, `templates/pages/cms-approval/*` | `spider-admin` 관리자/결재자 권한 |
| 롤백 | `components/approve/RollbackModal.tsx` | `domain.cms.approval` | `spider-admin` 관리자/결재자 권한 |
| 통계 | `components/approve/StatsModal.tsx`, `app/api/track/stats/route.ts` | `domain.cms.statistics` | `CMS:R` |
| 리소스 검토/참조 | `src/app/files/page.tsx`, `components/files/FileBrowser.tsx` | 필요 시 `domain.cms.file`, `templates/pages/cms-file/*` | Docker volume 정책상 업로드/삭제/폴더 생성은 소유하지 않음. read-only 참조 화면이 필요할 때만 유지 |
| A/B 관리 | `src/app/ab/page.tsx`, `components/ab/AbTestClient.tsx` | `domain.cms.abtest`, `templates/pages/cms-ab-test/*` | `spider-admin` 관리자 권한 |
| 배포 | `app/api/deploy/push/route.ts`, `app/api/deploy/history/route.ts`, `db/repository/file-send.repository.ts` | `domain.cms.deployment` | `spider-admin` 관리자 권한 |
| 편집 load/save | `app/api/builder/load/route.ts`, `app/api/builder/save/route.ts` | 1단계 CMS 유지, 2단계 `domain.cms.editor` 검토 | 조회 `CMS:R`, 저장 `CMS:W` |
| 컴포넌트 카탈로그/매핑 | `db/repository/component.repository.ts`, `db/queries/component-map.sql.ts` | `domain.cms.component` | 조회 `CMS:R`, 변경 `CMS:W` |
| 관리자 작업이력 | 해당 없음 | 기존 `domain.adminhistory`, `global.log`, `AdminActionLogMapper.xml` | 기존 admin history 권한 |

## 현재 `spider-admin` 반영 상태

- `src/main/resources/menu-resource-permissions.yml`에 제작자용 `v3_cms_*` 메뉴와 관리자/결재자용 `v3_cms_admin_*` 메뉴 매핑이 있다. Issue 11에서 `v3_cms_admin_pages`, `v3_cms_system`, `v3_cms_approve`, `v3_cms_files`의 비활성/전환 방향을 정리한다.
- `src/main/java/com/example/admin_demo/global/page/controller/CmsRedirectController.java`가 `/cms`, `/cms/` 리다이렉트를 처리한다.
- `src/main/java/com/example/admin_demo/global/page/controller/PageController.java`가 `/cms-admin/**` CMS 관리 화면 골격 라우트를 처리한다.
- `src/main/java/com/example/admin_demo/global/auth/controller/AuthController.java`가 `/api/auth/me`에서 현재 사용자와 authority 목록을 반환한다.
- `src/main/resources/templates/fragments/sidebar.html`은 `/cms/**` 메뉴 클릭 시 내부 tab fragment가 아니라 브라우저 URL 이동을 사용한다.
- `docs/sql/**/03_insert_initial_data.sql`과 `e2e/docker/e2e-seed.sql`은 CMS 런타임 메뉴(`/cms/**`)와 `spider-admin` 관리 메뉴(`/cms-admin/**`) seed를 가진다.

## 후속 이슈 인터페이스

- Issue 2는 `docs/cms-edit-popup-contract.md`에서 `/cms/dashboard -> /cms/edit` 라우팅 책임 경계를 정의한다.
- Issue 3은 `docs/cms-data-api-ownership-strategy.md`에서 `/api/cms-admin/**` API owner와 DB 접근 전환 순서를 확정한다.
- Issue 4는 `/cms-admin/**` 화면 골격과 메뉴를 추가한다.
- Issue 5는 `/cms/dashboard -> /cms/edit` 기존 CMS Next 이동이 유지되는지 확인한다. `spider-admin` 관리자/결재자 화면은 `/cms/edit`로 이동시키지 않는다.
- Issue 6은 CMS 앱의 관리자 라우트를 redirect, read-only, deprecated 중 하나로 축소한다.
- Issue 11은 제작자용 CMS Next 메뉴와 관리자/결재자용 `spider-admin` 메뉴를 분리하고, `/cms-admin/pages`를 비활성화한다.
