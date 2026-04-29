# CMS 관리자 기능 이관 이슈 초안

## 목표

CMS의 관리자/결재자 화면 기능은 `spider-admin`으로 옮기고, `cms-1-innova-next`는 페이지 제작자 dashboard(`/cms/dashboard`)와 ContentBuilder 편집 기능을 제공한다. CMS 앱은 제거 대상이 아니라 페이지 제작자 작업과 ContentBuilder 편집기 런타임으로 축소한다.

현재 기준:

- `spider-admin`은 로그인, 세션, 메뉴, 권한의 기준 시스템이다.
- `cms-1-innova-next`는 `/cms` basePath 아래에서 동작하며 `/api/auth/me`를 통해 현재 사용자와 `CMS:R`, `CMS:W` 권한을 확인한다.
- `cmsUser01` 같은 페이지 제작자는 `/cms/dashboard`에서 페이지 생성, 편집, 승인 요청을 수행한다.
- `userAdmin01` 같은 관리자/결재자는 `spider-admin`에서 승인, 반려, 결재, 운영 관리 작업을 수행한다.
- `spider-admin`과 `cms-1-innova-next`는 정적 이미지/HTML 리소스를 제외하고 같은 Oracle DB를 참조한다.
- 로컬 통합은 `spider-admin:8080`, `cms-1-innova-next:3000`, `nginx proxy:9000` 구조를 사용한다.

이관 이유:

- CMS 관리 기능을 `spider-admin`에 통합해 기존 관리자 콘솔의 기능을 CMS에도 계속 붙일 수 있게 한다.
- 예를 들어 메뉴/권한, 관리자 작업이력 로그, 배포/파일/통계 같은 기존 `spider-admin` 기능을 CMS 관리 기능에서 재사용할 수 있어야 한다.
- 승인/반려 같은 관리자/결재자 작업은 `spider-admin` 백엔드가 소유하고 직접 처리한다. 기존 CMS Next에는 별도 결재/워크플로 API 호출 로직이 없었으므로, 별도 결재/워크플로 API 연동은 이번 이관 범위에서 제외한다. 제작자의 승인 요청은 `/cms/dashboard` 흐름에서 수행한다.
- 페이지 제작자 dashboard와 ContentBuilder가 필요한 실제 편집 화면은 CMS 앱에 남기되, 관리자/결재자 화면/운영 기능/감사 로그 책임은 `spider-admin`으로 모은다.

역할 분리:

- `spider-admin`: 로그인, 권한, 메뉴, 관리자/결재자 화면, 작업이력 로그, 승인/반려, 배포/파일/통계 같은 운영 기능
- `cms-1-innova-next`: 페이지 제작자 dashboard(`/cms/dashboard`), ContentBuilder 편집 화면, 편집기 런타임, 페이지 생성/편집/승인 요청에 필요한 기능

## 구현 참고 코드 맵

이 문서의 각 이슈를 구현할 때 아래 CMS 코드를 기준으로 기능을 옮긴다. 이슈 본문에 별도 지시가 없으면, 기존 동작은 CMS 코드를 기준으로 유지하고 화면/권한/API owner만 `spider-admin` 방식에 맞게 바꾼다.

### CMS 제작자/관리자 화면 원본

```text
cms-1-innova-next/src/app/[userId]/page.tsx                         -> 작업자 대시보드 서버 컴포넌트
cms-1-innova-next/src/components/dashboard/DashboardClient.tsx       -> 작업자 대시보드 UI, 페이지 생성/삭제/승인 요청
cms-1-innova-next/src/components/dashboard/ApprovalRequestModal.tsx  -> 승인 요청 모달, 결재자 목록 조회
cms-1-innova-next/src/components/dashboard/RejectedReasonModal.tsx   -> 반려 사유 확인 모달

cms-1-innova-next/src/app/approve/page.tsx                          -> 관리자 승인 관리 서버 컴포넌트
cms-1-innova-next/src/components/approve/ApproveClient.tsx           -> 승인/반려/배포/공개 상태/노출 기간 관리 UI
cms-1-innova-next/src/components/approve/RollbackModal.tsx           -> 승인 이력 롤백 UI
cms-1-innova-next/src/components/approve/StatsModal.tsx              -> 통계 모달

cms-1-innova-next/src/app/files/page.tsx                             -> 기존 파일 관리 화면. 정적 리소스 Docker 관리로 전환 시 제거/비활성 후보
cms-1-innova-next/src/components/files/FileBrowser.tsx               -> 기존 파일/폴더 관리 UI. 정적 리소스 Docker 관리로 전환 시 참고만 하고 이관하지 않음
cms-1-innova-next/src/components/files/*                             -> 기존 파일 카드, 폴더 트리, 업로드 진행, 삭제/폴더 생성 모달. 이관 대상 아님

cms-1-innova-next/src/app/ab/page.tsx                                -> A/B 관리 서버 컴포넌트
cms-1-innova-next/src/components/ab/AbTestClient.tsx                 -> A/B 그룹/가중치/승격 UI
```

### CMS API 원본

```text
cms-1-innova-next/src/app/api/builder/pages/route.ts                         -> 페이지 목록/삭제
cms-1-innova-next/src/app/api/builder/pages/[pageId]/approve-request/route.ts -> 승인 요청
cms-1-innova-next/src/app/api/builder/pages/[pageId]/approve/route.ts         -> 승인 확정
cms-1-innova-next/src/app/api/builder/pages/[pageId]/reject/route.ts          -> 반려
cms-1-innova-next/src/app/api/builder/pages/[pageId]/set-public/route.ts      -> 공개 상태 변경
cms-1-innova-next/src/app/api/builder/pages/[pageId]/update-dates/route.ts    -> 노출 기간 수정
cms-1-innova-next/src/app/api/builder/pages/[pageId]/history/route.ts         -> 승인 이력 조회
cms-1-innova-next/src/app/api/builder/pages/[pageId]/rollback/route.ts        -> 롤백
cms-1-innova-next/src/app/api/builder/save/route.ts                           -> 편집 저장
cms-1-innova-next/src/app/api/builder/load/route.ts                           -> 편집 로드
cms-1-innova-next/src/app/api/auth/approvers/route.ts                         -> 결재자 목록 proxy

cms-1-innova-next/src/app/api/manage/folders/route.ts       -> 기존 폴더 트리 조회. Docker 정적 리소스 관리 전환 시 제거/비활성 후보
cms-1-innova-next/src/app/api/manage/files/route.ts         -> 기존 파일 목록. Docker 정적 리소스 관리 전환 시 제거/비활성 후보
cms-1-innova-next/src/app/api/manage/upload/route.ts        -> 기존 파일 업로드. Docker 정적 리소스 관리 전환 시 제거/비활성 후보
cms-1-innova-next/src/app/api/manage/delete/route.ts        -> 기존 파일/폴더 삭제. Docker 정적 리소스 관리 전환 시 제거/비활성 후보
cms-1-innova-next/src/app/api/manage/addfolder/route.ts     -> 기존 폴더 생성. Docker 정적 리소스 관리 전환 시 제거/비활성 후보
cms-1-innova-next/src/app/api/assets/route.ts               -> 기존 DB 기반 에셋 목록/등록. 정적 리소스 정책과 충돌 여부 확인
cms-1-innova-next/src/app/api/assets/[assetId]/route.ts     -> 기존 DB 기반 에셋 수정/삭제. 정적 리소스 정책과 충돌 여부 확인
cms-1-innova-next/src/app/api/assets/[assetId]/image/route.ts -> 기존 DB 기반 에셋 이미지 응답. 정적 리소스 정책과 충돌 여부 확인

cms-1-innova-next/src/app/api/builder/ab/route.ts           -> A/B 그룹 관리
cms-1-innova-next/src/app/api/builder/ab/promote/route.ts   -> A/B 승격
cms-1-innova-next/src/app/api/track/stats/route.ts          -> 조회/클릭 통계
cms-1-innova-next/src/app/api/deploy/push/route.ts          -> 배포 push
cms-1-innova-next/src/app/api/deploy/history/route.ts       -> 배포 이력
cms-1-innova-next/src/app/api/deploy/receive/route.ts       -> 배포 receive
```

### CMS DB/공통 로직 원본

```text
cms-1-innova-next/src/db/repository/page.repository.ts       -> 페이지/승인/반려/이력/롤백/기간/공개 상태 핵심 로직
cms-1-innova-next/src/db/repository/asset.repository.ts      -> 기존 DB 기반 에셋 관리 로직. Docker 정적 리소스 정책과 충돌 여부 확인
cms-1-innova-next/src/db/repository/file-send.repository.ts  -> 배포 이력/서버 인스턴스 로직
cms-1-innova-next/src/db/repository/page-view-log.repository.ts -> 조회/클릭 통계 로직
cms-1-innova-next/src/db/repository/component.repository.ts  -> 컴포넌트 카탈로그/매핑 로직

cms-1-innova-next/src/db/queries/page.sql.ts                 -> SPW_CMS_PAGE, SPW_CMS_PAGE_HISTORY SQL
cms-1-innova-next/src/db/queries/page-history.sql.ts         -> 승인 이력 SQL
cms-1-innova-next/src/db/queries/component-map.sql.ts        -> 페이지-컴포넌트 매핑 SQL
cms-1-innova-next/src/db/queries/asset.sql.ts                -> SPW_CMS_ASSET SQL
cms-1-innova-next/src/db/queries/file-send.sql.ts            -> FWK_CMS_FILE_SEND_HIS SQL
cms-1-innova-next/src/db/queries/server.sql.ts               -> FWK_CMS_SERVER_INSTANCE SQL
cms-1-innova-next/src/db/queries/page-view-log.sql.ts        -> SPW_CMS_PAGE_VIEW_LOG SQL

cms-1-innova-next/src/lib/current-user.ts                    -> CMS 권한 판단
cms-1-innova-next/src/lib/java-admin-api.ts                  -> spider-admin API 호출/쿠키 전달
cms-1-innova-next/src/lib/api-url.ts                         -> Next API/Java API 경로 분리
```

### spider-admin 구현 패턴 참고

```text
spider-admin/src/main/java/com/example/admin_demo/global/auth/controller/AuthController.java
  -> /api/auth/me 구현 참고

spider-admin/src/main/java/com/example/admin_demo/global/page/controller/CmsRedirectController.java
  -> /cms 루트 리다이렉트 참고

spider-admin/src/main/java/com/example/admin_demo/global/page/controller/PageController.java
  -> Thymeleaf 페이지 라우팅 추가 패턴 참고

spider-admin/src/main/resources/templates/pages/*/*
  -> 기존 관리자 화면 템플릿/스크립트 구조 참고

spider-admin/src/main/java/com/example/admin_demo/global/security/*
spider-admin/src/main/resources/menu-resource-permissions.yml
spider-admin/src/main/resources/mapper/oracle/security/AuthorityMapper.xml
  -> 권한 로딩/권한 매핑 참고

spider-admin/src/main/java/com/example/admin_demo/global/log/RequestTraceInterceptor.java
spider-admin/src/main/java/com/example/admin_demo/global/log/listener/RdbAccessLogListener.java
spider-admin/src/main/resources/mapper/oracle/adminhistory/AdminActionLogMapper.xml
spider-admin/src/main/java/com/example/admin_demo/domain/adminhistory/*
  -> 관리자 작업이력 로그 기록/조회 참고
```

---

## 공통 선행 이슈

### Issue 1. [CMS 이관] 관리자 기능 경계와 URL/권한 설계 확정

#### 선행 이슈

- 없음

#### 배경

CMS의 관리자 화면 기능을 `spider-admin`으로 옮기고, CMS 앱에는 편집기만 남기려면 기능별 owner와 URL 정책을 먼저 고정해야 한다. 이 이관은 단순한 화면 이동이 아니라 CMS 관리 기능이 기존 `spider-admin`의 메뉴/권한/작업이력/결재/운영 기능을 재사용할 수 있게 만드는 통합 작업이다.

#### 작업

- [x] `spider-admin`이 소유할 관리자/결재자 기능 목록 확정
  - 승인 관리
  - 관리자/결재자 승인/반려 API
  - 리소스 검토/참조
  - A/B 관리
  - 배포 이력
  - 통계
  - 시스템/컴포넌트 관리
  - 관리자 작업이력 로그 기록
- [x] `cms-1-innova-next`에 남길 기능 확정
  - 페이지 제작자 dashboard(`/cms/dashboard`)
  - 페이지 생성/편집/승인 요청
  - ContentBuilder 편집 화면
  - 편집 저장 API
  - 편집기 런타임에 필요한 최소 API
- [x] 최종 URL 정책 확정
  - admin 내부 CMS 관리 화면 URL
  - CMS 제작자 dashboard와 편집 화면 URL
  - `/cms` basePath 유지 여부
- [x] proxy 사용 범위 확정
  - 관리자 화면은 `spider-admin` 직접 로그인으로 접근 가능하게 할지 결정
  - CMS 제작자 dashboard와 편집 화면은 기존 CMS Next 라우팅/세션 흐름을 유지할지 결정
- [x] 권한 정책 확정
  - `CMS:R`
  - `CMS:W`
  - 승인/배포/리소스 참조 세부 권한 분리 여부
- [x] DB 소유권 확정
  - `SPW_CMS_*`
  - `FWK_CMS_*`
- [x] 위 "구현 참고 코드 맵"을 기준으로 기능별 이관 원본 파일을 확정한다.
- [x] `spider-admin` 구현 시 참고할 기존 admin 도메인/화면 패턴을 확정한다.

#### 확정 산출물

- `docs/cms-admin-boundary-design.md`에 기능별 owner, 최종 URL, proxy 범위, 권한 정책, DB 소유권, "CMS 원본 파일 -> spider-admin 구현 위치" 매핑을 확정했다.
- 첫 단계 권한은 `CMS:R`, `CMS:W`만 사용하고 승인/배포/파일/A-B/통계 세부 권한은 후속 이슈의 실제 화면/API 분리 시점까지 도입하지 않는다.
- `/cms` basePath는 CMS 편집 런타임 경계로 유지하고, 최종 `spider-admin` 관리자 화면은 `/cms-admin/**` prefix를 사용한다.

#### 완료 조건

- 기능별 owner와 URL이 문서화된다.
- CMS 앱은 페이지 제작자 dashboard와 편집기 런타임으로 남기고, 관리자/결재자 기능은 `spider-admin`으로 옮기는 기준이 문서화된다.
- CMS 관리 기능이 기존 `spider-admin` 공통 기능을 어떤 방식으로 재사용할지 기준이 문서화된다.
- 각 기능별로 "참고 CMS 원본 파일 -> 구현할 spider-admin 파일/패키지" 매핑이 정리된다.
- 이후 작업들이 서로 독립적으로 진행 가능한 수준으로 인터페이스가 고정된다.

---

### Issue 2. [CMS 이관] CMS 편집 라우팅 책임 정리

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정

#### 참고 코드

```text
CMS:
- src/app/edit/page.tsx
- src/components/edit/EditClientLoader.tsx
- src/components/edit/EditClient.tsx
- src/app/api/builder/load/route.ts
- src/app/api/builder/save/route.ts
- src/lib/api-url.ts
- src/lib/current-user.ts
- src/lib/java-admin-api.ts

spider-admin:
- src/main/resources/templates/fragments/sidebar.html
- src/main/java/com/example/admin_demo/global/page/controller/PageController.java
- src/main/java/com/example/admin_demo/global/security/SecurityConfig.java
```

#### 배경

페이지 제작자는 `/cms/dashboard`에서 기존 CMS 라우팅 방식으로 `/cms/edit`에 진입한다. 관리자는 페이지를 생성하거나 수정하지 않고, 제작자가 만든 페이지를 검토하고 승인/반려/운영 관리만 수행한다. 따라서 `spider-admin`에서 `/cms/edit`로 이동시키는 흐름은 현재 범위에서 구현하지 않는다.

#### 작업

- [x] `/cms/dashboard -> /cms/edit` 이동은 기존 CMS Next 라우팅 방식으로 유지한다고 명시
- [x] `spider-admin` 관리자/결재자 화면은 `/cms/edit`로 이동시키지 않는다고 명시
- [x] 관리자는 페이지 생성/수정이 아니라 승인/반려/검토/운영 관리만 수행한다고 명시
- [x] ContentBuilder 편집기는 CMS Next 책임으로 유지한다고 명시
- [x] CMS 편집 화면이 승인/반려/배포 API를 직접 호출하지 않도록 책임 경계를 명시

#### 확정 산출물

- `docs/cms-edit-popup-contract.md`는 파일명과 무관하게 CMS 편집 라우팅/책임 경계 문서로 정리한다.
- `/cms/dashboard -> /cms/edit` 이동은 기존 CMS Next 구현을 유지한다.
- `spider-admin` 관리자/결재자 화면은 `/cms/edit`로 이동시키지 않는다.
- CMS 편집 화면은 편집 load/save와 편집기 런타임 API만 호출한다. 제작자 승인 요청은 `/cms/dashboard` 흐름에서 처리하고, 관리자/결재자의 승인/반려/배포/작업이력 기록은 `spider-admin` API 책임으로 고정했다.

#### 완료 조건

- `/cms/dashboard`와 `/cms/edit`의 CMS 내부 라우팅 책임이 문서화된다.
- ContentBuilder 편집기는 CMS에 남기되, 관리자/결재자 작업은 `spider-admin` API로만 수행한다는 계약이 문서화된다.
- `spider-admin` 승인/검토 화면이 편집 기능을 소유하지 않는다는 기준이 문서화된다.

---

### Issue 3. [CMS 이관] 데이터/API 소유권 및 전환 전략 정리

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정

#### 참고 코드

```text
CMS API:
- src/app/api/builder/pages/route.ts
- src/app/api/builder/pages/[pageId]/approve-request/route.ts
- src/app/api/builder/pages/[pageId]/approve/route.ts
- src/app/api/builder/pages/[pageId]/reject/route.ts
- src/app/api/builder/pages/[pageId]/set-public/route.ts
- src/app/api/builder/pages/[pageId]/update-dates/route.ts
- src/app/api/builder/pages/[pageId]/history/route.ts
- src/app/api/builder/pages/[pageId]/rollback/route.ts
- src/app/api/builder/save/route.ts
- src/app/api/builder/load/route.ts
- src/app/api/deploy/push/route.ts
- src/app/api/deploy/history/route.ts
- src/app/api/deploy/receive/route.ts

CMS DB:
- src/db/repository/page.repository.ts
- src/db/repository/asset.repository.ts
- src/db/repository/file-send.repository.ts
- src/db/repository/page-view-log.repository.ts
- src/db/queries/page.sql.ts
- src/db/queries/page-history.sql.ts
- src/db/queries/asset.sql.ts
- src/db/queries/file-send.sql.ts
- src/db/queries/page-view-log.sql.ts

spider-admin:
- src/main/java/com/example/admin_demo/global/log/RequestTraceInterceptor.java
- src/main/java/com/example/admin_demo/global/log/listener/RdbAccessLogListener.java
- src/main/resources/mapper/oracle/adminhistory/AdminActionLogMapper.xml
- src/main/java/com/example/admin_demo/domain/*/controller
- src/main/java/com/example/admin_demo/domain/*/service
- src/main/resources/mapper/oracle/*/*.xml
```

#### 배경

현재 CMS는 Next API와 Oracle repository에서 페이지/승인/파일/배포 관련 데이터를 직접 다룬다. `spider-admin`과 `cms-1-innova-next`는 정적 이미지/HTML 리소스를 제외하고 같은 Oracle DB를 참조하므로 데이터 복제/이동보다는 API owner, mapper/service 구현 위치, 중복 write 차단 기준을 먼저 정해야 한다.

#### 작업

- [x] 페이지 목록/상세/생성/삭제 API owner 결정
- [x] 승인 요청/승인/반려/공개 상태/노출 기간 API owner 결정
- [x] 관리자/결재자 승인/반려 API는 `spider-admin` 소유로 전환
- [x] 제작자 승인 요청은 `/cms/dashboard`에 유지하고, 별도 결재/워크플로 API 중계는 이번 이관 범위에서 제외
- [x] CMS Next API의 `approve-request`, `approve`, `reject` 직접 DB 변경 경로 제거 또는 deprecated 처리
- [x] 이미지와 페이지 HTML 같은 정적 리소스는 서버 Docker 정적 리소스로 관리한다는 전제를 API owner 표에 반영
- [x] 기존 CMS 파일/에셋 관리 API를 이관할지 여부가 아니라 제거/비활성/참조 전용 유지 여부를 결정
- [x] A/B 및 통계 API owner 결정
- [x] 배포 push/receive/history API owner 결정
- [x] 편집 저장 API owner 단계 결정
  - 1단계: CMS 저장 API 유지, 저장 완료 이벤트를 admin에 전달
  - 2단계: 필요 시 저장 API도 `spider-admin`으로 이관
- [x] 임시 공존 기간의 호출 방향 정의
  - admin -> CMS API
  - CMS -> admin API
  - admin 직접 DB 접근
- [x] 관리자 작업이력 로그 기록 기준 정의
  - 관리자/결재자의 승인/반려는 `FWK_USER_ACCESS_HIS`에 남기는 것을 필수로 한다.
  - 편집 저장은 1단계에서 별도 callback 로그를 남길지, 2단계에서 admin 저장 API로 옮길지 결정한다.
- [x] 마이그레이션 순서와 rollback 기준 정리

#### 확정 산출물

- `docs/cms-data-api-ownership-strategy.md`에 기능별 API owner, 호출 방향, DB 소유권, 작업이력 기록 기준, 중복 write 방지 단계, rollback 기준을 확정했다.
- `spider-admin`과 `cms-1-innova-next`는 같은 Oracle DB를 사용하므로 정적 리소스를 제외한 업무 데이터 복제/이동은 원칙적으로 필요 없고, API owner와 mapper/service 코드를 `spider-admin` 방식으로 전환하는 작업이 핵심이라고 명시했다.
- 단순 코드 이동이 아니라 CMS TypeScript SQL을 Java/MyBatis mapper로 재작성하고, 트랜잭션/권한/작업이력/중복 write 차단을 반영해야 한다는 기준을 확정했다.
- 편집 load/save는 1단계 CMS Next API 유지, 2단계 admin API 이관 검토로 분리했다.

#### 완료 조건

- 기능별 API owner와 호출 방향이 표로 정리된다.
- 관리자/결재자의 승인/반려가 `spider-admin` 작업이력 로그에 남는 구조가 확정된다.
- 관리자/결재자의 승인/반려 API owner가 `spider-admin` 백엔드로 확정된다.
- 중복 write 경로가 없도록 전환 단계가 정의된다.

---

## 이후 독립 진행 가능 이슈

### Issue 4. [CMS 이관] spider-admin에 CMS 관리 메뉴/화면 골격 추가

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정

#### 참고 코드

```text
CMS 화면 원본:
- src/app/[userId]/page.tsx
- src/components/dashboard/DashboardClient.tsx
- src/app/approve/page.tsx
- src/components/approve/ApproveClient.tsx
- src/app/files/page.tsx
- src/components/files/FileBrowser.tsx
- src/app/ab/page.tsx
- src/components/ab/AbTestClient.tsx

spider-admin 화면 패턴:
- src/main/java/com/example/admin_demo/global/page/controller/PageController.java
- src/main/resources/templates/pages/*/*.html
- src/main/resources/templates/pages/*/*-script.html
- src/main/resources/templates/fragments/sidebar.html
- src/main/resources/menu-resource-permissions.yml
```

#### 작업

- [x] `spider-admin` 메뉴 트리에 CMS 관리 하위 메뉴 정리
- [x] CMS 승인 관리 화면 골격 추가
- [x] CMS 관리자/결재자 승인/반려 화면 골격 추가
- [x] CMS 리소스 검토/참조 화면 골격 추가
- [x] CMS A/B 또는 통계 화면 필요 여부 반영
- [x] 기존 tab 내부 페이지와 외부 URL 이동이 충돌하지 않도록 사이드바 이동 정책 정리

#### 확정 산출물

- `spider-admin`에 관리자/결재자용 `/cms-admin/approvals`, `/cms-admin/files`, `/cms-admin/ab-tests`, `/cms-admin/deployments`, `/cms-admin/statistics`, `/cms-admin/components` 화면 골격을 추가했다. `/cms-admin/files`는 업로드/삭제/폴더 생성이 아니라 필요 시 리소스 검토/참조 화면으로만 유지한다.
- 페이지 제작자 dashboard는 `/cms-admin/pages`로 이관하지 않고 현재처럼 `/cms/dashboard`를 유지한다.
- 기존 `/cms/**` seed는 CMS Next 제작자 dashboard와 편집 화면 진입 경로로 유지하고, `/cms-admin/**`용 새 메뉴 ID(`v3_cms_admin_*`)를 추가했다.
- 화면은 데이터/API 연동 전 골격만 제공하며, 실제 페이지/승인/파일/A-B/통계/배포/컴포넌트 데이터 연동은 Issue 7~9에서 진행한다.
- 관리자 작업이력/API 구현은 Issue 4 범위에서 제외했다.

#### 완료 조건

- admin 로그인 후 CMS 관리자/결재자 화면에 진입할 수 있다.
- CMS 관리 화면 진입은 proxy 없이 `spider-admin` 로그인만으로도 가능한지 확인한다.
- 실제 데이터 연동 전에도 화면, 라우팅, 권한 흐름을 확인할 수 있다.

---

### Issue 5. [CMS 이관] CMS dashboard의 기존 편집 이동 유지 확인

#### 선행 이슈

- Issue 2. CMS 편집 라우팅 책임 정리

#### 참고 코드

```text
CMS 편집 원본:
- src/app/edit/page.tsx
- src/components/edit/EditClientLoader.tsx
- src/components/edit/EditClient.tsx
- src/lib/api-url.ts
- src/app/api/builder/load/route.ts
- src/app/api/builder/save/route.ts

CMS 제작자 화면에서 편집 진입하는 기존 코드:
- src/components/dashboard/DashboardClient.tsx

spider-admin 참고:
- src/main/resources/templates/pages/cms-admin-approvals/*
```

#### 작업

- [x] `/cms/dashboard` 페이지 목록/상세에서 기존 CMS Next 방식으로 `/cms/edit`에 이동하는지 확인
- [x] `/cms/dashboard`에서 새 페이지 생성 후 기존 CMS Next 방식으로 `/cms/edit`에 진입하는지 확인
- [x] `spider-admin` 관리자/결재자 화면에는 페이지 생성/수정/편집 버튼을 제공하지 않는다고 정리
- [x] 관리자/결재자는 제작자가 만든 페이지를 검토하고 승인/반려/운영 관리만 수행한다고 정리
- [x] CMS 편집 화면에서 승인/반려 버튼을 직접 제공하지 않도록 화면 흐름 정리

#### 구현 메모

- 페이지 제작자 기본 흐름은 `/cms/dashboard`에서 기존 CMS Next 라우팅으로 `/cms/edit`에 진입한다.
- `/cms/edit`은 CMS Next 내부 편집 화면이다.
- `spider-admin` 관리자/결재자 화면은 `/cms/edit`로 이동시키지 않는다.
- 편집 저장 자체는 CMS Next save API의 책임으로 둔다.

#### 완료 조건

- `/cms/dashboard`에서 기존 CMS Next 방식으로 `/cms/edit`에 진입할 수 있다.
- `spider-admin` 관리자/결재자 화면에서는 페이지 생성/수정/편집 기능을 제공하지 않는다.
- 승인/반려/운영성 작업은 `spider-admin` 화면/API에서 수행된다.

---

### Issue 6. [CMS 이관] cms-1-innova-next를 제작자 dashboard와 편집기 중심으로 축소

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정
- Issue 2. CMS 편집 라우팅 책임 정리

#### 참고 코드

```text
장기 유지 대상:
- src/app/dashboard/page.tsx 또는 현재 `/cms/dashboard` route
- src/app/[userId]/page.tsx
- src/components/dashboard/*
- src/app/edit/page.tsx
- src/components/edit/EditClientLoader.tsx
- src/components/edit/EditClient.tsx
- src/app/api/builder/load/route.ts
- src/app/api/builder/save/route.ts
- src/app/api/builder/upload/route.ts
- src/app/api/builder/thumbnail/route.ts
- src/lib/current-user.ts
- src/lib/java-admin-api.ts
- src/lib/api-url.ts

제거/redirect/deprecated 검토 대상:
- src/app/approve/page.tsx
- src/app/files/page.tsx
- src/app/ab/page.tsx
- src/components/approve/*
- src/components/files/*
- src/components/ab/*
- src/app/api/builder/pages/[pageId]/approve-request/route.ts
- src/app/api/builder/pages/[pageId]/approve/route.ts
- src/app/api/builder/pages/[pageId]/reject/route.ts
- src/app/api/deploy/push/route.ts
- src/app/api/deploy/history/route.ts
```

#### 작업

- [ ] 제작자 dashboard(`/cms/dashboard`)는 유지하고, 관리자성 승인/파일/A-B 관리 페이지의 최종 처리 방향 결정
  - 제거
  - admin으로 redirect
  - read-only 임시 유지
- [ ] `/edit` route는 `/cms/dashboard`에서 진입하는 기존 CMS Next 편집 화면으로 유지
- [ ] 편집 저장에 필요한 최소 API만 유지
- [ ] 승인/반려 API의 직접 처리 경로 제거 또는 deprecated 처리
- [ ] 제작자 승인 요청 API는 `/cms/dashboard`에 유지하고, 별도 결재/워크플로 API 중계는 이번 이관 범위에서 제외
- [ ] admin으로 이관된 기능으로 이동하는 링크/버튼 정리
- [ ] 권한 체크를 admin의 `/api/auth/me` 기준으로 유지 또는 새 contract에 맞게 수정
- [ ] ContentBuilder 로딩과 편집기 런타임이 관리자 화면 제거 후에도 정상 동작하는지 확인

#### 완료 조건

- CMS 앱은 제작자 dashboard와 편집 화면에 필요한 기능만 제공한다.
- 관리자 기능으로 직접 접근하면 admin으로 안내되거나 차단된다.
- CMS 앱은 관리자/결재자의 승인/반려/운영성 작업 API를 직접 소유하지 않는다.

---

### Issue 7. [CMS 이관] spider-admin에 CMS 관리자/결재자 승인 데이터 연동 구현

#### 선행 이슈

- Issue 3. 데이터/API 소유권 및 전환 전략 정리
- Issue 4. spider-admin에 CMS 관리 메뉴/화면 골격 추가

#### 참고 코드

```text
CMS 화면/API 원본:
- src/components/approve/ApproveClient.tsx
- src/components/approve/RollbackModal.tsx
- src/app/api/builder/pages/route.ts
- src/app/api/builder/pages/[pageId]/approve/route.ts
- src/app/api/builder/pages/[pageId]/reject/route.ts
- src/app/api/builder/pages/[pageId]/set-public/route.ts
- src/app/api/builder/pages/[pageId]/update-dates/route.ts
- src/app/api/builder/pages/[pageId]/history/route.ts
- src/app/api/builder/pages/[pageId]/rollback/route.ts
- src/app/api/auth/approvers/route.ts

CMS DB 원본:
- src/db/repository/page.repository.ts
- src/db/queries/page.sql.ts
- src/db/queries/page-history.sql.ts
- src/db/queries/component-map.sql.ts
- src/db/queries/asset.sql.ts

spider-admin 구현 참고:
- src/main/java/com/example/admin_demo/domain/*/controller
- src/main/java/com/example/admin_demo/domain/*/service
- src/main/java/com/example/admin_demo/domain/*/mapper
- src/main/resources/mapper/oracle/*/*.xml
- src/main/java/com/example/admin_demo/global/log/RequestTraceInterceptor.java
- src/main/resources/mapper/oracle/adminhistory/AdminActionLogMapper.xml
```

#### 작업

- [x] 관리자/결재자 승인 대기 목록/검색/정렬/필터 API 연동
- [x] 승인 대상 페이지 미리보기/이력 조회 연동
- [x] 승인/반려/공개 상태/노출 기간 수정/롤백 연동
- [x] 승인/반려 시 `spider-admin` 백엔드에서 `SPW_CMS_PAGE` 상태 변경과 `SPW_CMS_PAGE_HISTORY` 이력 저장 처리
- [x] 승인/반려 요청이 `FWK_USER_ACCESS_HIS` 관리자 작업이력에 남는지 확인
- [x] 제작자 dashboard(`/cms/dashboard`)의 페이지 생성/삭제/편집/승인 요청은 Issue 7 범위에서 제외
- [x] `spider-admin` 관리자 권한 기준 적용
- [x] controller/service 테스트 추가

#### 진행 상황

- `domain.cmsapproval` 하위에 승인 관리 controller/service/mapper/DTO를 추가했다.
- `GET /api/cms-admin/approvals`에서 승인 관리 목록, 검색어, 승인 상태, 정렬, 페이징을 조회한다.
- `POST /api/cms-admin/pages/{pageId}/approval/approve`, `POST /api/cms-admin/pages/{pageId}/approval/reject`로 승인/반려를 처리하고 승인 이력 스냅샷을 저장한다.
- `PATCH /api/cms-admin/pages/{pageId}/public-state`, `PATCH /api/cms-admin/pages/{pageId}/display-period`, `GET /api/cms-admin/pages/{pageId}/approval-history`, `POST /api/cms-admin/pages/{pageId}/rollback`을 추가했다.
- `/cms-admin/approvals`는 `pages/cms-approval/cms-approval` 화면으로 연결되며, 목록/미리보기/승인/반려/공개 상태/노출 기간/이력/롤백 UI가 `spider-admin` API를 호출한다.
- 승인 관리 API는 조회에 `CMS:R`, 변경 작업에 `CMS:W` 권한을 적용한다. 화면 버튼도 `CMS:W` 권한 기준으로 노출한다.
- `/cms-admin/pages`, `/cms-admin/files`, `/cms-admin/ab-tests`, `/cms-admin/deployments`, `/cms-admin/statistics`, `/cms-admin/components`는 아직 스켈레톤으로 유지한다.
- 기존 CMS Next와 동일하게 별도 결재/워크플로 API 호출 없이 `spider-admin` 백엔드가 승인/반려 owner가 된다.
- 승인/반려 서비스는 `SPW_CMS_PAGE` 상태 변경과 `SPW_CMS_PAGE_HISTORY` 스냅샷 저장을 한 트랜잭션 흐름에서 처리한다.
- 승인, 반려, 공개 상태 변경, 노출 기간 수정, 롤백 같은 `/api/cms-admin/pages/**` write 요청은 기존 `RequestTraceInterceptor` -> `RdbAccessLogListener` -> `AdminActionLogMapper.insert` 흐름으로 `FWK_USER_ACCESS_HIS`에 기록된다. 기록 URL은 예를 들어 `[POST] /api/cms-admin/pages/{pageId}/approval/approve`, `[POST] /api/cms-admin/pages/{pageId}/approval/reject` 형식이다.
- 라우트 테스트는 `PageControllerCmsAdminMenuTest`에 반영됐다. 승인 관리 API controller/service 테스트는 `CmsApprovalControllerTest`, `CmsApprovalServiceTest`에 추가했고, 관리자 작업이력 저장 형식은 `RdbAccessLogListenerTest`로 검증한다.

#### 완료 조건

- 기존 CMS `/approve`의 관리자/결재자 승인 관리 기능을 `spider-admin`에서 수행할 수 있다.
- 승인/반려 관련 요청은 CMS Next API가 아니라 `spider-admin` API를 통해 처리된다.
- 승인/반려 관련 요청은 관리자 작업이력 로그에서 추적 가능하다.

---

### Issue 8. [CMS 이관] CMS 리소스 라이프사이클과 Docker volume 경로 정책 정리

#### 선행 이슈

- Issue 3. 데이터/API 소유권 및 전환 전략 정리
- Issue 4. spider-admin에 CMS 관리 메뉴/화면 골격 추가

#### 참고 코드

```text
기존 CMS 파일/에셋 화면/API. Docker volume 기반 리소스 라이프사이클로 전환 시 이관하지 않고 제거/비활성/참조 전용 여부만 판단:
- src/app/files/page.tsx
- src/components/files/FileBrowser.tsx
- src/components/files/FolderTree.tsx
- src/components/files/FileCard.tsx
- src/components/files/CreateFolderModal.tsx
- src/components/files/DeleteConfirmModal.tsx
- src/components/files/UploadProgressList.tsx
- src/app/api/manage/folders/route.ts
- src/app/api/manage/files/route.ts
- src/app/api/manage/upload/route.ts
- src/app/api/manage/delete/route.ts
- src/app/api/manage/addfolder/route.ts
- src/app/api/assets/route.ts
- src/app/api/assets/[assetId]/route.ts
- src/app/api/assets/[assetId]/image/route.ts

CMS DB/유틸 원본. Docker volume 기반 리소스 정책과 충돌 여부를 확인:
- src/db/repository/asset.repository.ts
- src/db/queries/asset.sql.ts
- src/lib/upload.ts
- src/lib/upload-utils.ts
- src/lib/page-file.ts

배포/정적 경로 참고:
- src/app/api/deploy/push/route.ts
- src/app/api/deploy/receive/route.ts
- src/lib/deploy-utils.ts
```

#### 작업

- [x] "파일/에셋" 범위를 "CMS 리소스"로 재정의
  - 이미지 정적 리소스
  - 배포된 페이지 HTML
  - ContentBuilder 편집 중 임시 업로드 리소스
- [x] 리소스 라이프사이클을 문서화
  - 현업 관리자 또는 제작자가 `3000` CMS 결재 전 volume에 이미지 리소스 승인 요청을 등록
  - 관리자/결재자가 `spider-admin`에서 리소스와 페이지를 검토 및 승인
  - 승인 완료 시 리소스가 `3001` Docker volume으로 복사되어 배포/사용 가능 상태가 됨
  - 현업 제작자는 `3001` 제작/대고객 환경에서 승인된 이미지만 사용해 페이지를 제작
  - 대고객 서비스를 통해 일반 사용자에게 콘텐츠 노출
- [x] `3001`이 저작환경과 승인 후 배포 volume으로 함께 언급되므로, 실제 운영 명칭을 확정
  - `3000`: 현업 관리자용 CMS, 결재 전 리소스 저장소(`/data/uploads`)
  - `3001`: 현업 제작자용 CMS + 대고객 서빙, 결재 후 리소스/배포 HTML 저장소(`/data/deployed`)
- [x] 결재 전 리소스를 `3000` CMS Docker volume에 임시 저장할지 영속 저장할지 정책 결정
  - 결재 전 리소스는 영속 저장한다.
  - 버전, 이력, 보관 기간, 정리 배치 기준은 이미지 승인 분리 이슈에서 후속 확정한다.
  - 이미지 승인 분리 상세는 `docs/cms-image-approval-issues.md`에서 별도 관리한다.
- [x] 이미지와 페이지 HTML은 서버 Docker volume의 정적 리소스 경로에서 관리한다는 정책을 문서화
  - 결재 전 이미지: `3000`의 `/data/uploads`
  - 승인 완료 이미지와 배포 HTML: `3001`의 `/data/deployed`
  - 페이지/템플릿 HTML 원본은 DB CLOB(`SPW_CMS_PAGE.PAGE_HTML`)을 우선 사용하고, 배포 결과물은 `/data/deployed/{pageId}.html`로 생성한다.
- [x] `spider-admin`에 파일 업로드/삭제/폴더 생성 관리 화면을 만들지 않는다는 전제를 명시
  - `/cms-admin/files`는 현재 구현대로 `/cms-admin/approvals` redirect를 유지한다.
  - `v3_cms_admin_files`는 `DISPLAY_YN=N`, `USE_YN=N` 상태를 유지한다.
- [x] 기존 CMS 파일 관리 화면(`/cms/files`)은 제거, `spider-admin` 안내로 redirect, 또는 read-only 참조 중 하나로 결정
  - 삭제하지 않고 주석 처리 또는 비활성 처리한다.
  - 사유는 "승인된 이미지만 사용" 정책과 충돌하는 임의 파일/폴더 관리 경로 차단으로 남긴다.
- [x] 기존 CMS `manage/*`, `assets/*` API 처리 방향 결정
  - `manage/*`는 삭제하지 않고 주석 처리 또는 비활성 처리한다.
  - 주석에는 "승인된 이미지만 사용" 정책과 충돌하므로 임의 파일/폴더 관리 경로를 막는다는 사유를 남긴다.
  - `assets/*`는 이미지 승인 요청/승인 이미지 조회 API로 재정의할지, 새 API로 분리하고 기존 API를 주석/비활성 처리할지 후속 이미지 승인 이슈에서 결정한다.
- [x] CMS 편집 화면(`/cms/edit`)이 ContentBuilder에서 사용할 이미지 URL과 HTML URL을 `3000` CMS 검토 volume 기준으로 참조하도록 경로 정책 정리
  - 기존 즉시 업로드 경로(`/api/builder/upload`)는 페이지 즉시 삽입용으로 계속 사용하지 않고, 이미지 승인 요청 흐름으로 분리한다.
  - 제작 화면에서는 승인 완료 이미지 목록만 사용한다.
  - 상세 구현 이슈는 `docs/cms-image-approval-issues.md`에서 별도 관리한다.
- [x] 승인/배포 완료 후 대고객 서비스가 사용할 이미지 URL과 HTML URL을 `3001` 배포 volume 기준으로 참조하도록 경로 정책 정리
  - 승인 완료 이미지는 `/data/deployed` 기준 URL을 사용한다.
  - 배포 HTML 물리 파일은 `/data/deployed/{pageId}.html` 기준으로 생성한다.
  - 브라우저 노출 URL은 CMS Next `basePath=/cms`와 정적 파일 서빙 방식에 따라 달라지므로, Issue 9의 배포 관리 이관에서 최종 확정한다.
- [x] 승인 완료 시 `3000` CMS Docker volume의 리소스를 `3001` Docker volume으로 이동할지 복사할지 결정
  - 승인 완료 시 복사한다.
  - 중복 파일 정리와 무결성 검증 방식은 이미지 승인 분리 이슈의 후속 보관 정책에서 확정한다.
- [x] 배포 시 HTML 내부 이미지/정적 리소스 URL 치환 정책을 `3000 -> 3001` volume 전환 기준으로 검증
  - 현재 코드에는 spider-admin 배포 시 HTML 내부 이미지/정적 리소스 URL 치환 로직이 없음을 확인했다.
  - 실제 치환/복사/서빙 구현은 Issue 9와 이미지 승인 분리 이슈로 이관한다.
- [x] 정적 리소스 삭제/교체/백업/rollback은 서버 운영 또는 배포 프로세스에서 관리한다는 책임 경계 정리
  - spider-admin은 파일 업로드/삭제/폴더 생성 기능을 소유하지 않는다.
  - 정적 리소스 삭제/교체/백업/rollback의 실제 운영 절차와 구현은 후속 운영/배포 이슈에서 확정한다.

#### 완료 조건

- [x] 이미지와 페이지 HTML이 `3000 결재 전 volume -> 3001 배포 volume -> 대고객 서비스` 흐름으로 관리된다는 기준이 문서화된다.
- [x] 결재 전 리소스의 임시/영속 저장 정책이 결정된다.
- [x] 승인 완료 시 리소스를 이동할지 복사할지 정책이 결정된다.
- [x] `spider-admin`은 파일 업로드/삭제/폴더 생성 기능을 소유하지 않는다.
- [x] CMS 편집기, 결재 검토 화면, 배포 결과물이 각 단계의 Docker volume 경로를 안정적으로 참조하는지 검증 범위를 분리했다.
  - 코드/수동 검증으로 현재 불일치 지점을 확인했다.
  - 실제 안정화 구현은 Issue 9와 이미지 승인 분리 이슈로 이관한다.
- [x] 기존 CMS 파일/에셋 관리 화면과 API의 처리 방향이 정리된다.

#### 진행 결과

- `docs/cms-image-approval-issues.md`를 추가해 이미지 승인 요청, 관리자 승인, 승인된 이미지 선택, 기존 즉시 업로드 경로 분리 작업을 별도 이슈로 분리했다.
- 결재 전 리소스는 영속 저장으로 결정했다.
- `manage/*` API는 삭제하지 않고 주석 처리 또는 비활성 처리하며, 승인된 이미지만 사용하는 정책과 충돌한다는 사유를 남기기로 했다.
- `/cms-admin/files`는 현재 구현대로 `/cms-admin/approvals` redirect를 유지한다.
- 배포 검증 중 receive URL, deploy secret, deployed HTML 서빙 경로 불일치를 확인했다.
- 해당 불일치는 Issue 8에서 즉시 구현하지 않고 Issue 9 배포 관리 이관 후속 작업으로 넘긴다.

#### 후속 이슈로 이관한 작업

- 이미지 승인 요청, 관리자 승인, 승인된 이미지 선택, `/api/builder/upload` 즉시 삽입 차단은 `docs/cms-image-approval-issues.md`에서 진행한다.
- CMS Next에서 `manage/*`, `/cms/files`, `/api/builder/upload`를 실제로 주석/비활성 처리하고 사유 주석을 남기는 작업도 이미지 승인 분리 이슈에서 진행한다.
- spider-admin 배포 receive URL, deploy secret, 배포 HTML 브라우저 서빙 경로, 미리보기 URL 정렬은 `docs/cms-deploy-serving-followup-issues.md`에서 진행한다.
- 배포 시 HTML 내부 이미지/정적 리소스 URL 치환 또는 승인 이미지 복사 정책은 이미지 승인 분리 이슈의 승인 이미지 저장/URL 정책 확정 후 `docs/cms-deploy-serving-followup-issues.md`에서 진행한다.
- 정적 리소스 삭제/교체/백업/rollback 책임 경계는 배포 서빙 경로가 확정된 뒤 `docs/cms-deploy-serving-followup-issues.md`의 운영 기준 이슈에서 확정한다.

#### 검증 역할 분리 체크리스트

Issue 8의 완료 판단은 코드만으로 끝낼 수 없다. 코드 기준으로 경로 정책을 확인한 뒤, 실제 CMS 제작/승인/배포 환경에서 이미지가 깨지지 않는지 사용자가 확인해야 한다.

##### Codex가 코드 기반으로 확인할 수 있는 것

- [x] CMS Next 업로드 API가 파일을 어떤 경로에 저장하는지 확인
  - 확인 파일:
    - `cms-1-innova-next/src/app/api/builder/upload/route.ts`
    - `cms-1-innova-next/src/app/api/assets/route.ts`
    - `cms-1-innova-next/src/lib/env.ts`
  - 확인 기준:
    - 업로드 파일의 실제 저장 경로가 `ASSET_UPLOAD_DIR` 또는 `/data/uploads` 정책과 맞는지 확인한다.
    - DB의 `SPW_CMS_ASSET.ASSET_PATH`, `ASSET_URL`에 어떤 값이 저장되는지 확인한다.
  - 확인 결과:
    - `/api/builder/upload`와 `/api/assets`는 모두 `ASSET_UPLOAD_DIR` 아래에 파일을 저장하고, `ASSET_BASE_URL/{filename}`을 URL로 반환한다.
    - `src/lib/env.ts` 기본값은 `ASSET_UPLOAD_DIR=public/uploads`, `ASSET_BASE_URL=/uploads`다.
    - 운영 예시 `.env.prod.example`은 `ASSET_UPLOAD_DIR=/app/public/uploads`, `ASSET_BASE_URL=/static`로 안내한다.
    - `docker-compose-prod.yml` 기준 CMS 컨테이너는 `/data/uploads:/app/public/uploads`를 마운트한다.
    - 따라서 운영 설정을 `.env.prod.example`처럼 적용하면 결재 전 업로드 파일은 호스트 `/data/uploads`에 저장되고 DB에는 컨테이너 내부 경로(`/app/public/uploads/...`)와 URL(`/static/...`)이 저장된다.

- [x] CMS Next 편집기에서 이미지 URL이 페이지 데이터에 어떻게 들어가는지 확인
  - 확인 파일:
    - `cms-1-innova-next/src/components/edit/EditClient.tsx`
    - `cms-1-innova-next/src/components/edit/SlideEditorModal.tsx`
    - `cms-1-innova-next/src/components/edit/AuthCenterIconEditor.tsx`
    - `cms-1-innova-next/src/components/edit/ProductMenuIconEditor.tsx`
  - 확인 기준:
    - 로컬 업로드 후 `/api/builder/upload` 응답 URL이 페이지 HTML 또는 블록 데이터에 즉시 반영되는지 확인한다.
    - 승인된 이미지 조회 API인 `GET /api/assets?assetState=APPROVED`를 사용할 수 있는 전환 지점이 어디인지 표시한다.
  - 확인 결과:
    - `EditClient.tsx`의 ContentBuilder `upload` 콜백은 `/api/builder/upload` 응답 URL을 `nextApi(result.url)`로 보정한 뒤 ContentBuilder 이미지 `src`에 직접 사용한다.
    - `SlideEditorModal.tsx`는 로컬 파일 업로드 후 응답 URL을 `bgImage` 값으로 즉시 반영한다.
    - `AuthCenterIconEditor.tsx`, `ProductMenuIconEditor.tsx`는 로컬 파일 업로드 후 응답 URL을 `<img src="...">`로 DOM에 즉시 삽입한다.
    - 현재 편집기는 승인 이미지 조회 API(`GET /api/assets?assetState=APPROVED`)를 사용하지 않는다.
    - 따라서 "승인된 이미지만 사용" 정책은 아직 UI/저장 단계에 적용되지 않았고, 후속 이미지 승인 분리 이슈에서 업로드 콜백을 승인 이미지 선택 흐름으로 교체해야 한다.

- [x] spider-admin 배포 API가 배포 HTML과 미리보기 URL을 어떤 경로로 만드는지 확인
  - 확인 파일:
    - `spider-admin/src/main/java/com/example/admin_demo/domain/cmsdeployment/service/CmsDeployService.java`
    - `spider-admin/src/main/resources/mapper/oracle/cmsdeployment/CmsDeployMapper.xml`
  - 확인 기준:
    - 배포 결과 HTML이 `/data/deployed/{pageId}.html` 정책과 맞는지 확인한다.
    - 미리보기 URL이 `/cms/deployed/{pageId}.html` 기준인지 확인한다.
  - 확인 결과:
    - `CmsDeployService.push()`는 승인된 페이지의 `SPW_CMS_PAGE.PAGE_HTML`을 읽어 `cms.deploy.receive-url`로 전송한다.
    - 현재 `application.yml` 기본 receive URL은 `http://133.186.135.23:3001/api/deploy/receive`다.
    - CMS Next의 `src/app/api/deploy/receive/route.ts`는 수신한 HTML을 operation 컨테이너의 `public/deployed/{pageId}.html`에 저장한다.
    - `docker-compose-prod.yml` 기준 operation 컨테이너의 `public/deployed`는 호스트 `/data/deployed`에 마운트된다.
    - 현재 `CmsDeployMapper.xml`의 배포 미리보기 URL은 `http://{INSTANCE_IP}:{INSTANCE_PORT}/deployed/{PAGE_ID}.html` 형태다.
    - 수동 검증 결과 CMS Next가 `basePath=/cms`로 동작하므로 receive URL, preview URL, 실제 HTML 서빙 경로가 서로 맞지 않는다.
    - 이 불일치는 Issue 9 배포 관리 이관에서 설정/서빙 route/nginx 중 하나로 확정한다.

- [x] HTML 내부 이미지/정적 리소스 URL 치환 로직 존재 여부 확인
  - 확인 기준:
    - 배포 시점에 `/uploads/...`, `http://localhost:3000/...`, `/api/assets/...` 같은 URL을 `/deployed/...` 또는 운영 정적 경로로 바꾸는 코드가 있는지 확인한다.
    - 치환 코드가 없다면 "현재 배포 HTML은 저장된 원본 URL을 그대로 사용한다"고 명시한다.
  - 확인 결과:
    - 현재 이관 경로인 `spider-admin`의 `CmsDeployService.push()`에는 HTML 내부 이미지/정적 리소스 URL 치환 로직이 없다.
    - `spider-admin`은 DB의 `PAGE_HTML`을 그대로 receive API로 전송한다.
    - CMS Next의 기존 `/api/deploy/push`에는 `/uploads`, `/assets`, `/api/assets` 경로를 `CMS_BASE_URL` 절대 URL로 치환하는 코드가 남아 있지만, `spider-admin` 배포 흐름에서는 이 코드를 호출하지 않는다.
    - `docker-compose-prod.yml` 기준 operation 컨테이너는 `/data/uploads:/app/public/uploads:ro`도 함께 마운트하고, `nginx/nginx.conf`도 `/static/`을 `/data/uploads/`로 alias 한다.
    - 이 구성은 현재 이미지 로딩을 가능하게 만들 수 있지만, 문서의 목표인 "3001은 승인 후 `/data/deployed` 리소스만 사용" 정책과는 충돌한다.
    - 따라서 실제 환경 검증에서 배포 HTML 내부 이미지 URL이 `/static/...`, `/uploads/...`, `/api/assets/...`, `/deployed/static/...` 중 무엇인지 반드시 확인해야 한다.

##### 사용자가 실제 환경에서 확인해야 하는 것

- [x] 개발 또는 검증 환경에서 CMS Next와 spider-admin을 실행한다.
  - 확인 동작:
    1. CMS Next 제작 화면에 접근 가능한 상태로 실행한다.
    2. spider-admin 관리자 화면에 접근 가능한 상태로 실행한다.
    3. 두 서비스가 같은 검증 DB를 바라보는지 확인한다.
  - 체크 기준:
    - `/cms/dashboard` 접근 가능
    - `spider-admin`의 `/cms-admin/approvals` 접근 가능
    - `spider-admin`의 `/cms-admin/deployments` 접근 가능

- [x] 이미지가 포함된 테스트 페이지를 만든다.
  - 확인 동작:
    1. 제작자 계정으로 `/cms/dashboard`에 접속한다.
    2. 새 페이지를 생성하거나 기존 테스트 페이지를 편집한다.
    3. 이미지 업로드가 가능한 컴포넌트에서 로컬 PC 이미지를 선택한다.
    4. 페이지를 저장한다.
  - 체크 기준:
    - 편집 화면에서 이미지가 보인다.
    - 저장 후 다시 열어도 이미지가 보인다.
    - 이 단계에서 로컬 이미지가 바로 적용된다면, 승인 이미지 분리 이슈에서 차단해야 할 현재 동작으로 기록한다.

- [x] DB에서 이미지와 페이지 저장 값을 확인한다.
  - 확인 SQL 예시:

```sql
SELECT ASSET_ID,
       ASSET_NAME,
       ASSET_STATE,
       ASSET_PATH,
       ASSET_URL,
       USE_YN,
       CREATE_DATE
FROM SPW_CMS_ASSET
ORDER BY CREATE_DATE DESC;
```

```sql
SELECT PAGE_ID,
       VERSION,
       APPROVE_STATE,
       PAGE_HTML,
       FILE_ID,
       DISPLAY_START_DATE,
       DISPLAY_END_DATE
FROM SPW_CMS_PAGE
WHERE PAGE_ID = :pageId;
```

  - 체크 기준:
    - 업로드한 이미지가 `SPW_CMS_ASSET`에 저장되어 있는지 확인한다.
    - `ASSET_STATE`가 무엇인지 기록한다. : (check) work
    - `ASSET_PATH`가 결재 전 저장소 경로인지 확인한다. : (check) public\uploads\6c86021a-5a8e-49b5-936b-65d825574efc_IBK_LOGO.png
    - `ASSET_URL`이 편집/배포 후에도 접근 가능한 URL인지 확인한다. : (check) /uploads/6c86021a-5a8e-49b5-936b-65d825574efc_IBK_LOGO.png
    - `PAGE_HTML` 안에 이미지 URL이 어떤 형태로 들어갔는지 기록한다. : (check) <img src="/cms/uploads/6c86021a-5a8e-49b5-936b-65d825574efc_IBK_LOGO.png" alt="은행 로고" style="width:34px;height:34px;object-fit:contain;border-radius:6px;flex-shrink:0;display:block;">

- [ ] spider-admin에서 페이지 승인과 배포를 진행한다.
  - 확인 동작:
    1. 관리자 계정으로 `spider-admin`에 로그인한다.
    2. `/cms-admin/approvals`에서 테스트 페이지를 조회한다.
    3. 페이지를 승인한다.
    4. `/cms-admin/deployments`에서 승인된 페이지를 조회한다.
    5. 배포를 실행한다.

  - 체크 기준:
    - 승인 성공
    - 배포 성공
    - 배포 이력 생성
    - 배포 미리보기 URL이 실제 접근 가능한 운영 HTML 경로로 표시
  - 확인 결과:
    - 1~4단계는 진행됐으나 배포 실행에서 서버 오류가 발생했다.
    - 1차 오류는 `http://133.186.135.23:3001/api/deploy/receive` 호출 `404 Not Found`였다.
    - 원인: CMS Next는 기본 `basePath=/cms`로 동작하므로 receive API는 `/cms/api/deploy/receive` 경로로 호출해야 한다.
    - 2차 오류는 `http://133.186.135.23:3001/cms/api/deploy/receive` 호출 `401 Unauthorized`였다.
    - 원인: CMS Next receive API는 `DEPLOY_SECRET`과 `x-deploy-token` 헤더를 비교한다. spider-admin은 `cms.deploy.secret` 값을 `x-deploy-token`으로 보내는데, 기존 기본값이 비어 있어 인증에 실패했다.
    - secret을 맞춘 뒤 배포 이력과 미리보기 URL 표시는 확인됐으나, `http://133.186.135.23:3001/cms/deployed/{pageId}.html` 접근 시에도 "페이지를 찾을 수 없습니다"가 표시됐다.
    - 결론: 배포 수신, 인증, 미리보기 URL, 배포 HTML 정적 서빙 경로가 서로 완전히 정렬되지 않았다.
    - 이 항목은 Issue 8에서 추가 구현하지 않고 Issue 9 배포 관리 이관의 후속 작업으로 넘긴다.

- [ ] 배포 결과 HTML과 이미지 로딩을 브라우저에서 확인한다.
  - 확인 동작:
    1. 배포 미리보기 URL을 브라우저에서 연다.
    2. 개발자 도구 Network 탭을 연다.
    3. 이미지 요청을 필터링한다.
    4. 이미지 요청 URL과 HTTP 상태를 확인한다.
  - 체크 기준:
    - 배포 HTML이 200으로 열린다.
    - 이미지 요청이 200으로 열린다.
    - 이미지 요청 URL이 `3000` 전용 경로를 바라보지 않는다.
    - 이미지 요청 URL이 운영 또는 `3001`에서 접근 가능한 정적 경로를 바라본다.

- [ ] 서버 파일 시스템 또는 Docker volume에서 실제 파일 위치를 확인한다.
  - 확인 동작:
    1. 결재 전 업로드 이미지가 `/data/uploads` 계열에 있는지 확인한다.
    2. 승인/배포 후 운영에서 참조해야 하는 파일이 `/data/deployed` 계열에 있는지 확인한다.
    3. 배포 HTML 파일이 `/data/deployed/{pageId}.html` 또는 설정된 배포 디렉터리에 생성됐는지 확인한다.
  - 체크 기준:
    - HTML 파일 존재
    - HTML이 참조하는 이미지 파일 존재
    - 브라우저에서 요청하는 URL과 서버 파일 위치가 매핑됨

##### 검증 결과별 판단

- [ ] 배포 HTML과 이미지가 모두 정상 로드된다.
  - 판단:
    - Issue 8의 Docker volume 경로 정책은 현재 구현과 충돌하지 않는다.
    - 남은 작업은 이미지 승인 분리 이슈의 업로드 차단/승인 이미지 선택 전환으로 넘긴다.

- [ ] 배포 HTML은 열리지만 이미지가 깨진다.
  - 판단:
    - Issue 8에서 HTML 내부 이미지 URL 치환 정책 또는 승인 시 이미지 복사 정책을 추가로 확정해야 한다.
    - 깨진 이미지 URL, 실제 파일 위치, 기대 URL을 기록한다.

- [x] 배포 HTML 자체가 열리지 않는다.
  - 판단:
    - spider-admin 배포 receive URL, secret, 미리보기 URL 생성, CMS Next 또는 nginx의 `/deployed/**` 정적 서빙 정책을 Issue 9에서 함께 확정해야 한다.
  - 확인 결과:
    - `/cms/deployed/{pageId}.html` 경로에서도 배포 HTML이 열리지 않는 것을 확인했다.
    - 이는 Issue 8에서 끝낼 검증 항목이 아니라 Issue 9 배포 관리 이관의 구현 항목으로 이관한다.

- [x] 미승인 이미지가 페이지에 그대로 들어간다.
  - 판단:
    - 이는 Issue 8의 Docker volume 검증 실패가 아니라 이미지 승인 분리 이슈의 구현 대상이다.
    - 후속 이슈에서 `/api/builder/upload` 즉시 삽입 차단, 승인 이미지 선택 UI, 저장/승인 시 미승인 이미지 검증을 진행한다.
  - 확인 결과:
    - DB 확인 결과 `ASSET_STATE=WORK` 이미지가 `PAGE_HTML` 안에 `/cms/uploads/...`로 저장되어 있다.
    - 이 항목은 Issue 8의 배포 volume 검증 실패라기보다, 이미지 승인 분리 이슈에서 차단해야 할 현재 동작으로 확정한다.

---

### Issue 9. [CMS 이관] A/B, 통계, 배포 관리 기능 이관

#### 선행 이슈

- Issue 3. 데이터/API 소유권 및 전환 전략 정리
- Issue 4. spider-admin에 CMS 관리 메뉴/화면 골격 추가

#### 참고 코드

```text
A/B 원본:
- src/app/ab/page.tsx
- src/components/ab/AbTestClient.tsx
- src/app/api/builder/ab/route.ts
- src/app/api/builder/ab/promote/route.ts
- src/db/ddl/V3__ab_test.sql

통계 원본:
- src/components/approve/StatsModal.tsx
- src/app/api/track/stats/route.ts
- src/db/repository/page-view-log.repository.ts
- src/db/queries/page-view-log.sql.ts
- src/db/ddl/V2__page_view_log.sql

배포 원본:
- src/app/api/deploy/push/route.ts
- src/app/api/deploy/history/route.ts
- src/app/api/deploy/receive/route.ts
- src/lib/deploy-utils.ts
- src/db/repository/file-send.repository.ts
- src/db/queries/file-send.sql.ts
- src/db/queries/server.sql.ts
```

#### 작업

- [ ] A/B 그룹 목록/승격/노출 가중치 관리 기능 이관
- [ ] 조회/클릭 통계 조회 기능 이관
- [ ] 배포 push/history 기능 이관
- [ ] 배포 receive API 유지 위치 결정
- [ ] 운영 서버 URL, tracker URL, asset URL 생성 정책 정리
- [ ] 배포 작업이 `spider-admin` 관리자 작업이력 로그에 남는지 확인

#### 완료 조건

- 운영자가 `spider-admin`에서 A/B, 통계, 배포 관련 관리 작업을 수행할 수 있다.

---

### Issue 10. [CMS 이관] 로컬 프록시와 E2E 시나리오 갱신

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정
- Issue 2. CMS 편집 라우팅 책임 정리

#### 참고 코드

```text
문서/프록시:
- docs/cms-integration-guide.md
- docker-compose.yml
- nginx/cms-local-proxy.conf
- README.md

CMS 테스트/설정:
- cms-1-innova-next/next.config.ts
- cms-1-innova-next/src/lib/api-url.ts
- cms-1-innova-next/src/lib/java-admin-api.ts
- cms-1-innova-next/e2e/**

spider-admin 테스트/설정:
- playwright.config.ts
- e2e/**
- src/test/java/com/example/admin_demo/global/auth/controller/AuthControllerCurrentUserTest.java
- src/test/java/com/example/admin_demo/global/page/controller/CmsRedirectControllerTest.java
```

#### 작업

- [ ] nginx local proxy routing 갱신
  - admin 관리 화면 경로
  - CMS 제작자 dashboard와 편집 화면 경로
  - admin API 경로
  - CMS Next API 경로
- [ ] proxy가 필요한 범위를 문서화
  - 관리자 화면은 `spider-admin` 직접 로그인으로 접근 가능
  - CMS 제작자 dashboard와 편집 화면은 기존 CMS Next 라우팅/세션 흐름으로 접근
- [ ] `README.md` 갱신
- [ ] `docs/cms-integration-guide.md` 갱신
- [ ] `cmsUser01` 로그인 -> `/cms/dashboard` -> `/cms/edit` -> 저장 후 dashboard 목록 refresh E2E 추가
- [ ] `CMS:R` 사용자의 제작/편집 차단 E2E 추가
- [ ] `CMS:W` 사용자의 제작/편집/승인 요청 가능 E2E 추가
- [ ] `userAdmin01` 로그인 -> `spider-admin` 승인/검토 화면 -> 승인/반려 E2E 추가
- [ ] 관리자/결재자 승인/반려가 `spider-admin` 작업이력 로그에 남는지 통합 테스트 시나리오 추가

#### 완료 조건

- 로컬 통합 테스트 경로가 문서와 E2E로 검증된다.
- `spider-admin` 관리자/결재자 흐름과 CMS Next 제작자 dashboard/편집 흐름이 구분되어 문서화된다.

---

### Issue 11. [CMS 이관] CMS 제작자/관리자 메뉴와 사용자 권한 정리

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정
- Issue 2. CMS 편집 라우팅 책임 정리
- Issue 4. spider-admin에 CMS 관리 메뉴/화면 골격 추가

#### 배경

CMS 이관 후 메뉴는 제작자용 CMS Next 화면과 관리자/결재자용 `spider-admin` 화면을 분리해야 한다.

- `cmsUser01` 같은 페이지 제작자는 `/cms/dashboard`로 이동해 페이지 생성, 편집, 승인 요청을 수행한다.
- `userAdmin01`, `cmsAdmin01` 같은 관리자/결재자는 `/cms-admin/**`에서 승인, 반려, 배포, 통계, 운영 관리만 수행한다.
- 관리자/결재자는 페이지를 생성하거나 ContentBuilder로 직접 편집하지 않는다.
- 제작자 dashboard는 `/cms-admin/pages`로 이관하지 않는다.

#### 참고 코드/설정

```text
spider-admin:
- src/main/java/com/example/admin_demo/global/page/controller/PageController.java
- src/main/resources/menu-resource-permissions.yml
- src/main/resources/templates/fragments/sidebar.html
- src/main/resources/templates/pages/cms-admin-approvals/*
- src/test/java/com/example/admin_demo/global/page/controller/PageControllerCmsAdminMenuTest.java
- src/test/java/com/example/admin_demo/global/page/controller/CmsRedirectControllerTest.java

CMS Next:
- src/app/dashboard/page.tsx 또는 현재 `/cms/dashboard` route
- src/components/dashboard/DashboardClient.tsx
- src/app/edit/page.tsx
- src/components/edit/EditClient.tsx
```

#### 목표 메뉴 구조

```text
CMS 상위 메뉴:
- v3_cms_manage              /cms                    DISPLAY_YN=Y  USE_YN=Y

CMS Next 제작자 화면. 메뉴에는 숨기되 권한/라우팅용으로 유지:
- v3_cms_dashboard           /cms/dashboard          DISPLAY_YN=N  USE_YN=Y
- v3_cms_edit                /cms/edit               DISPLAY_YN=N  USE_YN=Y

spider-admin 관리자/결재자 화면:
- v3_cms_admin_approvals     /cms-admin/approvals    DISPLAY_YN=Y  USE_YN=Y
- v3_cms_admin_deployments   /cms-admin/deployments  DISPLAY_YN=Y  USE_YN=Y
- v3_cms_admin_statistics    /cms-admin/statistics   DISPLAY_YN=Y  USE_YN=Y
- v3_cms_admin_ab_tests      /cms-admin/ab-tests     DISPLAY_YN=Y  USE_YN=Y
- v3_cms_admin_components    /cms-admin/components   DISPLAY_YN=Y  USE_YN=Y

제거/비활성 후보:
- v3_cms_admin_pages         /cms-admin/pages        USE_YN=N
- v3_cms_system              /cms/system             /cms/dashboard로 변경 또는 USE_YN=N
- v3_cms_approve             /cms/approve            USE_YN=N 또는 전환용 숨김
- v3_cms_files               /cms/files              USE_YN=N 또는 전환용 숨김
- v3_cms_admin_files         /cms-admin/files        리소스 참조 화면 필요 시만 유지, 일반 파일 관리면 USE_YN=N
```

#### 작업

- [x] `v3_cms_system` 처리 방향 결정
  - 기존 menu id를 유지한다면 `MENU_URL`을 `/cms/dashboard`로 변경하고 `MENU_NAME`을 `CMS 작업자 대시보드`로 유지
  - 새 menu id를 사용한다면 `v3_cms_dashboard`를 추가하고 `v3_cms_system`은 `USE_YN=N`
- [x] `v3_cms_admin_pages` 비활성화
  - 제작자 dashboard를 `/cms-admin/pages`로 이관하지 않으므로 `DISPLAY_YN=N`, `USE_YN=N`
  - `cmsAdmin01` 등 관리자 사용자 권한에서도 제거
- [x] `v3_cms_edit`은 메뉴에는 보이지 않게 유지
  - `DISPLAY_YN=N`, `USE_YN=Y`
  - 제작자 권한/라우팅용으로만 사용
  - 관리자/결재자 화면에서 `/cms/edit`로 이동시키지 않음
- [x] 기존 CMS Next 관리자성 메뉴 정리
  - `v3_cms_approve`(`/cms/approve`)는 `/cms-admin/approvals` 이관 후 `USE_YN=N` 또는 redirect/read-only 전환
  - `v3_cms_files`(`/cms/files`)는 Docker volume 리소스 정책 확정 후 `USE_YN=N` 또는 read-only/redirect 전환
- [x] `v3_cms_admin_files` 유지 여부 결정
  - 파일 업로드/삭제/폴더 생성 기능이면 비활성화
  - 승인/검토용 리소스 참조 화면이면 메뉴명을 `CMS 리소스 검토` 등으로 변경하고 read-only 기준 명시
- [x] `cmsUser01` 권한 정리
  - `v3_cms_manage` `W`
  - `v3_cms_dashboard` 또는 `/cms/dashboard`로 매핑되는 메뉴 `W`
  - `v3_cms_edit` `W`
  - `/cms-admin/**` 메뉴 권한은 부여하지 않음
- [x] `cmsAdmin01` 또는 관리자/결재자 권한 정리
  - `v3_cms_manage` `W`
  - `v3_cms_admin_approvals`, `v3_cms_admin_deployments`, `v3_cms_admin_statistics`, `v3_cms_admin_ab_tests`, `v3_cms_admin_components` 필요한 항목만 `W`
  - `v3_cms_admin_pages`, `v3_cms_edit`, `v3_cms_system` 권한 제거
  - 전환 기간에 `/cms/approve`, `/cms/files` 확인이 필요할 때만 임시 권한 유지
- [x] `menu-resource-permissions.yml` 갱신
  - 제작자 화면은 `CMS:R`, `CMS:W` 매핑 유지
  - 관리자/결재자 화면은 `spider-admin` 관리자 권한 정책 또는 CMS admin 전용 authority 정책으로 분리
- [x] 메뉴 클릭 동작 확인
  - `cmsUser01`이 CMS 메뉴 클릭 시 `/cms/dashboard`로 이동
  - `cmsAdmin01`이 CMS 관리자/결재자 메뉴 클릭 시 `/cms-admin/approvals` 등 `spider-admin` 화면으로 이동
  - `cmsAdmin01`에게 페이지 생성/편집 메뉴가 노출되지 않음

#### 진행 결과

- `v3_cms_dashboard`를 추가하고 `v3_cms_system`은 `DISPLAY_YN=N`, `USE_YN=N`으로 비활성화했다.
- `v3_cms_manage`는 CMS 상위 메뉴로 유지하고 `/cms` 진입점으로 사용한다.
- `/cms`, `/cms/` 진입 시 제작자 계정은 `/cms/dashboard`로 이동하고, `ADMIN` 또는 `cms_admin` 역할은 `/cms-admin/approvals`로 이동한다.
- `/cms-admin/pages`, `/cms-admin/files` 직접 접근은 `/cms-admin/approvals`로 리다이렉트한다.
- `v3_cms_admin_pages`, `v3_cms_admin_files`, `v3_cms_approve`, `v3_cms_files`는 `DISPLAY_YN=N`, `USE_YN=N`으로 비활성화했다.
- `cmsUser01`은 `v3_cms_manage`, `v3_cms_dashboard`, `v3_cms_edit` 권한만 갖도록 정리했다.
- `cmsAdmin01`은 `v3_cms_manage`와 관리자/결재자용 `v3_cms_admin_approvals`, `v3_cms_admin_deployments`, `v3_cms_admin_statistics`, `v3_cms_admin_ab_tests`, `v3_cms_admin_components` 권한만 갖도록 정리했다.
- 관련 컨트롤러 테스트인 `CmsRedirectControllerTest`, `PageControllerCmsAdminMenuTest`를 수정했다.

#### 권한 예시

```text
cmsUser01:
- v3_cms_manage      W
- v3_cms_dashboard   W
- v3_cms_edit        W

cmsAdmin01:
- v3_cms_manage              W
- v3_cms_admin_approvals     W
- v3_cms_admin_deployments   W
- v3_cms_admin_statistics    W
- v3_cms_admin_ab_tests      W
- v3_cms_admin_components    W

제거 또는 미부여:
- cmsAdmin01 -> v3_cms_admin_pages
- cmsAdmin01 -> v3_cms_edit
- cmsAdmin01 -> v3_cms_system
- cmsUser01  -> v3_cms_admin_*
```

#### 완료 조건

- 제작자 메뉴와 관리자/결재자 메뉴가 URL과 권한 기준에서 분리된다.
- `cmsUser01`은 `/cms/dashboard`에서만 제작/편집/승인 요청을 수행한다.
- `cmsAdmin01`은 `/cms-admin/**`에서 승인/반려/배포/통계/운영 관리만 수행한다.
- `/cms-admin/pages` 메뉴는 더 이상 노출되거나 사용되지 않는다.
- 관리자/결재자에게 `/cms/edit` 또는 제작자 dashboard 편집 권한이 노출되지 않는다.
- 메뉴/권한 변경 후 관련 controller/menu 테스트가 통과한다.

---

### Issue 12. [CMS 이관] 제작자 승인 요청 시 노출 시작일/종료일 설정

#### 선행 이슈

- Issue 1. 관리자 기능 경계와 URL/권한 설계 확정
- Issue 2. CMS 편집 라우팅 책임 정리
- Issue 7. spider-admin에 CMS 관리자/결재자 승인 데이터 연동 구현
- Issue 11. CMS 제작자/관리자 메뉴와 사용자 권한 정리

#### 배경

기존 승인 요청 흐름은 제작자 dashboard와 관리자 승인 화면 사이에서 노출 시작일/종료일 책임이 명확하지 않았다.

앞으로는 페이지 제작자가 `/cms/dashboard`에서 승인 요청 또는 재승인 요청을 보낼 때 노출 시작일과 노출 종료일을 함께 설정한다. 선택한 날짜는 `SPW_CMS_PAGE.BEGINNING_DATE`, `SPW_CMS_PAGE.EXPIRED_DATE`에 저장되어 `PENDING` 상태의 승인 대기 건에서도 확인할 수 있다.

관리자/결재자는 `spider-admin`의 `/cms-admin/approvals` 승인 모달에서 제작자가 요청한 노출 기간을 확인하고, 필요하면 시작일/종료일을 수정한 뒤 최종 승인한다. 최종 노출 기간은 관리자 승인 시점의 입력값으로 확정된다.

#### 참고 코드

```text
CMS Next 제작자 승인 요청 흐름:
- cms-1-innova-next/src/components/dashboard/ApprovalRequestModal.tsx
- cms-1-innova-next/src/components/dashboard/DashboardClient.tsx
- cms-1-innova-next/src/app/api/builder/pages/[pageId]/approve-request/route.ts
- cms-1-innova-next/src/db/repository/page.repository.ts
- cms-1-innova-next/src/db/queries/page.sql.ts

spider-admin 승인/검토 흐름:
- src/main/java/com/example/admin_demo/domain/cmsapproval/controller/CmsApprovalController.java
- src/main/java/com/example/admin_demo/domain/cmsapproval/service/CmsApprovalService.java
- src/main/resources/mapper/oracle/cmsapproval/CmsApprovalMapper.xml
- src/main/resources/templates/pages/cms-approval/cms-approval.html
- src/main/resources/templates/pages/cms-approval/cms-approval-table.html
- src/main/resources/templates/pages/cms-approval/cms-approval-script.html

관련 DB:
- SPW_CMS_PAGE.BEGINNING_DATE
- SPW_CMS_PAGE.EXPIRED_DATE
- SPW_CMS_PAGE.APPROVE_STATE
- SPW_CMS_PAGE.APPROVER_ID
- SPW_CMS_PAGE.APPROVER_NAME
- SPW_CMS_PAGE.CONFIRM_DTIME
- SPW_CMS_PAGE.APPROVE_DATE
- SPW_CMS_PAGE_HISTORY.BEGINNING_DATE
- SPW_CMS_PAGE_HISTORY.EXPIRED_DATE
```

#### next-cms 작업

제작자 승인 요청 흐름은 기존처럼 CMS Next의 `/cms/dashboard`에서 처리한다. 이 작업의 책임은 제작자가 승인 요청 또는 재승인 요청 시 노출 시작일/종료일을 입력하고, 해당 값을 `SPW_CMS_PAGE`에 `PENDING` 상태와 함께 저장하는 것이다.

- [x] `ApprovalRequestModal.tsx`에서 승인 요청 대상과 함께 노출 시작일/노출 종료일을 입력하도록 변경
  - `beginningDate`: 노출 시작일
  - `expiredDate`: 노출 종료일
- [x] 승인 요청 모달을 열 때 시작일 기본값 설정
  - 기존 `BEGINNING_DATE`가 있으면 기존 값을 기본값으로 사용
  - 기존 값이 없으면 승인 요청일 당일을 기본값으로 사용
  - 재승인 요청 시 기존 시작일이 과거여도 선택 가능
  - 종료일은 기존 `EXPIRED_DATE`가 있으면 기존 값을 사용하고, 없으면 사용자가 직접 입력
- [x] 승인 요청 모달 유효성 검증 추가
  - 시작일 필수
  - 종료일 필수
  - 날짜 형식 `YYYY-MM-DD`
  - 종료일은 시작일보다 빠를 수 없음
  - 재승인 요청에서는 기존 과거 시작일을 유지할 수 있음
- [x] `DashboardClient.tsx`의 승인 요청 submit 인자를 `approverId`, `approverName`, `beginningDate`, `expiredDate`로 변경
- [x] `/cms/dashboard` 카드에 노출 시작일/종료일 표시
- [x] `/api/builder/pages/[pageId]/approve-request` 요청 body에 `beginningDate`, `expiredDate` 추가

```json
{
  "approverId": "userAdmin01",
  "approverName": "관리자",
  "beginningDate": "2026-04-20",
  "expiredDate": "2026-05-20"
}
```

- [x] 승인 요청 API에서 `beginningDate`, `expiredDate` 필수값 검증
- [x] 승인 요청 API에서 시작일/종료일 순서 검증
- [x] CMS Next repository/query 수정
  - `requestApproval` 함수 시그니처에 `beginningDate` 추가
  - `PAGE_REQUEST_APPROVAL` SQL에서 `BEGINNING_DATE`, `EXPIRED_DATE` 함께 저장

```sql
UPDATE SPW_CMS_PAGE
SET APPROVE_STATE = 'PENDING',
    APPROVER_ID   = :approverId,
    APPROVER_NAME = :approverName,
    CONFIRM_DTIME = SYSTIMESTAMP,
    BEGINNING_DATE = TO_DATE(:beginningDate, 'YYYY-MM-DD'),
    EXPIRED_DATE  = TO_DATE(:expiredDate, 'YYYY-MM-DD')
WHERE PAGE_ID = :pageId
  AND APPROVE_STATE IN ('WORK', 'REJECTED', 'APPROVED')
```

- [x] 승인된 페이지의 노출 기간 변경 요청을 지원
  - `APPROVED` 상태 페이지도 제작자가 시작일/종료일을 다시 지정해 승인 요청 가능
  - 요청 시 `APPROVE_STATE`를 `PENDING`으로 변경
  - 기존 `PAGE_HTML`, `FILE_PATH`는 유지하고 노출 기간 변경 요청으로 처리
  - 버튼 문구는 `재승인요청`으로 표시
- [x] CMS Next 테스트 추가/수정
  - 제작자 승인 요청 시 시작일/종료일 정상 저장
  - 시작일 누락 시 실패
  - 종료일 누락 시 실패
  - 종료일이 시작일보다 빠르면 실패
  - 기존 `WORK`, `REJECTED`, `APPROVED` 상태에서 승인 요청 가능
  - `APPROVED` 상태 페이지가 노출 기간 변경 요청 후 `PENDING`으로 전환됨
  - 현재까지 `npx tsc --noEmit` 통과 확인

#### spring-spider-admin 작업

관리자/결재자 승인 흐름은 `spider-admin`의 `/cms-admin/approvals`에서 처리한다. 이 작업의 책임은 제작자가 요청한 노출 시작일/종료일을 확인하고, 승인 모달에서 필요 시 최종 노출 기간을 수정한 뒤 승인 확정하는 것이다.

- [x] 승인 목록 API가 `SPW_CMS_PAGE.BEGINNING_DATE`, `EXPIRED_DATE`를 반환하는지 확인
- [x] `/cms-admin/approvals` 화면에서 승인 대기 건의 노출 시작일/종료일 표시 확인
- [x] 승인된 페이지의 노출 기간 변경 요청도 `PENDING` 승인 대기 건으로 표시되는지 확인
- [x] 승인 모달에서 노출 시작일/종료일 수정 가능
  - 기존 `BEGINNING_DATE`, `EXPIRED_DATE`가 있으면 기본값으로 표시
  - 기존 시작일이 과거여도 재승인 요청 건에서는 선택 가능
  - 종료일은 시작일보다 빠를 수 없음
- [x] 승인 처리에서 최종 노출 기간을 함께 저장
  - 승인 API request DTO에 `beginningDate`, `expiredDate` 필수
  - 승인 SQL은 승인 상태, 승인 시각, 노출 시작일/종료일을 함께 변경

```sql
UPDATE SPW_CMS_PAGE
SET APPROVE_STATE = 'APPROVED',
    BEGINNING_DATE = TO_DATE(#{beginningDate}, 'YYYY-MM-DD'),
    EXPIRED_DATE = TO_DATE(#{expiredDate}, 'YYYY-MM-DD'),
    APPROVE_DATE = SYSTIMESTAMP,
    LAST_MODIFIER_ID = :modifierId
WHERE PAGE_ID = :pageId
```

- [x] 승인/반려 후 `SPW_CMS_PAGE_HISTORY`에 현재 노출 시작일/종료일이 스냅샷으로 저장되는지 확인
- [x] 승인 이후 노출 기간 변경은 제작자가 `/cms/dashboard`에서 재승인 요청으로 처리한다는 책임 경계 문서화
- [x] 관리자 승인 화면에서 노출 기간을 최종 수정할 수 있도록 UI 유지
- [x] spring-spider-admin 테스트 추가/수정
  - 관리자 승인 시 입력한 시작일/종료일이 저장됨
  - 재승인 요청 건의 기존 과거 시작일을 유지해도 승인 가능
  - 승인 이력에 시작일/종료일이 저장됨
  - 승인된 페이지의 노출 기간 변경 요청을 승인/반려할 수 있음
  - `CmsApprovalServiceTest`, `CmsApprovalControllerTest` 통과 확인
  - 승인/반려 write 요청이 `FWK_USER_ACCESS_HIS`에 기록되는지는 Issue 10 통합 테스트에서 최종 확인

#### 통합 확인

- [x] 제작자 `cmsUser01` 로그인
- [x] `/cms/dashboard`에서 승인 요청 또는 재승인 요청
- [x] 결재자 선택
- [x] 노출 시작일/종료일 입력
- [x] `/cms/dashboard` 카드에 노출 시작일/종료일 표시 확인
- [x] 관리자/결재자 `cmsAdmin01` 또는 `userAdmin01` 로그인
- [x] `/cms-admin/approvals`에서 요청된 노출 시작일/종료일 확인
- [x] 승인 모달에서 노출 시작일/종료일 수정 후 승인 처리
- [x] `SPW_CMS_PAGE`의 승인 상태와 노출 시작일/종료일 확인
- [x] `SPW_CMS_PAGE_HISTORY`의 승인 이력 스냅샷 확인
- [x] 승인된 페이지에서 제작자가 노출 시작일/종료일을 다시 지정해 승인 요청
- [x] 관리자/결재자가 변경 요청을 승인/반려
- [x] 승인 시 변경된 시작일/종료일이 반영되고, 반려 시 반려 사유가 제작자 dashboard에서 확인되는지 검증

#### 완료 조건

- [x] 제작자는 승인 요청 시 노출 시작일과 노출 종료일을 반드시 입력한다.
- [x] 승인 요청 모달의 노출 시작일 기본값은 기존 값이 있으면 기존 값, 없으면 승인 요청일 당일이다.
- [x] 승인 요청 후 `SPW_CMS_PAGE.APPROVE_STATE`는 `PENDING`이 된다.
- [x] 승인 요청 후 `SPW_CMS_PAGE.BEGINNING_DATE`, `EXPIRED_DATE`에 제작자가 입력한 값이 저장된다.
- [x] 승인된 페이지도 제작자가 노출 시작일/종료일을 다시 지정해 승인 요청할 수 있다.
- [x] `/cms/dashboard` 제작자 화면에서 노출 시작일/종료일을 확인할 수 있다.
- [x] `spider-admin` 승인 관리 화면에서 승인 대기 건의 노출 시작일/종료일을 확인할 수 있다.
- [x] 관리자는 승인 시 노출 시작일/종료일을 최종 수정할 수 있다.
- [x] 승인 완료 후 `SPW_CMS_PAGE_HISTORY`에 노출 시작일/종료일이 함께 저장된다.
- [x] 승인 이후 노출 기간 변경은 제작자 재승인 요청과 관리자/결재자 승인/반려 흐름으로 처리된다.
- [ ] 실제 브라우저에서 제작자 요청 -> 관리자 승인/반려 -> DB 확인까지 수동 통합 검증한다.

#### 고려 사항

- 기존 `PENDING` 데이터 중 `BEGINNING_DATE`, `EXPIRED_DATE`가 비어 있는 건의 처리 정책이 필요하다.
  - 재승인 요청 요구
  - 관리자 승인 시 1회 보정 허용
  - 기본값 보정
- 이미 `PENDING` 상태인 요청의 노출 기간을 제작자가 수정할 수 있는지 정책을 정해야 한다.
  - 기본 방향은 요청 취소 후 재요청 또는 기존 요청 반려 후 재요청
- 관리자 승인 화면에서 노출 기간을 직접 수정할 수 있으므로, 최종 확정값은 관리자 승인 시점의 입력값이라는 기준을 운영자에게 안내해야 한다.
- 승인/반려 작업은 기존처럼 `FWK_USER_ACCESS_HIS`에 기록되어야 한다.

---

## 병렬화 기준

Issue 1, 2, 3은 공통 선행 작업이다. 이 세 가지가 끝나면 아래 작업은 비교적 독립적으로 진행할 수 있다.

- Issue 4: admin 화면 골격
- Issue 5: CMS dashboard의 기존 편집 이동 유지 확인
- Issue 6: CMS 앱 축소
- Issue 8: CMS 리소스 라이프사이클과 Docker volume 경로 정책 정리
- Issue 9: A/B, 통계, 배포 관리 이관
- Issue 10: 프록시와 E2E 갱신
- Issue 11: CMS 제작자/관리자 메뉴와 사용자 권한 정리
- Issue 12: 제작자 승인 요청 시 노출 시작일/종료일 설정

Issue 7은 데이터/API owner 결정과 admin 화면 골격에 의존하므로 Issue 3, 4 이후 착수한다. Issue 11은 Issue 4 이후 바로 진행할 수 있으며, Issue 7~10의 E2E와 권한 검증 기준이 된다.

Issue 12는 승인 요청 데이터 구조와 승인 처리 흐름을 바꾸므로 Issue 7 이후 진행하는 것이 좋다. Issue 10의 E2E를 작성하기 전에 Issue 12를 먼저 끝내면 제작자 승인 요청 -> 관리자 승인/반려 통합 시나리오를 한 번에 검증할 수 있다. Issue 8의 Docker volume 경로 정책과는 직접 충돌하지 않지만, 승인 후 배포 시점의 노출 URL/리소스 검증은 Issue 8, 9와 함께 확인한다.

## 후속 이슈 진행 기준

Issue 8은 현재 확인된 정책과 코드/환경 불일치 지점까지 정리하고 종료한다. Issue 8에서 발견한 남은 작업은 아래 순서로 진행한다.

1. `docs/cms-deploy-serving-followup-issues.md`의 Issue 1, 2를 Issue 9 배포 관리 이관 시작 시점에 먼저 진행한다.
   - 배포 receive URL, `cms.deploy.secret`/`DEPLOY_SECRET`, 배포 HTML 저장 위치, 브라우저 미리보기 URL을 먼저 맞춘다.
   - 이 작업이 끝나야 `/cms-admin/deployments`의 배포 버튼 검증과 Issue 10 E2E가 의미 있게 진행된다.
2. `docs/cms-image-approval-issues.md`의 Issue 1, 2를 이미지 승인 기능 구현 시점에 진행한다.
   - 제작자의 이미지 승인 요청과 `spider-admin` 관리자의 이미지 승인/반려를 먼저 구현한다.
   - 이 작업은 Issue 8 완료 후 바로 시작할 수 있지만, 편집기 업로드 흐름 변경 전에는 승인 이미지 API/상태 기준이 먼저 확정되어야 한다.
3. `docs/cms-image-approval-issues.md`의 Issue 3, 4를 승인 이미지 API 기준이 정해진 뒤 진행한다.
   - CMS 편집기에서 로컬 이미지 즉시 삽입을 막고 승인된 이미지 선택으로 전환한다.
   - `manage/*`, `/cms/files`, `/api/builder/upload` 주석/비활성 처리 사유도 이 시점에 코드에 남긴다.
4. `docs/cms-deploy-serving-followup-issues.md`의 Issue 3을 승인 이미지 URL/저장소 정책 확정 후 진행한다.
   - 배포 HTML 내부 이미지 URL 치환 또는 승인 이미지 복사 정책을 구현한다.
   - 배포 HTML은 열리지만 이미지가 깨지는 경우 이 작업을 Issue 9 안에서 우선 처리한다.
5. `docs/cms-deploy-serving-followup-issues.md`의 Issue 4, 5는 배포 성공 경로가 열린 뒤 진행한다.
   - 배포 이력, 관리자 작업이력, 실패 메시지, 정적 리소스 백업/교체/rollback 기준을 정리한다.
6. Issue 10은 위 후속 이슈의 핵심 경로가 정리된 뒤 최종 갱신한다.
   - 제작자 승인 요청, 관리자 승인, 이미지 사용 제한, 배포, 미리보기 URL 접근, 이미지 로딩까지 하나의 E2E 시나리오로 묶어 검증한다.
