/**
 * @file savePage.ts
 * @description CMS 빌더 페이지 저장 핸들러.
 * 실행 모드에 따라 저장 방식을 분기합니다:
 *   - admin 연동 모드 (npm run dev:proxy, BASE_URL=/react-cms/):
 *       Oracle DB 저장만 수행 — 파일 생성 없음.
 *       URL의 `?pageId=...` 가 있으면 해당 행을 UPDATE, 없으면 새 row INSERT.
 *       INSERT 후에는 응답 pageId를 URL에 replaceState로 동기화하여
 *       이후 재저장·새로고침이 동일 row에 매핑되도록 보장합니다.
 *   - 단독 실행 모드 (npm run dev, BASE_URL=/):
 *       파일 시스템 저장만 수행 — 사용자가 입력한 savePath에 .tsx 파일을 생성하며,
 *       라우트 등록은 호출 측 개발자가 직접 수행해야 합니다.
 *
 * @param page 저장할 CMSPage 데이터
 * @param params pageName(PascalCase), savePath(파일 시스템 저장 위치, 단독 실행 모드 한정), code(JSX 코드 문자열)
 */
import { generateJSX } from "@cms-core"
import type { CMSPage, SavePageParams } from "@cms-core"
import { isAdminMode, cmsBase } from "./lib/client-env"

export async function savePage(page: CMSPage, params: SavePageParams): Promise<void> {
  const { pageName, savePath } = params
  // CMSBuilder에서 Context 정보를 포함해 사전 생성한 코드 우선 사용.
  // 없으면(직접 호출 시) generateJSX로 폴백 — Context 정보 미포함 주의.
  const code = params.code ?? generateJSX(page)

  if (isAdminMode) {
    // ── admin 연동 모드: DB 저장만 수행 ─────────────────────────
    // URL의 ?pageId 를 신뢰 — 신규 빌더 진입(?pageId 없음)은 항상 INSERT,
    // 기존 페이지 편집(?pageId=xxx)은 UPDATE로 직결된다.
    // localStorage 기반 pageName→pageId 매핑은 동일 이름 페이지 간
    // 의도치 않은 UPDATE 충돌을 일으켜 제거함.
    const urlPageId = new URLSearchParams(window.location.search).get("pageId") ?? undefined

    const dbRes = await fetch(`${cmsBase}/api/save`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        pageId:   urlPageId,
        pageName,
        pageJson: JSON.stringify(page),
        pageCode: code,
      }),
    })

    if (!dbRes.ok) {
      const data = await dbRes.json().catch(() => ({}))
      throw new Error((data as { error?: string }).error ?? "DB 저장에 실패했습니다.")
    }

    const { pageId } = (await dbRes.json()) as { pageId: string }

    // 응답 pageId를 URL에 동기화 — 새로고침/재저장 시 동일 row를 UPDATE하도록 보장.
    // pushState가 아닌 replaceState를 사용하는 이유: React Router의 useSearchParams가
    // 재트리거되어 BuilderPage가 재마운트(=DB 재조회 + 작업 중 상태 손실)되는 것을 막기 위함.
    if (urlPageId !== pageId) {
      const url = new URL(window.location.href)
      url.searchParams.set("pageId", pageId)
      window.history.replaceState(null, "", url.toString())
    }
  } else {
    // ── 단독 실행 모드: 파일 시스템 저장만 수행 ─────────────────
    // 모달에서 입력받은 savePath 경로에 컴포넌트 파일을 생성한다.
    // 라우터 등록은 자동 수행하지 않으므로 개발자가 별도로 추가해야 한다.
    const fsRes = await fetch(`${cmsBase}/create-page`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ savePath, code, pageName }),
    })

    if (!fsRes.ok) {
      const data = await fsRes.json().catch(() => ({}))
      throw new Error((data as { error?: string }).error ?? "파일 저장에 실패했습니다.")
    }
  }
}
