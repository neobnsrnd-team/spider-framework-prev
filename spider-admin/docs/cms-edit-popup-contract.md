# CMS 편집 라우팅 책임 경계

이 문서는 `docs/cms-admin-migration-issues.md`의 Issue 2 확정 산출물이다. Issue 1의 기능 경계는 `docs/cms-admin-boundary-design.md`를 따른다.

## 목표

페이지 제작자는 기존 CMS dashboard(`/cms/dashboard`)에서 페이지 생성, 편집, 승인 요청을 수행한다. 편집 화면은 `cms-1-innova-next`의 `/cms/edit` 라우트이며, 현재 CMS Next에 구현된 기존 라우팅 방식을 유지한다.

`spider-admin`은 관리자/결재자의 승인, 반려, 배포, 운영 관리 작업을 소유한다. 관리자는 제작자가 만든 페이지를 검토하고 승인/반려/운영 관리만 수행하며, 페이지를 생성하거나 수정하지 않는다.

## 라우팅 기준

```text
cmsUser01 같은 페이지 제작자
-> /cms/dashboard
-> /cms/edit

userAdmin01 같은 관리자/결재자
-> spider-admin CMS 승인/운영 화면
-> /cms/edit 진입 없음
```

`/cms/dashboard -> /cms/edit` 이동은 CMS Next 내부 책임이다. `spider-admin`은 이 이동을 새로 구현하지 않는다.

## 유지할 CMS 책임

- `/cms/dashboard`
- `/cms/edit`
- 페이지 제작자의 페이지 생성
- 페이지 제작자의 ContentBuilder 편집
- 페이지 제작자의 승인 요청
- 편집 load/save에 필요한 CMS Next API

## spider-admin 책임

- 관리자/결재자 승인 화면
- 승인
- 반려
- 공개/비공개 처리
- 노출 기간 수정
- 롤백
- 배포 관리
- 관리자 작업이력 기록

## 하지 않을 것

- `spider-admin` 관리자/결재자 화면에서 `/cms/edit`로 이동시키지 않는다.
- `spider-admin` 관리자/결재자 화면에 페이지 생성/수정/편집 버튼을 제공하지 않는다.
- 관리자가 ContentBuilder로 제작자 페이지를 직접 수정하는 흐름을 만들지 않는다.
- CMS 편집 화면에서 승인, 반려, 배포 API를 직접 호출하지 않는다.

## 권한 기준

- `/cms/dashboard` 조회는 `CMS:R` 또는 `CMS:W`를 허용한다.
- `/cms/dashboard`의 페이지 생성, 편집, 승인 요청은 `CMS:W`를 요구한다.
- 관리자/결재자의 승인, 반려, 배포, 운영 관리는 `spider-admin` 관리자 권한 정책을 따른다.

## 후속 구현 기준

- Issue 5는 CMS Next에 이미 구현된 `/cms/dashboard -> /cms/edit` 이동이 유지되는지 확인한다.
- Issue 6은 CMS 앱에서 제작자 dashboard와 편집 런타임을 남기고, 관리자성 화면/API만 정리한다.
- Issue 7은 `spider-admin`에서 관리자/결재자 승인 데이터와 승인/반려 API를 구현한다.
