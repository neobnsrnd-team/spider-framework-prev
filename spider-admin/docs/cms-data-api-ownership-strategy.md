# CMS 데이터/API 소유권 및 전환 전략

이 문서는 `docs/cms-admin-migration-issues.md`의 Issue 3 확정 산출물이다. Issue 1의 기능 경계는 `docs/cms-admin-boundary-design.md`, Issue 2의 CMS 편집 라우팅 책임 경계는 `docs/cms-edit-popup-contract.md`를 따른다.

## 기본 전제

- `spider-admin`과 `cms-1-innova-next`는 같은 업무 DB를 사용한다.
- 따라서 이관의 1차 작업은 DB 데이터 이동이 아니라 기능별 API owner와 write 경로를 명확히 나누는 코드 전환이다.
- 단, TypeScript repository SQL을 Java/MyBatis mapper로 그대로 복사할 수는 없다. Oracle SQL, 트랜잭션 경계, DTO 필드명, MyBatis parameter/result mapping, 작업이력 기록을 `spider-admin` 방식으로 재작성해야 한다.
- 기존 CMS 동작은 이슈에 적힌 CMS 원본 API/DB 코드를 기준으로 유지한다.
- 페이지 제작자 작업은 `/cms/dashboard`에서 유지한다. 페이지 생성, 편집, 제작자의 승인 요청 UI는 CMS 제작자 흐름에 남긴다.
- 관리자/결재자 write 작업은 `spider-admin` API만 수행하고 `FWK_USER_ACCESS_HIS` 관리자 작업이력에 남긴다.
- ContentBuilder 편집 load/save는 CMS Next API가 유지한다. 저장 owner 이관은 후속 검토로 둔다.

## API Owner

| 기능 | 기존 CMS 원본 | 최종 owner | 최종 API | 전환 기간 호출 방향 | write 경로 |
| --- | --- | --- | --- | --- | --- |
| 제작자 페이지 목록/상세 | `app/api/builder/pages/route.ts`, `page.repository.ts`, `page.sql.ts` | `cms-1-innova-next` | 기존 `/cms/api/builder/pages` | `/cms/dashboard` -> CMS Next API -> 같은 DB | CMS |
| 관리자/결재자 검토 목록/상세 | `app/api/builder/pages/route.ts`, `page.repository.ts`, `page.sql.ts` | `spider-admin` | `GET /api/cms-admin/approvals`, `GET /api/cms-admin/pages/{pageId}` | `spider-admin` 승인 화면 -> `spider-admin` DB 직접 조회 | read-only |
| 페이지 생성 | `app/api/builder/pages/route.ts`, `page.repository.ts`, `page.sql.ts` | `cms-1-innova-next` | 기존 `/cms/api/builder/pages` | `/cms/dashboard` -> CMS Next API | CMS |
| 페이지 삭제 | `app/api/builder/pages/route.ts`, `page.repository.ts`, `page.sql.ts` | `cms-1-innova-next` | 기존 `/cms/api/builder/pages` | `/cms/dashboard` -> CMS Next API | CMS |
| 승인 요청 | `app/api/builder/pages/[pageId]/approve-request/route.ts` | `cms-1-innova-next` dashboard UI | 기존 CMS API 유지. 기존 CMS Next에 별도 결재/워크플로 API 호출이 없었으므로 중계 API는 만들지 않음 | `/cms/dashboard` -> CMS Next API | CMS |
| 승인 | `app/api/builder/pages/[pageId]/approve/route.ts` | `spider-admin` | `POST /api/cms-admin/pages/{pageId}/approval/approve` | admin 화면 -> `spider-admin` | `spider-admin` |
| 반려 | `app/api/builder/pages/[pageId]/reject/route.ts` | `spider-admin` | `POST /api/cms-admin/pages/{pageId}/approval/reject` | admin 화면 -> `spider-admin` | `spider-admin` |
| 공개 상태 변경 | `app/api/builder/pages/[pageId]/set-public/route.ts` | `spider-admin` | `PATCH /api/cms-admin/pages/{pageId}/public-state` | admin 화면 -> `spider-admin` | `spider-admin` |
| 노출 기간 수정 | `app/api/builder/pages/[pageId]/update-dates/route.ts` | `spider-admin` | `PATCH /api/cms-admin/pages/{pageId}/display-period` | admin 화면 -> `spider-admin` | `spider-admin` |
| 승인 이력 조회 | `app/api/builder/pages/[pageId]/history/route.ts`, `page-history.sql.ts` | `spider-admin`, CMS dashboard read 허용 | `GET /api/cms-admin/pages/{pageId}/approval-history` 또는 기존 CMS read API | 관리자 화면 -> `spider-admin`, 제작자 dashboard -> read-only 조회 | read-only |
| 롤백 | `app/api/builder/pages/[pageId]/rollback/route.ts` | `spider-admin` | `POST /api/cms-admin/pages/{pageId}/rollback` | admin 화면 -> `spider-admin` | `spider-admin` |
| 편집 load | `app/api/builder/load/route.ts` | `cms-1-innova-next` | `/cms/api/builder/load` | `/cms/edit` -> CMS Next API -> 같은 DB | CMS |
| 편집 save | `app/api/builder/save/route.ts` | `cms-1-innova-next` | `/cms/api/builder/save` | `/cms/edit` -> CMS Next API -> 같은 DB | CMS |
| A/B 그룹 관리 | `app/api/builder/ab/route.ts` | `spider-admin` | `/api/cms-admin/ab-tests` | admin 화면 -> `spider-admin` | `spider-admin` |
| A/B 승격 | `app/api/builder/ab/promote/route.ts` | `spider-admin` | `POST /api/cms-admin/ab-tests/{abTestId}/promote` | admin 화면 -> `spider-admin` | `spider-admin` |
| 조회/클릭 통계 | `app/api/track/stats/route.ts`, `page-view-log.repository.ts`, `page-view-log.sql.ts` | `spider-admin` | `GET /api/cms-admin/statistics` | admin 화면 -> `spider-admin` | read-only |
| 배포 push | `app/api/deploy/push/route.ts`, `file-send.repository.ts`, `file-send.sql.ts` | `spider-admin` | `POST /api/cms-admin/deployments/push` | admin 화면 -> `spider-admin` | `spider-admin` |
| 배포 이력 | `app/api/deploy/history/route.ts`, `file-send.repository.ts`, `file-send.sql.ts` | `spider-admin` | `GET /api/cms-admin/deployments` | admin 화면 -> `spider-admin` | read-only |
| 배포 receive | `app/api/deploy/receive/route.ts` | 배포 수신 런타임 | `/cms/api/deploy/receive` 또는 별도 deploy receiver | 운영 배포 토폴로지에 묶어 Issue 9에서 최종 위치 확정 | receiver |
| 파일 관리 | `app/api/manage/*` | 제거/비활성/read-only 후보 | `spider-admin` 신규 관리 API는 만들지 않음 | Issue 8에서 Docker 정적 리소스 정책으로 확정 | 이관 보류 |
| 에셋 관리 | `app/api/assets/*`, `asset.repository.ts`, `asset.sql.ts` | 보류 | 1단계 참조 전용 또는 비활성 | Issue 8에서 `SPW_CMS_ASSET`와 Docker 정적 리소스 충돌 여부 확정 | 이관 보류 |
| 컴포넌트 카탈로그/매핑 | `component.repository.ts`, `component-map.sql.ts` | `spider-admin` | `/api/cms-admin/components` | admin 화면 -> `spider-admin` | `spider-admin` |

## DB 소유권

같은 DB를 사용하므로 물리 데이터 복제는 하지 않는다. owner는 “어느 애플리케이션 코드가 해당 테이블에 write할 수 있는가”를 의미한다.

| DB 객체 | 최종 write owner | CMS 원본 SQL | 전환 기준 |
| --- | --- | --- | --- |
| `SPW_CMS_PAGE` | 기능별 분리: 제작자 write는 CMS, 관리자/결재자 write는 `spider-admin` | `page.sql.ts` | 페이지 생성/삭제/편집/승인 요청은 `/cms/dashboard` 흐름 유지. 승인, 반려, 공개 상태, 노출 기간, 롤백 같은 관리자/결재자 write는 admin으로 이관 |
| `SPW_CMS_PAGE_HISTORY` | `spider-admin` | `page.sql.ts`, `page-history.sql.ts` | 승인 이력, 롤백 이력 write는 admin으로 이관 |
| 페이지-컴포넌트 매핑 테이블 | 1단계 CMS, 2단계 검토 | `component-map.sql.ts` | 실제 테이블명은 CMS 원본 SQL 기준으로 확인한다. 편집 저장과 강하게 결합되어 있으면 1단계 CMS owner 유지 |
| `SPW_CMS_ASSET` | 보류 | `asset.sql.ts` | DB 기반 에셋과 Docker 정적 리소스 정책 충돌 여부를 Issue 8에서 결정. 그 전에는 신규 admin write API를 만들지 않음 |
| `SPW_CMS_PAGE_VIEW_LOG` | `spider-admin` 조회 owner, 수집 write는 tracker/runtime | `page-view-log.sql.ts` | 통계 조회는 admin. 실제 view/click 수집 write 주체는 tracker/runtime 유지 가능 |
| `FWK_CMS_FILE_SEND_HIS` | `spider-admin` | `file-send.sql.ts` | 배포 push/history 관리와 작업이력 연결을 admin으로 이관 |
| `FWK_CMS_SERVER_INSTANCE` | `spider-admin` | `server.sql.ts` | 배포 대상 서버 조회/관리 owner는 admin |
| 기존 `FWK_COMPONENT`, `FWK_COMPONENT_PARAM` | 기존 `spider-admin` component domain | 현 `spider-admin` mapper | CMS 컴포넌트 카탈로그와 같은 개념인지 확인 후 재사용 여부 결정. 별도 CMS 매핑 테이블은 `component-map.sql.ts` 기준으로 구분 |

## 호출 방향

| 방향 | 허용 여부 | 기준 |
| --- | --- | --- |
| admin 화면 -> `spider-admin` API | 허용 | 최종 기본 경로 |
| `spider-admin` API -> 동일 DB 직접 접근 | 허용 | CMS와 같은 DB를 사용하므로 mapper/service 전환 중심으로 구현 |
| CMS dashboard -> CMS Next API | 허용 | 제작자 페이지 목록/생성/삭제/승인 요청 UI는 기존 CMS 제작자 흐름을 유지 |
| CMS dashboard -> `spider-admin` API | 제한 허용 | 현재 사용자/권한 조회 등 공통 인증 확인에 한정. 승인 요청 write는 CMS Next API 유지 |
| CMS 편집 화면 -> CMS Next API | 허용 | `/cms/api/builder/load`, `/cms/api/builder/save` 허용 |
| CMS 편집 화면 -> `spider-admin` API | 제한 허용 | `/api/auth/me` 정도만 허용. 승인, 반려, 배포 API 직접 호출 금지 |
| admin 화면 -> CMS Next API | 원칙적 금지 | 승인/반려/배포/통계 관리는 admin API 사용 |
| CMS Next API -> admin API | 제한 허용 | 현재 사용자/권한/결재자 목록 조회 같은 read 성격 호출에 한정. 편집 load/save와 제작자 승인 요청 write는 CMS Next API에서 처리 |

## 작업이력 기록 기준

`spider-admin`으로 이관된 관리자성 API는 기존 `RequestTraceInterceptor`, `RdbAccessLogListener`, `AdminActionLogMapper.xml` 흐름을 통해 `FWK_USER_ACCESS_HIS`에 남긴다.

필수 기록 대상:

- 승인
- 반려
- 공개 상태 변경
- 노출 기간 수정
- 롤백
- A/B 변경/승격
- 배포 push
- 파일/에셋 write 작업이 Issue 8 이후 admin owner로 확정되는 경우 해당 write 작업

제작자 dashboard에서 수행하는 페이지 생성/삭제/편집/승인 요청은 관리자 작업이력 필수 대상이 아니다. 기존 CMS Next에 별도 결재/워크플로 API 호출이 없었으므로 승인 요청은 `/cms/dashboard`의 CMS Next API 흐름을 유지한다.

편집 저장:

- CMS Next save API 유지.
- 저장 자체의 감사가 필요해지면 별도 callback API를 정의한다.
- 2단계: 저장 API를 admin으로 이관하는 경우 `FWK_USER_ACCESS_HIS` 기록 필수.

## 중복 Write 방지

| 단계 | 원칙 | CMS Next 처리 |
| --- | --- | --- |
| 0단계 현재 | CMS 기존 API 유지 | 기존 동작 유지 |
| 1단계 admin 관리 API 도입 | 관리자/결재자 write는 `spider-admin`만 수행 | `approve`, `reject`, `set-public`, `update-dates`, `rollback`, `deploy/push`는 deprecated 또는 410/redirect 처리. 제작자 승인 요청은 `/cms/dashboard` 유지 |
| 1단계 편집 API | 편집 load/save는 CMS만 수행 | `/cms/api/builder/load`, `/cms/api/builder/save` 유지 |
| 2단계 저장 API 검토 | 저장 API도 admin 이관 여부 결정 | admin 저장 API가 생기면 CMS save write는 비활성 |

전환 중 같은 기능을 CMS Next API와 admin API가 동시에 write하지 않는다. admin API가 운영에 연결된 기능부터 CMS Next의 동일 write API를 deprecated 처리한다.

## Rollback 기준

- admin 관리 API 이관 후 장애가 발생하면, 해당 기능의 admin 메뉴/API 노출을 feature flag 또는 라우팅 설정으로 비활성화하고 기존 CMS Next 관리자 API를 임시 복구한다.
- rollback 시에도 동일 기능의 write owner는 하나만 열어둔다.
- 편집 load/save는 1단계에서 CMS owner를 유지하므로, 관리자 API rollback과 독립적으로 유지한다.
- 배포 push rollback은 `FWK_CMS_FILE_SEND_HIS` 기록의 중복 생성 여부를 확인한 뒤 수행한다.
- DB가 동일하므로 rollback은 데이터 복구보다 API 라우팅/화면 노출 복구가 중심이다. 단, admin API가 이미 변경한 상태값은 기존 CMS 화면에서 해석 가능한 상태 코드인지 확인해야 한다.

## 같은 DB 사용 시 코드만 수정하면 되는가

결론: 데이터 이관은 원칙적으로 필요 없지만, 코드만 단순 이동하면 끝나는 작업은 아니다.

필요한 작업:

- CMS TypeScript repository SQL을 `spider-admin` MyBatis mapper로 재작성
- 기존 CMS transaction 경계와 Java service transaction 경계 비교
- 동일 테이블을 쓰는 기간 동안 write owner를 하나로 제한
- `CMS:R`, `CMS:W` 권한 검사와 `FWK_USER_ACCESS_HIS` 작업이력 기록 적용
- CMS Next의 관리자/결재자 write API deprecated/비활성 처리. 제작자 dashboard API는 유지
- Java DTO와 CMS 응답 필드 호환성 유지

필요하지 않은 작업:

- 같은 DB를 그대로 사용한다면 `SPW_CMS_*`, `FWK_CMS_*` 데이터를 별도 DB로 복제하는 데이터 마이그레이션
- CMS 전용 사용자/권한 테이블 신설

## 후속 이슈 기준

- Issue 4는 이 문서의 화면/API prefix를 기준으로 `spider-admin` 화면 골격을 추가한다.
- Issue 5는 `/cms/dashboard -> /cms/edit` 기존 CMS Next 이동이 유지되는지 확인한다.
- Issue 6은 제작자 dashboard와 `/cms/edit` 런타임을 남기고 관리자성 CMS Next API를 deprecated/비활성 처리한다.
- Issue 7은 관리자/결재자 승인 API를 이 문서의 owner 기준으로 실제 구현한다. 제작자 dashboard API는 CMS 유지 범위다.
- Issue 8은 `SPW_CMS_ASSET`과 Docker 정적 리소스 정책을 확정한다.
- Issue 9는 A/B, 통계, 배포 API를 이 문서의 owner 기준으로 실제 구현한다.
