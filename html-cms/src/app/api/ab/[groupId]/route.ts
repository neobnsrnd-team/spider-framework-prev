// src/app/api/ab/[groupId]/route.ts
// A/B 테스트 라우팅 — Weighted Random Selection + 쿠키 기반 Sticky Session

import { NextRequest, NextResponse } from 'next/server';

import { getAbGroup } from '@/db/repository/page.repository';
import { getServerList } from '@/db/repository/file-send.repository';
import { getErrorMessage } from '@/lib/api-response';
import { buildServerUrl } from '@/lib/deploy-utils';

const COOKIE_PREFIX = 'ab_';
const COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30일

/** Weighted Random Selection — 가중치 합계 대비 무작위 선택 */
function pickByWeight(pages: { PAGE_ID: string; AB_WEIGHT: number | null; IS_PUBLIC: string }[]): string | null {
    // 활성(IS_PUBLIC='Y') + 가중치 > 0 인 페이지만 대상
    const candidates = pages.filter((p) => p.IS_PUBLIC === 'Y' && (p.AB_WEIGHT ?? 0) > 0);
    if (candidates.length === 0) return null;

    const total = candidates.reduce((sum, p) => sum + (p.AB_WEIGHT ?? 0), 0);
    if (total <= 0) return null;

    let rand = Math.random() * total;
    for (const p of candidates) {
        rand -= p.AB_WEIGHT ?? 0;
        if (rand <= 0) return p.PAGE_ID;
    }
    // 부동소수점 오차 방어
    return candidates[candidates.length - 1].PAGE_ID;
}

/**
 * GET /api/ab/[groupId]
 * - 쿠키에 이미 배정된 pageId가 있고 현재 그룹에 유효하면 그 페이지로 리다이렉트
 * - 없거나 유효하지 않으면 Weighted Random Selection 후 쿠키 저장 및 리다이렉트
 * - 활성 운영 서버(FWK_CMS_SERVER_INSTANCE)의 /cms/deployed/{pageId}.html 절대 URL로 302 리다이렉트
 * - 그룹 내 활성 페이지가 없으면 404 반환
 */
export async function GET(req: NextRequest, { params }: { params: Promise<{ groupId: string }> }) {
    try {
        const { groupId } = await params;

        const pages = await getAbGroup(groupId);

        if (pages.length === 0) {
            return NextResponse.json({ ok: false, error: 'A/B 그룹을 찾을 수 없습니다.' }, { status: 404 });
        }

        const cookieKey = `${COOKIE_PREFIX}${groupId}`;
        const savedPageId = req.cookies.get(cookieKey)?.value ?? null;

        // 쿠키 유효성 검증 — Winner 선정 후 기존 pageId가 그룹에 존재하는지 확인
        const isValid =
            savedPageId !== null &&
            pages.some((p) => p.PAGE_ID === savedPageId && p.IS_PUBLIC === 'Y' && (p.AB_WEIGHT ?? 0) > 0);

        const targetPageId = isValid ? savedPageId! : pickByWeight(pages);

        if (!targetPageId) {
            return NextResponse.json({ ok: false, error: '노출 가능한 페이지가 없습니다.' }, { status: 404 });
        }

        // 활성 서버 중 운영(localhost 가 아닌) 서버 우선 선택 — 시드 환경에 React 데모 서버
        // (localhost:5173/5174) 가 ALIVE_YN='Y' 로 함께 존재할 때 단순히 servers[0] 를 쓰면
        // 이름 가나다 정렬 영향으로 데모 서버가 잡히는 문제를 방어한다.
        // IPv6 루프백(::1)도 제외하고, 폴백에서도 INSTANCE_IP 가 비어있는 서버를 걸러
        // 'http:///...' 형태의 잘못된 redirect URL 생성을 막는다.
        const servers = await getServerList('Y');
        const operationServer =
            servers.find((s) => !!s.INSTANCE_IP && !/^(localhost|127\.0\.0\.1|::1)$/i.test(s.INSTANCE_IP)) ??
            servers.find((s) => !!s.INSTANCE_IP);
        if (!operationServer) {
            return NextResponse.json({ ok: false, error: '활성화된 운영 서버가 없습니다.' }, { status: 503 });
        }
        const redirectUrl = new URL(
            buildServerUrl(
                operationServer.INSTANCE_IP,
                operationServer.INSTANCE_PORT,
                `/cms/deployed/${targetPageId}.html`,
            ),
        );

        const res = NextResponse.redirect(redirectUrl, 302);

        // 신규 배정 시에만 쿠키 저장
        if (!isValid) {
            res.cookies.set(cookieKey, targetPageId, {
                httpOnly: true,
                sameSite: 'lax',
                maxAge: COOKIE_MAX_AGE,
                path: '/',
            });
        }

        return res;
    } catch (err: unknown) {
        console.error('A/B 라우팅 실패:', err);
        return NextResponse.json({ ok: false, error: getErrorMessage(err) }, { status: 500 });
    }
}
