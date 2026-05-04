/**
 * @file PreviewPage.tsx
 * @description CMS 빌더 미리보기 페이지 컴포넌트.
 *
 * pageType 쿼리 파라미터로 데이터 소스를 구분한다.
 *   - ?pageType=REACT&pageId=xxx : DB에서 pageId로 페이지 JSON 조회 (승인 관리 등 외부 진입)
 *   - 파라미터 없음              : localStorage["cms_preview"] 읽기 (빌더 내부 미리보기 버튼)
 */
import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import type { CMSPage } from "../types";
import type { CmsPage } from "../../db/types";
import PageRenderer from "../runtime/renderPage";
import { UserScopeWrapper } from "../UserScopeWrapper";
import { cmsBase } from "../../lib/client-env";

const PREVIEW_KEY = "cms_preview";

export default function PreviewPage() {
  const [searchParams] = useSearchParams();
  const pageType = searchParams.get("pageType");
  const pageId = searchParams.get("pageId");

  const [page, setPage] = useState<CMSPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (pageType === "REACT" && pageId) {
      // 외부 진입: DB에서 pageId로 조회
      setLoading(true);
      fetch(`${cmsBase}/api/load`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ pageId }),
      })
        .then((r) => r.json())
        .then((data: { page?: CmsPage | null }) => {
          if (!data.page || !data.page.PAGE_HTML) {
            setError("페이지를 찾을 수 없습니다.");
            return;
          }
          // PAGE_HTML 컬럼에 CMSPage JSON이 문자열로 저장되어 있음
          try {
            setPage(JSON.parse(data.page.PAGE_HTML) as CMSPage);
          } catch {
            setError("페이지 데이터를 파싱할 수 없습니다.");
          }
        })
        .catch(() => setError("페이지 로드 중 오류가 발생했습니다."))
        .finally(() => setLoading(false));
    } else {
      // 빌더 내부 미리보기: localStorage에서 읽기
      const raw = localStorage.getItem(PREVIEW_KEY);
      if (!raw) return;
      try {
        setPage(JSON.parse(raw) as CMSPage);
      } catch {
        setError("미리보기 데이터를 파싱할 수 없습니다.");
      }
    }
  }, [pageType, pageId]);

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-white">
        <div className="w-8 h-8 border-4 border-[#0046A4] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-screen flex items-center justify-center bg-white">
        <p className="text-sm text-red-400">{error}</p>
      </div>
    );
  }

  if (!page) {
    return (
      <div className="h-screen flex items-center justify-center bg-white">
        <p className="text-sm text-gray-400">미리보기 데이터가 없습니다.</p>
      </div>
    );
  }

  return (
    <UserScopeWrapper className="bg-white">
      <PageRenderer page={page} />
    </UserScopeWrapper>
  );
}
