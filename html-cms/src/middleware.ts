// src/middleware.ts
// CORS 응답 헤더 주입 + 긴급차단(IS_PUBLIC='N') 페이지 접근 차단

import { NextRequest, NextResponse } from 'next/server';

const ALLOWED_ORIGINS = (process.env.CORS_ALLOWED_ORIGINS ?? '')
    .split(',')
    .map((o) => o.trim())
    .filter(Boolean);

// basePath('/cms') — 내부 API URL 조립에 사용
const CMS_BASE_PATH = process.env.NEXT_PUBLIC_CMS_BASE_PATH ?? '/cms';
const INTERNAL_SECRET = process.env.INTERNAL_API_SECRET ?? '';

function setCorsHeaders(res: NextResponse, origin: string): void {
    res.headers.set('Access-Control-Allow-Origin', origin);
    res.headers.append('Vary', 'Origin');
    res.headers.set('Access-Control-Allow-Credentials', 'true');
    res.headers.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.headers.set('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-deploy-token');
    res.headers.set('Access-Control-Max-Age', '86400');
}

/**
 * /deployed/{pageId}.html 요청에 대해 IS_PUBLIC 상태를 확인한다.
 * 내부 API(/api/internal/page-public/{pageId})를 호출해 DB 값을 조회하며,
 * IS_PUBLIC='N'이면 점검 페이지로 redirect, 'Y'이면 null을 반환해 정상 서빙을 허용한다.
 *
 * fail-open 정책: 내부 API 호출 실패(DB 장애 등) 시 차단하지 않고 null 반환.
 */
async function checkEmergencyBlock(req: NextRequest): Promise<NextResponse | null> {
    const { pathname } = req.nextUrl;

    // /deployed/{pageId}.html 패턴 — 영숫자·하이픈·언더스코어로 구성된 pageId만 허용
    const match = pathname.match(/^\/deployed\/([a-zA-Z0-9_-]+)\.html$/);
    if (!match) return null;

    const pageId = match[1];

    // 타임아웃 500ms — 지연 발생 시 TTFB 저하 방지, 초과 시 fail-open으로 정상 서빙
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 500);

    try {
        // 내부 API는 같은 서버의 Node.js 런타임에서 실행됨
        // pathname이 /api/internal/... 이므로 이 middleware의 deployed 체크에 재진입하지 않음
        const checkUrl = `${req.nextUrl.origin}${CMS_BASE_PATH}/api/internal/page-public/${pageId}`;
        const res = await fetch(checkUrl, {
            headers: { 'x-internal-secret': INTERNAL_SECRET },
            signal: controller.signal,
        });

        if (res.ok) {
            const data = (await res.json()) as { data?: { isPublic?: string } };
            if (data.data?.isPublic === 'N') {
                // 점검 페이지로 리다이렉트 — basePath 포함
                return NextResponse.redirect(
                    new URL(`${CMS_BASE_PATH}/maintenance`, req.nextUrl.origin),
                );
            }
        }
    } catch {
        // DB 장애·네트워크 오류·타임아웃 시 fail-open: 기존 파일을 그대로 서빙
    } finally {
        clearTimeout(timeoutId);
    }

    return null;
}

export async function middleware(req: NextRequest): Promise<NextResponse> {
    // ── 긴급차단 체크 (#308) — /deployed/*.html 요청만 대상 ──
    const blockRedirect = await checkEmergencyBlock(req);
    if (blockRedirect) return blockRedirect;

    // ── CORS 처리 ──
    const origin = req.headers.get('origin') ?? '';
    const isAllowed = ALLOWED_ORIGINS.length > 0 && ALLOWED_ORIGINS.includes(origin);

    // Preflight 요청 처리
    if (req.method === 'OPTIONS') {
        const res = new NextResponse(null, { status: 204 });
        if (isAllowed) setCorsHeaders(res, origin);
        return res;
    }

    const res = NextResponse.next();
    if (isAllowed) setCorsHeaders(res, origin);
    return res;
}

export const config = {
    matcher: [
        // API 라우트 및 페이지 라우트 모두 적용 (정적 파일·_next 제외)
        '/((?!_next/static|_next/image|favicon.ico).*)',
    ],
};
