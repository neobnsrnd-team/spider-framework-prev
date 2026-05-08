/**
 * @file main.tsx
 * @description React CMS 빌더 앱의 진입점.
 *
 * Router를 직접 소유하고 admin 컴포넌트(CmsAuthGuard, NotAuthorizedPage)를 조립합니다.
 * CMSApp은 CMS 컨텍스트만 제공하며, RouterProvider를 children으로 받습니다.
 *
 * 라우트 구성:
 *   - /not-authorized  : 권한 없는 사용자용 공개 페이지 (인증 가드 밖)
 *   - /                : admin 연동 모드 → CmsAuthGuard (REACT_CMS:R 검증)
 *                        단독 실행 모드 → 인증 없이 바로 자식 라우트 렌더링
 *   - /builder         : CMS 에디터
 *   - /preview         : 페이지 미리보기
 */
import { StrictMode, useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import {
  createBrowserRouter,
  RouterProvider,
  Navigate,
  Outlet,
  useSearchParams,
} from "react-router-dom";
import { CMSApp } from "@cms-core";
import type { CMSPage } from "@cms-core/types";
import { CMSBuilder } from "@cms-core/CMSBuilder";
import PreviewPage from "@cms-core/preview/PreviewPage";
import CmsAuthGuard from "./cms-admin/CmsAuthGuard";
import NotAuthorizedPage from "./cms-admin/NotAuthorizedPage";
import { blocks, overlays, layouts } from "./cms.config";
import { savePage } from "./savePage";
import userScopeCSS from "./user-scope.css?inline";
import { isAdminMode, cmsBase } from "./lib/client-env";
import "./index.css";

// .env의 VITE_CMS_BRAND 값을 data-theme으로 설정해 index.css의 브랜드 토큰을 활성화한다.
// hana가 기본값(:root)이므로 미설정 시에도 하나은행 테마가 적용된다.
document.documentElement.setAttribute(
  "data-theme",
  import.meta.env.VITE_CMS_BRAND ?? "hana",
);


/**
 * pageId 쿼리 파라미터가 있으면 DB에서 해당 페이지를 조회해 편집 모드로 빌더를 시작합니다.
 * pageId 없이 접근하면 신규 생성 모드로 빌더를 시작합니다.
 *
 * 저장 시 UPDATE/INSERT 분기는 URL의 ?pageId 존재 여부로 결정합니다 (savePage.ts).
 * 신규 저장 직후 응답 pageId는 history.replaceState로 URL에 동기화되어
 * 이후 재저장·새로고침이 동일 row에 매핑됩니다.
 */
function BuilderPage() {
  const [searchParams] = useSearchParams();
  const pageId = searchParams.get("pageId");

  const [initialPage, setInitialPage] = useState<CMSPage | undefined>(
    undefined,
  );
  const [initialPageName, setInitialPageName] = useState<string | undefined>(
    undefined,
  );
  const [approveState, setApproveState] = useState<string | undefined>(
    undefined,
  );
  const [rejectedReason, setRejectedReason] = useState<string | null>(null);
  const [loading, setLoading] = useState(!!pageId);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!pageId) return;

    const pageLoad = fetch(`${cmsBase}/api/load`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ pageId }),
    }).then((r) => r.json());

    // admin 연동 모드에서만 승인 상태 조회
    // 승인 상태는 부가 정보이므로 실패해도 에디터 진입이 가능하도록 독립 에러 처리
    const approvalLoad = isAdminMode
      ? fetch(`/api/react-cms-dashboard/pages/${pageId}/approval-status`)
          .then((r) => r.json())
          .catch(() => null)
      : Promise.resolve(null);

    Promise.all([pageLoad, approvalLoad])
      .then(
        ([data, approvalData]: [
          { page?: { PAGE_HTML?: string | null; PAGE_NAME?: string } | null },
          {
            data?: { approveState?: string; rejectedReason?: string | null };
          } | null,
        ]) => {
          const row = data.page;
          if (!row) {
            setError("페이지를 찾을 수 없습니다.");
            return;
          }
          // 모달 초기 컴포넌트명 세팅. 재편집 시 UPDATE 분기는 URL의 ?pageId가 직접 보장한다.
          if (row.PAGE_NAME) {
            setInitialPageName(row.PAGE_NAME);
          }
          if (row.PAGE_HTML) {
            try {
              setInitialPage(JSON.parse(row.PAGE_HTML) as CMSPage);
            } catch {
              setError("페이지 데이터를 불러올 수 없습니다.");
            }
          }
          if (approvalData?.data) {
            setApproveState(approvalData.data.approveState);
            setRejectedReason(approvalData.data.rejectedReason ?? null);
          }
        },
      )
      .catch(() => setError("페이지 불러오기 중 오류가 발생했습니다."))
      .finally(() => setLoading(false));
  }, [pageId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen text-sm text-gray-500">
        페이지를 불러오는 중...
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4">
        <p className="text-sm text-red-500">{error}</p>
        <div className="flex gap-2">
          <button
            className="px-4 py-2 text-xs rounded-lg bg-primary text-white hover:bg-primary-dark transition-colors"
            onClick={() => {
              window.location.href = window.location.pathname;
            }}
          >
            새 페이지 생성
          </button>
          <button
            className="px-4 py-2 text-xs rounded-lg bg-gray-200 text-gray-700 hover:bg-gray-300 transition-colors"
            onClick={() => window.history.back()}
          >
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <CMSBuilder
      onSave={savePage}
      initialPage={initialPage}
      mode={pageId ? "edit" : "create"}
      initialPageName={initialPageName}
      approveState={approveState}
      rejectedReason={rejectedReason}
      // admin 연동 모드는 DB 저장이라 저장 위치 개념이 없음 → 입력란 숨김
      // 단독 실행 모드는 파일 시스템 저장이므로 저장 위치 입력란 노출
      requireSavePath={!isAdminMode}
      // demo/front 앱의 페이지 디렉토리를 기본값으로 제공.
      // 사용자는 모달에서 자유롭게 다른 경로로 변경 가능.
      defaultSavePath="../demo/front/src/pages/cms"
    />
  );
}

// BASE_URL은 Vite가 vite.config.ts의 base 설정값으로 주입한다.
// VITE_BASE=/react-cms/ 로 실행 시 '/react-cms/' — nginx 프록시를 거쳐 admin과 연동되는 모드.
// 단독 개발 시 기본값 '/' → admin 연동 없이 builder/preview 직접 접근 허용.
const basename =
  import.meta.env.BASE_URL !== "/"
    ? import.meta.env.BASE_URL.replace(/\/$/, "")
    : undefined;

const router = createBrowserRouter(
  [
    // 공개 경로 — 인증 가드 밖
    {
      path: "not-authorized",
      element: <NotAuthorizedPage />,
    },
    {
      path: "/",
      // admin 연동 모드: CmsAuthGuard로 REACT_CMS:R 권한 검증 후 자식 라우트 렌더링
      // 단독 실행 모드: 인증 없이 Outlet으로 바로 자식 라우트 렌더링
      element: isAdminMode ? <CmsAuthGuard /> : <Outlet />,
      children: [
        { index: true, element: <Navigate to="/builder" replace /> },
        {
          path: "builder",
          element: <BuilderPage />,
        },
        {
          path: "preview",
          element: <PreviewPage />,
        },
      ],
    },
  ],
  { basename },
);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <CMSApp
      blocks={blocks}
      overlays={overlays}
      layouts={layouts}
      codegenConfig={{ blockImportFrom: "@cl", layoutImportFrom: "@cl" }}
      stylesheetContent={userScopeCSS}
      stylesheetScope={{ "data-brand": import.meta.env.VITE_CMS_BRAND ?? "hana", "data-domain": "card" }}
    >
      <RouterProvider router={router} />
    </CMSApp>
  </StrictMode>,
);
