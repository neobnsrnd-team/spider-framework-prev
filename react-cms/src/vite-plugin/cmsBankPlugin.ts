/**
 * @file cmsBankPlugin.ts
 * @description CMS 빌더에서 페이지 저장 시 호출되는 Vite 플러그인.
 *
 * 기존 파일 시스템 저장 엔드포인트에 더해 Oracle DB 기반 CRUD API를 제공합니다.
 *
 * 등록 엔드포인트:
 *   - POST /__cms/create-page      — JSX 파일 생성 + 라우터 등록 (기존 유지)
 *   - POST /__cms/api/save         — 페이지 JSON DB 저장 (신규 UUID 또는 기존 업데이트)
 *   - POST /__cms/api/load         — pageId로 DB 조회 후 JSON 반환
 *   - GET  /__cms/api/pages        — 페이지 목록 (검색·정렬·페이지네이션)
 *   - DELETE /__cms/api/pages      — pageId로 페이지 삭제
 *
 * @example
 * // react-cms/vite.config.ts
 * import { cmsBankPlugin } from './src/vite-plugin/cmsBankPlugin'
 * export default defineConfig({
 *   plugins: [cmsBankPlugin({ routerPath: '../demo/front/src/routes/index.tsx', pagesDir: '../demo/front/src/pages/cms' })],
 * })
 */
import type { Plugin } from "vite";
import fs from "node:fs";
import path from "node:path";
import { v4 as uuidv4 } from "uuid";
import {
  getCurrentUser,
  requireCmsWrite,
  requireCmsRead,
  canReadCms,
  UnauthorizedError,
  type CurrentUser,
} from "../cms-admin/current-user";

// DB 모듈은 dynamic import로 지연 로드합니다.
// Vite는 vite.config.ts 평가 단계(서버 초기화 전)에 플러그인을 로드하므로,
// 이 시점에는 .env가 아직 process.env에 반영되지 않을 수 있습니다.
// 첫 요청이 들어올 때 import하면 .env가 반영된 이후이므로 안전합니다.
// Promise를 캐싱하여 중복 import 방지 (module system이 캐싱하지만 명시적으로도 보장).
type PageRepository = typeof import("../db/repository/page.repository");

let _repoPromise: Promise<PageRepository> | null = null;
async function getRepo(): Promise<PageRepository> {
  if (!_repoPromise) {
    _repoPromise = import("../db/repository/page.repository") as Promise<PageRepository>;
  }
  return _repoPromise;
}

// ── 유틸 ────────────────────────────────────────────────────────

/** 요청 헤더에서 Cookie 문자열을 추출한다 */
function getCookieHeader(req: import("http").IncomingMessage): string {
  const raw = req.headers.cookie;
  return Array.isArray(raw) ? raw.join('; ') : (raw ?? '');
}

/**
 * UnauthorizedError를 403 JSON 응답으로 변환한다.
 * @returns true이면 이미 응답 전송 완료, 호출부는 return해야 한다.
 */
function handleAuthError(
  err: unknown,
  res: import("http").ServerResponse,
): boolean {
  if (err instanceof UnauthorizedError) {
    jsonResponse(res, 403, { error: err.message });
    return true;
  }
  return false;
}

/** req body를 JSON으로 파싱 */
function readBody(req: import("http").IncomingMessage): Promise<unknown> {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk: Buffer) => { raw += chunk.toString(); });
    req.on("end", () => {
      try { resolve(JSON.parse(raw)); }
      catch { reject(new Error("Invalid JSON")); }
    });
    req.on("error", reject);
  });
}

/** JSON 응답 전송 헬퍼 */
function jsonResponse(
  res: import("http").ServerResponse,
  status: number,
  body: unknown,
): void {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json");
  res.end(JSON.stringify(body));
}

// ── 파일 시스템 저장 (기존 로직) ────────────────────────────────

interface CreatePagePayload {
  uri: string;
  code: string;
  pageName: string;
}

export interface CmsBankPluginOptions {
  /**
   * 라우터 파일 경로 (프로젝트 루트 기준).
   * pageRoutes 배열과 modalRoutes 배열을 export 하는 파일이어야 한다.
   * @default "src/routes/index.tsx"
   */
  routerPath?: string;
  /**
   * 생성된 페이지 파일을 저장할 디렉토리 (프로젝트 루트 기준)
   * @default "src/pages/cms"
   */
  pagesDir?: string;
}

/**
 * 페이지 컴포넌트 파일(.tsx)을 디스크에 생성한다.
 * 코드 내 "NewPage" 함수명을 실제 컴포넌트 이름으로 치환한다.
 */
function createPageFile(pagesDir: string, pageName: string, code: string) {
  if (!fs.existsSync(pagesDir)) {
    fs.mkdirSync(pagesDir, { recursive: true });
  }

  const finalCode = code.replace(/function NewPage\(\)/, `function ${pageName}()`);
  fs.writeFileSync(path.join(pagesDir, `${pageName}.tsx`), finalCode, "utf-8");
}

/**
 * routes/index.tsx 에 import 문과 pageRoutes 항목을 삽입한다.
 *
 * 삽입 위치:
 *   - import: `export const pageRoutes` 선언 바로 위
 *   - route:  `pageRoutes` 배열 닫는 `];` 바로 앞 (modalRoutes 선언 직전)
 */
function addToRouter(
  routerFile: string,
  pageName: string,
  uri: string,
  pageImportPath: string,
) {
  let content = fs.readFileSync(routerFile, "utf-8");

  // 1. pageRoutes 선언 직전에 import 삽입
  content = content.replace(
    /\nexport const pageRoutes/,
    `\nimport ${pageName} from "${pageImportPath}";\nexport const pageRoutes`,
  );

  // URL 앞 슬래시 제거 (라우트 path에는 슬래시 없이 등록)
  const routePath = uri.startsWith("/") ? uri.slice(1) : uri;

  // 2. pageRoutes 배열 닫는 `];` 바로 앞, modalRoutes 선언 직전에 route 삽입
  content = content.replace(
    /(\n\];)\n+(export const modalRoutes)/,
    `\n  { path: "${routePath}", element: <${pageName} /> },$1\n\n$2`,
  );

  fs.writeFileSync(routerFile, content, "utf-8");
}

// ── 플러그인 ─────────────────────────────────────────────────────

/**
 * CMS 빌더 페이지 저장 요청을 처리하는 Vite 플러그인.
 * Vite dev 서버에서만 동작하며 프로덕션 빌드에는 영향을 주지 않는다.
 */
export function cmsBankPlugin(options: CmsBankPluginOptions = {}): Plugin {
  let root: string;
  let base = "/";

  return {
    name: "vite-cms-page-writer",
    configResolved(config) {
      root = config.root;
      base = config.base ?? "/";
    },
    configureServer(server) {
      // 단일 미들웨어로 모든 /__cms/ 요청을 처리한다.
      // nginx 프록시 모드(base=/react-cms/)에서 들어오는 요청은
      // '/react-cms/__cms/...' 형태이므로, base 접두사를 제거해 경로를 정규화한다.
      server.middlewares.use(async (req, res, next) => {
        const rawUrl = req.url ?? "/";

        // base 접두사 제거: '/react-cms/__cms/api/save' → '/__cms/api/save'
        const normalizedUrl =
          base !== "/" && rawUrl.startsWith(base)
            ? "/" + rawUrl.slice(base.length)
            : rawUrl;

        const urlPath = normalizedUrl.split("?")[0];

        // ── GET /__cms/api/me ──────────────────────────────────
        // 현재 로그인 사용자 정보를 반환한다.
        // 클라이언트가 마운트 시 호출해 권한을 확인하는 용도로 사용한다.
        if (urlPath === "/__cms/api/me" && req.method === "GET") {
          const cookieHeader = getCookieHeader(req);
          const user = await getCurrentUser(cookieHeader);
          // canRead 여부를 함께 반환해 클라이언트 라우트 가드에서 활용
          jsonResponse(res, 200, { user, canRead: canReadCms(user) });
          return;
        }

        // ── POST /__cms/create-page ────────────────────────────
        if (urlPath === "/__cms/create-page" && req.method === "POST") {
          try {
            // admin 연동 모드(base !== '/')에서만 쓰기 권한 검증
            // 단독 실행 모드(base === '/')에서는 인증 없이 파일 저장 허용
            if (base !== "/") {
              // admin 연동 모드에서만 쓰기 권한 검증 (단독 실행 모드는 인증 없이 파일 저장 허용)
              await requireCmsWrite(getCookieHeader(req));
            }
            const payload = await readBody(req) as CreatePagePayload;

            // PascalCase 영숫자만 허용 — 경로 조작(../ 등) 방지
            const NAME_REGEX = /^[A-Z][a-zA-Z0-9]*$/;
            if (!NAME_REGEX.test(payload.pageName)) {
              jsonResponse(res, 400, { error: "pageName은 PascalCase 영숫자만 허용됩니다." });
              return;
            }

            const routerFile = path.join(root, options.routerPath ?? "src/routes/index.tsx");
            const pagesDir   = path.join(root, options.pagesDir   ?? "src/pages/cms");

            // @/ alias는 src/ 를 가리키므로 routerFile 경로에서 /src/ 위치를 찾아 srcDir 추론
            const srcMatch = routerFile.replace(/\\/g, "/").match(/^(.*\/src)\//);
            const srcDir = srcMatch ? srcMatch[1] : path.join(root, "src");
            const relativePath = path.relative(srcDir, pagesDir).replace(/\\/g, "/");
            const pageImportPath = `@/${relativePath}/${payload.pageName}`;

            createPageFile(pagesDir, payload.pageName, payload.code);
            addToRouter(routerFile, payload.pageName, payload.uri, pageImportPath);

            jsonResponse(res, 200, { success: true });
          } catch (err) {
            if (handleAuthError(err, res)) return;
            jsonResponse(res, 500, { error: String(err) });
          }
          return;
        }

        // ── POST /__cms/api/save ───────────────────────────────
        // body: { pageId?: string, pageName: string, pageJson: string, pageCode: string }
        // response: { pageId: string }
        if (urlPath === "/__cms/api/save" && req.method === "POST") {
          let user: CurrentUser;
          try {
            // 저장은 CMS:W 권한 필요
            user = await requireCmsWrite(getCookieHeader(req));
          } catch (err) {
            if (handleAuthError(err, res)) return;
            jsonResponse(res, 500, { error: String(err) });
            return;
          }
          try {
            const body = await readBody(req) as {
              pageId?: string;
              pageName: string;
              pageJson: string;
              pageCode: string;
            };

            const repo = await getRepo();
            let pageId = body.pageId ?? null;

            if (pageId) {
              const existing = await repo.getPageById(pageId);
              if (existing) {
                await repo.updatePage({ pageId, pageName: body.pageName, pageJson: body.pageJson, pageCode: body.pageCode, user });
              } else {
                // pageId가 DB에 없으면 소프트 삭제된 행이거나 무효 ID — 새 UUID로 신규 생성.
                // 기존 pageId로 INSERT하면 USE_YN='N' 행과 PK 충돌(ORA-00001)이 발생한다.
                pageId = uuidv4();
                await repo.createPage({ pageId, pageName: body.pageName, pageJson: body.pageJson, pageCode: body.pageCode, user });
              }
            } else {
              pageId = uuidv4();
              await repo.createPage({ pageId, pageName: body.pageName, pageJson: body.pageJson, pageCode: body.pageCode, user });
            }

            jsonResponse(res, 200, { pageId });
          } catch (err) {
            console.error("[react-cms] DB save error:", err);
            jsonResponse(res, 500, { error: String(err) });
          }
          return;
        }

        // ── POST /__cms/api/load ───────────────────────────────
        // body: { pageId: string }
        // response: { page: CmsPage | null }
        if (urlPath === "/__cms/api/load" && req.method === "POST") {
          try {
            // 조회는 CMS:R 권한 필요
            await requireCmsRead(getCookieHeader(req));
            const body = await readBody(req) as { pageId: string };
            const page = await (await getRepo()).getPageById(body.pageId);
            jsonResponse(res, 200, { page });
          } catch (err) {
            if (handleAuthError(err, res)) return;
            console.error("[react-cms] DB load error:", err);
            jsonResponse(res, 500, { error: String(err) });
          }
          return;
        }

        // ── GET /__cms/api/pages ───────────────────────────────
        // ?search=&sortBy=date&approveState=&page=1&pageSize=10
        // response: { list: CmsPage[], totalCount: number }
        if (urlPath === "/__cms/api/pages" && req.method === "GET") {
          try {
            // 목록 조회는 CMS:R 권한 필요
            await requireCmsRead(getCookieHeader(req));
            const url          = new URL(normalizedUrl, "http://localhost");
            const search       = url.searchParams.get("search")       ?? undefined;
            const sortBy       = url.searchParams.get("sortBy")       ?? undefined;
            const approveState = url.searchParams.get("approveState") ?? undefined;
            const page         = parseInt(url.searchParams.get("page")     ?? "1",  10);
            const pageSize     = parseInt(url.searchParams.get("pageSize") ?? "10", 10);

            const result = await (await getRepo()).listPages({
              search,
              sortBy: sortBy === "name" ? "name" : "date",
              approveState,
              page,
              pageSize,
            });

            jsonResponse(res, 200, result);
          } catch (err) {
            if (handleAuthError(err, res)) return;
            console.error("[react-cms] DB list error:", err);
            jsonResponse(res, 500, { error: String(err) });
          }
          return;
        }

        // ── DELETE /__cms/api/pages?pageId=xxx ─────────────────
        // response: { success: true }
        if (urlPath === "/__cms/api/pages" && req.method === "DELETE") {
          try {
            // 삭제는 CMS:W 권한 필요
            await requireCmsWrite(getCookieHeader(req));
            const url    = new URL(normalizedUrl, "http://localhost");
            const pageId = url.searchParams.get("pageId");

            if (!pageId) {
              jsonResponse(res, 400, { error: "pageId가 필요합니다." });
              return;
            }

            await (await getRepo()).deletePage(pageId);
            jsonResponse(res, 200, { success: true });
          } catch (err) {
            if (handleAuthError(err, res)) return;
            console.error("[react-cms] DB delete error:", err);
            jsonResponse(res, 500, { error: String(err) });
          }
          return;
        }

        next();
      });
    },
  };
}
