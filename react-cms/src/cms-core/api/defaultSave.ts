/**
 * @file defaultSave.ts
 * @description CMSApp의 기본 페이지 저장 핸들러 (파일 시스템 저장 전용).
 * cmsBankPlugin이 등록한 `/__cms/create-page` Vite dev 서버 엔드포인트에 POST 요청을 보냅니다.
 * CMSBuilder가 이미 Context 정보를 포함한 코드를 params.code에 주입하므로,
 * 이 함수는 별도 코드 생성 없이 params.code를 그대로 사용합니다.
 * params.code가 없는 경우(직접 호출)에는 generateJSX로 기본 코드를 생성합니다.
 *
 * 라우트 등록은 자동 수행하지 않으므로 개발자가 직접 추가해야 합니다.
 *
 * ※ admin 연동 모드(npm run dev:proxy)에서는 사용되지 않습니다.
 *    main.tsx에서 onSave={savePage}를 주입하므로 CMSApp의 fallback인 이 함수는 호출되지 않습니다.
 *    admin 연동 모드의 저장 로직은 src/savePage.ts를 참고하세요.
 *
 * @param page 저장할 CMSPage 데이터
 * @param params pageName(PascalCase), savePath(파일 시스템 저장 위치), code(JSX 코드 문자열)
 */
import { generateJSX } from "../codegen/exportCode";
import type { CMSPage } from "../types";
import type { SavePageParams } from "../SavePageModal";

// BASE_URL 기준 /__cms/ 접두사 생성.
// 프록시 모드(BASE_URL=/react-cms/): '/react-cms/__cms' → nginx가 Vite로 라우팅
// 단독 모드(BASE_URL=/):             '/__cms'           → Vite 직접 처리
const cmsBase = `${import.meta.env.BASE_URL.replace(/\/$/, "")}/__cms`;

export async function defaultSave(page: CMSPage, params: SavePageParams): Promise<void> {
  const { pageName, savePath } = params;
  // CMSBuilder에서 layouts/codegenConfig/overlayTemplates Context를 포함해 사전 생성한 코드 우선 사용.
  // params.code가 없는 경우(직접 호출 시) generateJSX로 폴백 — Context 정보 미포함 주의.
  // pageName을 6번째 인자로 전달하여 함수명도 올바르게 생성한다.
  const code = params.code ?? generateJSX(page, undefined, undefined, undefined, undefined, pageName);

  const res = await fetch(`${cmsBase}/create-page`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ savePath, code, pageName }),
  });

  if (!res.ok) {
    // 서버 응답이 JSON이 아닌 경우(예: Vite 오류 페이지)에 대비해 catch로 빈 객체 반환
    const data = await res.json().catch(() => ({}));
    // data가 { error?: string } 형태임을 단언 — cmsBankPlugin 응답 포맷에 의존
    throw new Error((data as { error?: string }).error ?? "페이지 저장에 실패했습니다.");
  }
}
