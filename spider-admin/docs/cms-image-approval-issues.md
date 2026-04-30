# CMS 이미지 승인 분리 이슈

## 목표

CMS 페이지 제작자가 `/cms/**` 제작 화면에서 사용하는 이미지는 관리자 승인 완료 이미지로 제한한다. 이미지 승인 업무는 관리자 영역이며, 승인/반려/이력 관리는 `spider-admin`에서 수행한다.

현재 CMS Next 편집 화면은 로컬 PC 이미지를 업로드하면 바로 페이지에 적용할 수 있는 경로가 남아 있다. 이 경로를 "이미지 승인 요청"과 "승인된 이미지 선택" 흐름으로 분리해야 한다.

## 확정 기준

- 이미지 승인은 관리자 업무다.
- 관리자 계정 예시는 `userAdmin01`이며, 승인 화면/API owner는 `spider-admin`이다.
- 제작자 계정 예시는 `cmsUser01`이며, `/cms/**`에서 페이지를 제작한다.
- 제작자는 관리자 승인을 받은 이미지만 페이지 제작에 사용할 수 있다.
- 결재 전 이미지 리소스는 영속 저장한다.
- 미승인 이미지는 승인 전용 저장소에 남기고, 승인 완료 이미지는 배포/사용 저장소로 복사한다.
- 이미지 업로드/승인 요청과 페이지 제작 중 이미지 사용은 서로 다른 동작으로 분리한다.

## 현재 코드에서 확인된 경로

### 즉시 업로드 경로

```text
cms-1-innova-next/src/components/edit/EditClient.tsx
  -> ContentBuilder upload handler에서 /api/builder/upload 호출

cms-1-innova-next/src/app/api/builder/upload/route.ts
  -> 로컬 파일을 업로드하고 URL을 반환
```

이 경로는 현재 로컬 PC 이미지를 바로 업로드해 편집 화면에 적용할 수 있으므로, 승인된 이미지만 사용할 수 있다는 정책과 충돌한다.

### 컴포넌트별 직접 업로드 경로

```text
cms-1-innova-next/src/components/edit/AuthCenterIconEditor.tsx
cms-1-innova-next/src/components/edit/ProductMenuIconEditor.tsx
cms-1-innova-next/src/components/edit/SlideEditorModal.tsx
  -> input type=file 생성 후 /api/builder/upload 호출
```

편집기 본체 외에도 일부 컴포넌트 에디터가 직접 파일 선택과 업로드를 수행한다. 승인 정책 적용 시 이 경로들도 같이 분리해야 한다.

### 기존 파일/에셋 관리 후보

```text
cms-1-innova-next/src/app/files/page.tsx
cms-1-innova-next/src/app/api/manage/folders/route.ts
cms-1-innova-next/src/app/api/manage/files/route.ts
cms-1-innova-next/src/app/api/manage/upload/route.ts
cms-1-innova-next/src/app/api/manage/delete/route.ts
cms-1-innova-next/src/app/api/manage/addfolder/route.ts

cms-1-innova-next/src/app/api/assets/route.ts
cms-1-innova-next/src/app/api/assets/[assetId]/route.ts
cms-1-innova-next/src/app/api/assets/[assetId]/image/route.ts
```

`manage/*`는 폴더/파일 관리 API이고, `assets/*`는 `SPW_CMS_ASSET` 기반 에셋 메타데이터 API다. 이미지 승인 분리 시 둘 중 어떤 경로를 유지/전환/폐기할지 결정해야 한다.

## 용어 정리

- 이미지 승인 요청: 제작자 또는 현업 사용자가 이미지를 업로드하고 관리자 승인 대기 상태로 등록하는 동작
- 이미지 승인 관리: 관리자가 승인 대기 이미지를 검토하고 승인/반려하는 동작
- 승인된 이미지 선택: 페이지 제작 중 이미 승인된 이미지만 목록에서 골라 사용하는 동작
- 즉시 업로드: 로컬 PC 파일을 선택한 즉시 페이지 HTML에 URL을 삽입하는 기존 동작

## Issue 1. 제작자 이미지 승인 요청 화면/API 추가

### 배경

제작자는 `/cms/**` 제작 화면에서 페이지를 만들지만, 사용할 이미지는 관리자 승인을 받아야 한다. 따라서 `/cms/**` 영역에 "이미지 승인 요청" 화면 또는 모달이 필요하다.

### 작업

- [ ] `/cms/**`에서 접근 가능한 이미지 승인 요청 화면 위치 결정
  - 예: `/cms/assets/request`
  - 예: `/cms/dashboard` 내 이미지 승인 요청 모달
- [ ] 제작자 이미지 업로드 요청 API 설계
  - 업로드 파일
  - 이미지명
  - 업무 카테고리
  - 설명
  - 요청자 ID/이름
- [ ] 업로드된 이미지를 승인 대기 상태로 저장
  - `SPW_CMS_ASSET.ASSET_STATE = 'PENDING'`
  - 물리 파일은 결재 전 저장소에 영속 저장
- [ ] 요청 후 제작 화면에 바로 삽입하지 않도록 차단
- [ ] 반려 사유 확인 또는 재요청 흐름 설계

### 완료 조건

- 제작자는 로컬 이미지를 바로 페이지에 삽입하지 못한다.
- 제작자는 이미지를 승인 요청으로 등록할 수 있다.
- 승인 전 이미지는 페이지 제작용 이미지 선택 목록에 나오지 않는다.

## Issue 2. spider-admin 이미지 승인 관리 구현

### 배경

이미지 승인은 관리자 영역이다. `spider-admin`에서 승인 대기 이미지를 조회하고 승인/반려해야 한다.

### 작업

- [ ] `spider-admin`에 이미지 승인 관리 메뉴/화면 추가
  - 예: `/cms-admin/assets`
  - 메뉴명 예: `CMS 이미지 승인 관리`
- [ ] 승인 대기 이미지 목록 API 구현
  - 검색어
  - 업무 카테고리
  - 승인 상태
  - 요청자
  - 요청일
- [ ] 이미지 미리보기 구현
- [ ] 승인 처리 구현
  - `SPW_CMS_ASSET.ASSET_STATE = 'APPROVED'`
  - 결재 전 저장소의 물리 파일을 배포/사용 저장소로 복사
- [ ] 반려 처리 구현
  - `SPW_CMS_ASSET.ASSET_STATE = 'REJECTED'`
  - 반려 사유 저장 위치 결정 필요
- [ ] 승인/반려 작업이 `FWK_USER_ACCESS_HIS` 관리자 작업이력에 기록되는지 확인
- [ ] controller/service/mapper 테스트 추가

### 완료 조건

- `userAdmin01` 같은 관리자는 `spider-admin`에서 이미지 승인/반려를 수행한다.
- 승인된 이미지만 제작자가 사용할 수 있는 상태가 된다.
- 이미지 승인/반려 작업이 관리자 작업이력에 남는다.

## Issue 3. 페이지 제작 화면의 이미지 사용 경로 분리

### 배경

현재 편집기와 일부 컴포넌트 에디터는 로컬 파일을 바로 `/api/builder/upload`로 업로드해 페이지에 적용한다. 이 방식은 "승인된 이미지만 사용" 정책과 충돌한다.

### 작업

- [ ] ContentBuilder 기본 upload handler 동작 변경
  - 기존 즉시 업로드 제거 또는 승인 요청 흐름으로 전환
- [ ] 컴포넌트별 직접 업로드 버튼 점검
  - `AuthCenterIconEditor`
  - `ProductMenuIconEditor`
  - `SlideEditorModal`
- [ ] 페이지 제작 중에는 승인된 이미지 선택 UI만 제공
- [ ] 승인된 이미지 목록 API 설계
  - `ASSET_STATE = 'APPROVED'`
  - `USE_YN = 'Y'`
  - 업무 카테고리 필터
- [ ] 기존 페이지 HTML에 미승인 `/uploads/**` 이미지가 들어가는지 검증
- [ ] 저장/승인 요청 시 페이지 HTML 안의 이미지가 승인된 에셋인지 검증

### 완료 조건

- 제작자는 페이지 제작 중 로컬 이미지를 즉시 삽입할 수 없다.
- 제작자는 승인된 이미지 목록에서만 이미지를 선택한다.
- 페이지 저장 또는 승인 요청 시 미승인 이미지가 포함되면 차단된다.

## Issue 4. 기존 파일/에셋 API 처리 방향 결정

### 배경

기존 CMS Next에는 파일 관리와 에셋 관리 API가 섞여 있다. 이미지 승인 분리 후에는 각 API의 owner와 동작을 명확히 해야 한다.

### 처리 대상

```text
파일/폴더 관리:
- /api/manage/folders
- /api/manage/files
- /api/manage/upload
- /api/manage/delete
- /api/manage/addfolder
- /cms/files

에셋 메타데이터:
- /api/assets
- /api/assets/{assetId}
- /api/assets/{assetId}/image

편집기 즉시 업로드:
- /api/builder/upload
```

### 결정 후보

- `/api/manage/*`: 일반 파일/폴더 관리 기능이므로 삭제하지 않고 주석 처리 또는 비활성 처리한다.
  - 사유: 승인된 이미지만 제작에 사용해야 하므로 임의 파일 업로드/삭제/폴더 생성 경로를 막아야 한다.
  - 사유: 기존 코드 참조와 전환 이력을 남겨 이후 승인 이미지 관리 흐름으로 재사용할 수 있는 부분을 추적한다.
  - 사유: 물리 리소스 저장소가 `/data/uploads`와 `/data/deployed`로 분리되므로 기존 파일 브라우저 방식과 책임이 맞지 않는다.
- `/cms/files`: 현재 spider-admin 구현대로 별도 화면을 만들지 않고 `/cms-admin/files`는 `/cms-admin/approvals` redirect를 유지한다.
- `/api/assets/*`: 승인된 이미지 목록/승인 요청 기반으로 재정의하거나 read-only 조회로 축소
- `/api/builder/upload`: 즉시 페이지 삽입용 업로드가 아니라 이미지 승인 요청 업로드로 전환하거나 별도 API로 분리

### 확인 질문

- 이미지 승인 요청 API를 기존 `/api/assets`에 얹을지, 새 `/api/cms/assets/approval-requests` 형태로 분리할지 결정이 필요하다.
- `/api/assets/*`를 이미지 승인 요청/승인 이미지 조회 API로 재정의할지, 새 API로 분리하고 기존 API는 주석/비활성 처리할지 결정이 필요하다.
- 반려된 이미지 파일을 영속 보관할지, 일정 기간 뒤 정리할지 결정이 필요하다.

## Issue 5. 리소스 저장소와 보관 정책 정리

### 확정

- 결재 전 리소스는 영속 저장한다.
- 미승인/승인대기 이미지는 결재 전 저장소에 보관한다.
- 승인 완료 이미지는 결재 후 저장소로 복사한다.
- 제작 화면에서는 승인 완료 저장소 또는 승인 완료 URL만 참조한다.

### 추가 결정 필요

- 반려 이미지 보관 기간
- 승인 취소 또는 재승인 시 기존 파일 처리
- 동일 파일 중복 업로드 시 deduplication 여부
- 승인 이미지 교체 시 기존 페이지 영향 범위
- 정리 배치 기준

## 병렬 진행 기준

- Issue 1과 Issue 2는 화면/API owner가 달라 병렬 진행 가능하다.
- Issue 3은 Issue 1의 승인 요청 API와 Issue 2의 승인 완료 상태 기준이 정해진 뒤 진행하는 것이 안전하다.
- Issue 4는 Issue 1~3의 API 설계와 함께 확정해야 한다.
- Issue 5는 운영 보관 정책이 필요하므로 별도 확인 후 진행한다.
