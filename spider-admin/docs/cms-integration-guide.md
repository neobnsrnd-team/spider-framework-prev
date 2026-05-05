# CMS 통합 가이드

## 목표

최종 목표는 CMS 관리자 기능을 `spider-admin`으로 옮기는 것이다.

`spider-admin`은 관리자 콘솔, 로그인, 권한, 관리자 작업이력 로그, 결재 연동의 기준 시스템으로 유지한다. CMS 관리자/결재자 기능은 기존 `spider-admin`의 메뉴, 권한, 관리자 작업이력, 결재 연동, 배포, 파일 관리, 통계 기능을 재사용할 수 있어야 한다. `cms-1-innova-next`는 페이지 제작자 dashboard(`/cms/dashboard`)와 ContentBuilder 편집기 런타임을 유지한다.

이 문서는 관리자 기능 이관 중에도 유지해야 하는 현재 통합 기준과 전환 규칙을 정리한다. 목표 이관 이슈 계획은 `docs/cms-admin-migration-issues.md`에서 관리하고, Issue 1의 기능 경계와 URL/권한 확정 기준은 `docs/cms-admin-boundary-design.md`, Issue 2의 CMS 편집 라우팅 책임 경계는 `docs/cms-edit-popup-contract.md`, Issue 3의 데이터/API 소유권과 전환 전략은 `docs/cms-data-api-ownership-strategy.md`를 따른다.

관리자 기능 이관 완료 전의 현재 기준:

```text
/login        -> spider-admin
/api/**       -> spider-admin
/cms          -> spider-admin이 기본 CMS 페이지로 리다이렉트
/cms/**       -> cms-1-innova-next
/cms/api/**   -> cms-1-innova-next
```

CMS는 별도의 사용자, 역할, 권한 테이블을 만들지 않는다. 현재 사용자와 권한은 `GET /api/auth/me`로 `spider-admin`에서 읽는다.

관리자 기능 이관 후 목표:

```text
spider-admin        -> CMS 관리자/결재자 화면, 권한, 감사/작업이력, 승인/반려, 배포/파일/통계 관리
cms-1-innova-next   -> 페이지 제작자 dashboard, ContentBuilder 편집기 런타임, 제작자 페이지 생성/편집/승인 요청
```

승인/반려 등 관리자/결재자 작업은 `spider-admin`이 소유해야 기존 관리자 작업이력에 기록된다. 제작자의 승인 요청은 기존 CMS Next와 동일하게 `/cms/dashboard`에서 수행한다. 기존 CMS Next에는 별도 결재/워크플로 API 호출 로직이 없었으므로, 이번 이관 범위에서도 별도 결재/워크플로 API 중계는 만들지 않는다.

## 구현 체크리스트

이 섹션을 작업 체크리스트로 사용한다. 순서대로 완료한다.

### 1. spider-admin 준비

- [ ] 기존 관리자 권한 모델을 유지하기 위해 `AUTHORITY_SOURCE`가 `user-menu`인지 확인한다.
- [ ] CMS 메뉴 행을 `FWK_MENU`에 추가한다.
- [ ] CMS 역할 기본 권한을 `FWK_ROLE_MENU`에 추가한다.
- [ ] 로그인 시점의 실제 CMS 권한이 `FWK_USER_MENU`에 들어가도록 역할 기본 권한을 사용자에게 적용한다.
- [ ] `menu-resource-permissions.yml`이 CMS 메뉴 ID를 `CMS:R`, `CMS:W`에 매핑하는지 확인한다.
- [ ] `GET /api/auth/me`가 로그인 사용자와 권한을 반환하는지 확인한다.
- [ ] `/cms`, `/cms/`가 현재 역할/권한에 따라 리다이렉트되는지 확인한다.

예상 CMS 메뉴 ID:

```text
v3_cms_manage
v3_cms_dashboard
v3_cms_edit
v3_cms_admin_approvals
v3_cms_admin_ab_tests
v3_cms_admin_deployments
v3_cms_admin_statistics
v3_cms_admin_components
```

예상 기본 권한:

```text
Admin 역할:
v3_cms_manage   W
v3_cms_admin_approvals    W
v3_cms_admin_ab_tests     W
v3_cms_admin_deployments  W
v3_cms_admin_statistics   W
v3_cms_admin_components   W

CMS 제작자 역할:
v3_cms_manage     W
v3_cms_dashboard  W
v3_cms_edit       W
```

예상 런타임 권한:

```text
CMS 제작자 사용자 -> CMS:R, CMS:W
관리자/결재자 사용자 -> spider-admin 관리자 권한과 필요한 v3_cms_admin_* 메뉴 권한
```

### 2. cms-1-innova-next 준비

- [ ] Next 앱이 `/cms` 아래에서 실행되도록 설정한다.
- [ ] 현재 전환 단계에서는 `/cms/dashboard`, `/cms/edit` 직접 진입 라우트가 렌더링되는지 확인한다.
- [ ] CMS Next API는 `/cms/api/**` 아래에 둔다.
- [ ] Java API 호출과 Next API 호출이 충돌하지 않도록 API URL helper를 추가한다.
- [ ] 현재 사용자 조회를 `GET /api/auth/me`로 교체한다.
- [ ] 운영 권한 체크에서 `DEMO_USERS`, `cms-token`, `cms-user`를 제거한다.
- [ ] `role !== 'admin'` 같은 역할 문자열 체크를 `CMS:W` 권한 체크로 교체한다.
- [ ] 읽기 페이지와 읽기 API는 `CMS:R` 또는 `CMS:W`를 허용한다.
- [ ] `/cms/dashboard`의 생성, 수정, 삭제, 승인 요청은 `CMS:W`를 요구한다.
- [ ] 관리자/결재자의 승인, 반려, 배포는 `spider-admin` 관리자 권한을 요구한다.
- [ ] CMS 권한이 없는 사용자가 CMS URL에 직접 접근하면 권한 없음 페이지를 보여주거나 리다이렉트한다.
- [ ] `/cms/approve`, `/cms/files` 등 CMS 관리자성 라우트는 전환용 라우트로만 취급한다. 이관 중에는 `docs/cms-admin-migration-issues.md`에 따라 `spider-admin`으로 이동, 리다이렉트, read-only 유지, deprecated 처리 중 하나를 선택한다.
- [ ] `/cms/edit`과 ContentBuilder 런타임 동작은 장기적으로 CMS 책임으로 유지한다.

### 3. 로컬 통합 실행

- [ ] `spider-admin`을 `http://localhost:8080`에서 실행한다.
- [ ] `cms-1-innova-next`를 `http://localhost:3000`에서 실행한다.
- [ ] 로컬 Nginx proxy를 실행한다.

```bash
docker compose --profile admin-proxy up -d admin-proxy
```

- [ ] `http://localhost:9000/login`을 연다.
- [ ] `FWK_USER_MENU`에 CMS 권한이 있는 사용자로 로그인한다.
- [ ] `spider-admin`에서 CMS 메뉴를 클릭한다.
- [ ] `cmsUser01` 같은 페이지 제작자는 `/cms/dashboard`로 이동하는지 확인한다.
- [ ] `userAdmin01` 같은 관리자/결재자는 `spider-admin`의 CMS 승인/운영 화면으로 이동하는지 확인한다.
- [ ] CMS가 `/api/auth/me`를 호출하고 `CMS:R` 또는 `CMS:W`를 받는지 확인한다.

### 4. 하지 말 것

- [ ] CMS 사용자, 역할, 권한 테이블을 별도로 만들지 않는다.
- [ ] 운영 권한 체크에 `DEMO_USERS`, `cms-token`, `cms-user`를 사용하지 않는다.
- [ ] 브라우저 코드에 `http://localhost:8080/api/auth/me`를 하드코딩하지 않는다. proxy를 통한 `/api/auth/me` 상대 경로를 사용한다.
- [ ] 현재 로컬 CMS 통합 테스트에서는 `http://localhost:8080/login`을 사용하지 않는다. `http://localhost:9000/login`을 사용한다.
- [ ] 승인, 반려, 배포, 관리자 작업이력 책임이 `spider-admin`으로 이관된 후에는 CMS 편집 화면이 해당 작업을 직접 소유하지 않는다.

## 권한 모델

기존 `spider-admin` 권한 모델을 유지한다.

```text
FWK_ROLE_MENU = 역할별 기본 권한
FWK_USER_MENU = 로그인 시 실제로 사용하는 사용자 권한
menu-resource-permissions.yml = 메뉴 R/W 권한을 Spring Security authority로 매핑
```

현재 기본값:

```yaml
app:
  security:
    authority-source: ${AUTHORITY_SOURCE:user-menu}
```

이 설정은 `AUTHORITY_SOURCE`를 변경하지 않는 한 로그인 시점의 권한을 `FWK_USER_MENU`에서 읽는다는 뜻이다.

CMS 기본 권한은 `FWK_ROLE_MENU`에서 역할별로 관리한다. 이후 기존 admin 권한 관리 흐름을 통해 역할 기본 권한을 실제 사용자에게 적용해 `FWK_USER_MENU`에 CMS 권한이 들어가게 한다.

첫 단계에서 CMS는 아래 두 권한만 사용한다.

```text
CMS:R = CMS 읽기/접근
CMS:W = CMS 페이지 제작/편집/승인 요청
```

기존 converter는 write 권한이 있으면 R 매핑을 먼저 추가하고 W 매핑을 추가한다. 따라서 CMS 메뉴에 `W`가 있는 사용자는 `CMS:R`과 `CMS:W`를 모두 가진다.

## spider-admin 설정

CMS 메뉴를 `FWK_MENU`에 등록한다.

```text
v3_cms_manage   -> CMS 상위 메뉴
v3_cms_dashboard -> /cms/dashboard
v3_cms_edit     -> /cms/edit
```

Issue 4 이후 `spider-admin` 관리자 화면 골격은 별도 `/cms-admin/**` 메뉴를 사용한다. 기존 `/cms/**` 메뉴는 CMS 런타임 전환 경로로 유지한다.

```text
v3_cms_admin_approvals    -> /cms-admin/approvals
v3_cms_admin_ab_tests     -> /cms-admin/ab-tests
v3_cms_admin_deployments  -> /cms-admin/deployments
v3_cms_admin_statistics   -> /cms-admin/statistics
v3_cms_admin_components   -> /cms-admin/components
```

`v3_cms_admin_pages`는 제작자 dashboard를 `/cms-admin/pages`로 이관하지 않으므로 사용하지 않는다. `v3_cms_approve`, `v3_cms_files`는 전환 기간에만 원본 확인용으로 둘 수 있고, 이관 후에는 redirect, read-only, deprecated, 비활성 중 하나로 정리한다.

CMS 메뉴 권한을 `menu-resource-permissions.yml`에 등록한다.

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

역할 기본 권한은 `FWK_ROLE_MENU`에서 관리한다.

```text
Admin 역할:
v3_cms_manage   W
v3_cms_admin_approvals    W
v3_cms_admin_ab_tests     W
v3_cms_admin_deployments  W
v3_cms_admin_statistics   W
v3_cms_admin_components   W

CMS 제작자 역할:
v3_cms_manage     W
v3_cms_dashboard  W
v3_cms_edit       W
```

실제 로그인 사용자도 `FWK_USER_MENU`에 CMS 메뉴 권한을 가져야 한다. 기존 admin 권한 관리 흐름으로 역할 기본 권한을 사용자에게 적용한다.

## CMS 설정

### 1. `/cms` 아래에서 실행

Next 앱을 `/cms` 아래에서 실행되도록 설정한다.

```ts
// next.config.ts
const nextConfig = {
  basePath: '/cms',
};

export default nextConfig;
```

CMS가 이미 `/cms/dashboard`를 제공한다면 페이지 제작자 화면은 기존 설정을 유지한다. 기존 `/cms/approve`는 관리자성 화면이므로 `spider-admin` 승인/운영 화면으로 이관한 뒤 redirect, read-only, deprecated 처리 중 하나로 정리한다.

### 2. API 경로 분리

CMS Next API는 `/cms/api/**`를 사용하고, `spider-admin` Java API는 `/api/**`를 사용한다.

```text
Next API: /cms/api/builder/save
Java API: /api/auth/me
```

권장 helper:

```ts
export function nextApi(path: string) {
  return `/cms${path}`;
}

export function javaApi(path: string) {
  return path;
}
```

예시:

```ts
fetch(nextApi('/api/builder/save'));
fetch(javaApi('/api/auth/me'), { credentials: 'include' });
```

### 3. 현재 사용자 조회 교체

운영 권한 체크에서 `DEMO_USERS`, `cms-token`, `cms-user`를 사용하지 않는다.

`spider-admin`을 사용한다.

```ts
const response = await fetch('/api/auth/me', {
  credentials: 'include',
});

if (response.status === 401) {
  throw new Error('UNAUTHENTICATED');
}

const body = await response.json();
const currentUser = body.data;
```

Next API route 또는 server component가 `spider-admin`을 호출해야 한다면 들어온 `Cookie` 헤더를 전달한다.

```ts
const cookie = request.headers.get('cookie') ?? '';

const response = await fetch(`${process.env.SPIDER_ADMIN_BASE_URL}/api/auth/me`, {
  headers: { cookie },
});
```

로컬 proxy 테스트에서는 `SPIDER_ADMIN_BASE_URL`을 `http://localhost:9000`으로 둘 수 있다.

### 4. 역할 체크를 권한 체크로 교체

운영 코드에서 `role !== 'admin'` 같은 체크를 제거한다.

```ts
function hasAuthority(user: CurrentUser, authority: string) {
  return user.authorities.includes(authority);
}

function canReadCms(user: CurrentUser) {
  return hasAuthority(user, 'CMS:R') || hasAuthority(user, 'CMS:W');
}

function canWriteCms(user: CurrentUser) {
  return hasAuthority(user, 'CMS:W');
}
```

읽기 라우트와 페이지는 `CMS:R` 또는 `CMS:W`를 요구한다.

```ts
if (!canReadCms(user)) {
  return errorResponse('권한이 없습니다.', 403);
}
```

`/cms/dashboard`의 생성, 수정, 삭제, 승인 요청은 `CMS:W`를 요구한다. 승인, 반려, 배포는 `spider-admin` 관리자 권한을 요구한다.

```ts
if (!canWriteCms(user)) {
  return errorResponse('권한이 없습니다.', 403);
}
```

UI 버튼은 같은 권한 체크로 숨기거나 비활성화할 수 있다. 최종 권한 검사는 반드시 Next API route 또는 Java API에서도 수행해야 한다.

### 5. 라우트 처리

`spider-admin`은 `/cms`, `/cms/` 리다이렉트를 처리한다.

```text
cmsUser01 같은 페이지 제작자 -> /cms/dashboard
userAdmin01 같은 관리자/결재자 -> spider-admin CMS 승인/운영 화면
```

전환 기간 동안 CMS는 제작자 라우트를 렌더링할 수 있어야 한다. 관리자성 라우트는 원본 확인이 필요할 때만 임시로 유지하고, 이관 후 redirect, read-only, deprecated, 비활성 중 하나로 정리한다.

```text
/cms/dashboard
/cms/edit

전환용 후보:
/cms/approve
/cms/files
```

CMS 권한이 없는 사용자가 CMS URL을 직접 열면 권한 없음 페이지를 보여주거나 admin home으로 리다이렉트한다. 빈 화면을 렌더링하지 않는다.

## 로컬 통합 테스트

아래 세 프로세스를 실행한다.

```text
spider-admin: http://localhost:8080
cms-1-innova-next: http://localhost:3000
admin-proxy: http://localhost:9000
```

이 저장소에서 proxy를 실행한다.

```bash
docker compose --profile admin-proxy up -d admin-proxy
```

브라우저에서는 아래 URL을 사용한다.

```text
http://localhost:9000/login
```

현재 통합 테스트에서는 `http://localhost:8080/login`을 사용하지 않는다. `/cms/**`가 proxy를 우회하게 되기 때문이다.

관리자 기능 이관 중/이관 후에는 아래 흐름을 구분한다.

```text
CMS 관리자 화면 -> spider-admin 로그인과 spider-admin 라우트에서 동작해야 한다.
CMS 제작자 화면 -> /cms/dashboard와 /cms/edit은 CMS Next 내부 라우팅으로 동작한다.
```

## 인수 확인 항목

- `GET /api/auth/me`가 로그인 사용자와 `CMS:R` 또는 `CMS:W`를 반환한다.
- `cmsUser01` 같은 페이지 제작자는 `/cms`에서 `/cms/dashboard`로 이동한다.
- `userAdmin01` 같은 관리자/결재자는 `spider-admin`의 CMS 승인/운영 화면으로 이동한다.
- CMS 메뉴 클릭 시 admin 내부 tab fragment가 아니라 `/cms/**`로 이동한다.
- `CMS:R` 사용자는 읽을 수 있지만 `/cms/dashboard`에서 생성, 수정, 삭제, 승인 요청은 할 수 없다.
- `CMS:W` 사용자는 `/cms/dashboard`에서 페이지 제작, 편집, 승인 요청을 수행할 수 있다.
- 운영 권한 체크는 `DEMO_USERS`, `cms-token`, `cms-user`를 사용하지 않는다.
- `spider-admin`으로 이관된 관리자 작업은 관리자 작업이력에서 확인할 수 있다.
- 이관 후 CMS 편집 화면은 승인, 반려, 배포, 관리자 작업이력 기록 작업을 직접 수행하지 않는다.
