/**
 * @file CmsAuthGuard.tsx
 * @description Spider Admin 권한 검증 레이아웃 가드.
 *
 * 마운트 시 `/__cms/api/me`를 호출해 REACT_CMS:R 권한을 확인한다.
 * - 로딩 중: 스피너 표시
 * - canRead=false: `/not-authorized`로 리다이렉트
 * - canRead=true: 자식 라우트 렌더링 (Outlet)
 */
import { useEffect, useState } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { cmsBase } from "../lib/client-env";

export default function CmsAuthGuard() {
  const [authorized, setAuthorized] = useState<boolean | null>(null);

  useEffect(() => {
    fetch(`${cmsBase}/api/me`, { credentials: "include" })
      .then((r) => r.json())
      .then((data: { canRead?: boolean }) => {
        setAuthorized(data.canRead ?? false);
      })
      .catch(() => {
        // 네트워크 오류 시 미인가 처리
        setAuthorized(false);
      });
  }, []);

  if (authorized === null) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-[#0046A4] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  // 권한 없음 — not-authorized 페이지로 이동 (replace로 뒤로가기 차단)
  if (!authorized) return <Navigate to="/not-authorized" replace />;
  return <Outlet />;
}
