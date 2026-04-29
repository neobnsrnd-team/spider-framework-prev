# Springware CMS

금융권 특화 비주얼 웹 콘텐츠 빌더. 개발자가 아닌 사람도 드래그앤드랍으로 금융권 모바일 앱 화면을 만들 수 있는 에디터입니다.

## 노션 URL
https://omniscient-poultry-94a.notion.site/Springware-CMS-HTML-33607fbbb0738045a8cadb0cd5997226?source=copy_link

## 기술 스택

- **Next.js 15.5.4** (App Router) + **React 19** + **TypeScript**
- **ContentBuilder.js** — 비주얼 에디터 라이브러리 (Innova Studio)
- **Tailwind CSS 4**
- **AI**: OpenRouter / OpenAI / FAL AI (이미지 생성)

## 실행 방법

```bash
npm install
npm run dev
```

| URL | 설명 |
|-----|------|
| `http://localhost:3000/cms/edit` | 에디터 (메인 작업 화면) |
| `http://localhost:3000/cms/view` | 저장된 결과물 미리보기 |
| `http://localhost:3000/cms/dashboard` | 페이지 대시보드 |
| `http://localhost:3000/cms/approve` | 승인 관리 화면 |
| `http://localhost:3000/cms/ab` | A/B 테스트 관리 |

## 환경변수

프로젝트 루트에 `.env` 파일 생성 후 아래 키 입력:

```env
# Oracle DB
ORACLE_USER=
ORACLE_PASSWORD=
ORACLE_HOST=
ORACLE_PORT=
ORACLE_SERVICE=
ORACLE_SCHEMA=

# AI
OPENROUTER_API_KEY=   # AI 코드 생성 (기본 프로바이더)
OPENAI_API_KEY=       # AI 대안 프로바이더
FAL_API_KEY=          # AI 이미지 생성 (fal.ai)
GEMINI_API_KEY=       # Google GenAI

# 에셋 저장소
ASSET_UPLOAD_DIR=     # 업로드 파일 저장 경로 (기본: public/uploads)
ASSET_BASE_URL=       # 업로드 파일 URL prefix (기본: /uploads)
```

## 데이터 저장 방식

| 데이터 | 저장 위치 | 비고 |
|--------|-----------|------|
| 에디터 캔버스 HTML | Oracle DB (`SPW_CMS_PAGE.PAGE_HTML` CLOB) | Save 버튼 → API → DB |
| 페이지 수정 이력 | Oracle DB (`SPW_CMS_PAGE_HISTORY`) | 버전별 렌더링 HTML 보관 |
| 업로드 이미지 | `ASSET_UPLOAD_DIR` (기본 `public/uploads/`) | 운영: nginx `/static/` 정적 서빙 |
| 에셋 메타데이터 | Oracle DB (`SPW_CMS_ASSET`) | 파일명, 경로, MIME 타입 등 |

> DB 스키마: `src/db/ddl/V1__init_schema.sql` ~ `V5__asset_blob_to_file.sql`

## 금융 컴포넌트

우측 패널 "금융 컴포넌트" 탭에서 드래그하거나 + 클릭으로 캔버스에 추가합니다.
추가 후 더블클릭으로 텍스트·이미지를 인라인 편집할 수 있습니다.

| 컴포넌트 | 설명 | 방식 |
|----------|------|------|
| 앱 헤더 (`app-header`) | 로고·은행명 헤더 | 순수 HTML |
| 퀵뱅킹 메뉴 (`product-menu`) | 아이콘 그리드 (조회·이체·카드 등) | 순수 HTML |
| 보안인증센터 (`auth-center`) | 공동인증서·금융인증서·OTP·보안카드 | 순수 HTML |
| 미디어 (`media-video`) | YouTube 임베드 | 순수 HTML |
| 사이트 푸터 (`site-footer`) | 약관·연락처·TOP 버튼 | 순수 HTML |
| 상품 갤러리 (`product-gallery`) | 카드형 예금·적금·대출 금리 | 플러그인 |
| 홍보 배너 (`promo-banner`) | 슬라이드 배너 | 플러그인 |
| 환율 보드 (`exchange-board`) | USD·EUR·JPY·CNY 환율 + 환전 신청 | 플러그인 |
| 영업점/ATM (`branch-locator`) | 지도 + 영업점 목록 바텀시트 | 플러그인 |
| 금융 계산기 (`loan-calculator`) | 대출·예금·적금 탭 전환 계산기 | 플러그인 |

> **순수 HTML**: ContentBuilder 인라인 편집(더블클릭) 지원
> **플러그인**: JS 런타임 의존 (실시간 데이터·지도·계산 등)

## API 구조

```
# 에디터 콘텐츠
/api/builder/load                           콘텐츠 불러오기
/api/builder/save                           콘텐츠 저장
/api/builder/upload                         에디터 내 파일 업로드

# 페이지 관리
/api/builder/pages                          페이지 목록 조회·삭제
/api/builder/pages/[pageId]/approve         페이지 승인
/api/builder/pages/[pageId]/approve-request 승인 요청
/api/builder/pages/[pageId]/reject          승인 반려
/api/builder/pages/[pageId]/history         페이지 이력 조회
/api/builder/pages/[pageId]/rollback        이력 롤백
/api/builder/pages/[pageId]/set-public      공개 여부 설정
/api/builder/pages/[pageId]/update-dates    노출 기간 수정

# 에셋 관리
/api/assets                                 에셋 목록 조회·등록
/api/assets/[assetId]                       에셋 단건 조회·수정·삭제
/api/assets/[assetId]/image                 에셋 이미지 직접 조회

# A/B 테스트
/api/builder/ab                             A/B 그룹 관리
/api/builder/ab/promote                     A/B 승급
/api/ab/[groupId]                           그룹별 콘텐츠 조회

# 배포
/api/deploy/push                            운영 서버 배포
/api/deploy/receive                         배포 수신
/api/deploy/history                         배포 이력

# 트래킹
/api/track/view                             페이지 조회 추적
/api/track/click                            클릭 추적
/api/track/stats                            통계 조회

# AI
/api/openrouter                             OpenRouter 프록시
/api/openrouter/stream                      OpenRouter 스트리밍
/api/openai                                 OpenAI 프록시
/api/openai/stream                          OpenAI 스트리밍
/api/fal/request                            FAL AI 이미지 생성 큐 등록
/api/fal/status                             생성 상태 확인
/api/fal/result                             생성 결과 조회
/api/fal/cleanup                            임시 데이터 정리

# 기타
/api/exchange                               환율 데이터
/api/branches                               영업점·ATM 위치 데이터
/api/components                             금융 컴포넌트 목록 (DB)
/api/codes                                  공통 코드 목록
/api/auth/approvers                         승인자 목록
/api/scheduler/expire                       노출 기간 만료 처리
/api/health                                 헬스 체크
```

## 프로젝트 구조

```
src/
├── app/                            # 라우팅 전용 (page.tsx, route.ts)
│   ├── edit/, view/, dashboard/
│   ├── approve/                    # 승인 관리 페이지
│   ├── ab/                         # A/B 테스트 페이지
│   ├── [userId]/                   # 사용자별 대시보드
│   └── api/
├── components/
│   ├── edit/                       # 에디터 UI
│   │   ├── EditClient.tsx          # ★ 핵심 — ContentBuilder 초기화·플러그인 등록
│   │   ├── EditClientLoader.tsx    # SSR 방지 동적 로더
│   │   ├── ComponentPanel.tsx      # 금융 컴포넌트 패널
│   │   └── *Editor.tsx             # 각 컴포넌트별 편집 패널 (15개+)
│   ├── view/
│   │   └── ViewClient.tsx          # ContentBuilderRuntime으로 저장된 HTML 렌더링
│   ├── dashboard/                  # 사용자 대시보드
│   ├── approve/                    # 승인 관리
│   ├── ab/                         # A/B 테스트
│   ├── admin/                      # 관리자 네비게이션
│   └── ui/                         # 공통 UI (Modal, PageCard, Toast)
├── data/
│   ├── finance-component-data.ts   # 금융 컴포넌트 타입 정의
│   ├── ko.ts                       # ContentBuilder 한국어 로컬라이제이션
│   ├── approve-config.ts           # 승인 워크플로우 설정
│   └── brand-themes.ts             # 금융사 브랜드 테마
├── lib/
│   ├── api-response.ts             # successResponse, errorResponse 헬퍼
│   ├── api-url.ts                  # nextApi, javaApi (basePath 반영 URL 빌더)
│   ├── current-user.ts             # getCurrentUser, canWriteCms, canReadCms
│   ├── env.ts                      # 환경변수 래퍼 (ASSET_UPLOAD_DIR, ASSET_BASE_URL 등)
│   ├── validators.ts               # isValidBankId, isPageExpired
│   ├── page-file.ts                # readPageHtml (FILE_PATH 기반 레거시 폴백)
│   ├── deploy-utils.ts             # 배포 유틸
│   └── constants.ts                # 상수 정의
├── db/                             # Oracle DB 레이어
│   ├── connection.ts               # 커넥션 풀, withTransaction, clobBind
│   ├── queries/                    # SQL 상수 (page, component, asset, page-history 등)
│   └── repository/                 # 데이터 접근 레이어 (page, component, asset, file-send, page-view-log)
└── types/                          # 타입 선언
public/
├── assets/plugins/                 # 플러그인 컴포넌트 (lazy-load)
├── runtime/                        # ContentBuilder 런타임 라이브러리
└── uploads/                        # 업로드된 에셋 파일
```

자세한 기술 내용은 [docs/reference/기술-개요.md](docs/reference/기술-개요.md) 참고.



