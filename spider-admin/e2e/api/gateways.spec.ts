/**
 * Gateway 관리 API 계약 테스트 — /api/gateways
 *
 * 검증 범위:
 * - 페이지네이션 조회 (목록, searchField=gwName/gwId, ioType 필터)
 * - 단건 조회 (존재/미존재) — GatewayDetailResponse(gateway + systems)
 * - 저장 (생성/수정 upsert) — POST /with-systems
 * - 엑셀 내보내기
 * - 비인증 요청
 *
 * 참고:
 * - Gateway는 삭제 API가 없으므로 테스트 데이터 정리 불가.
 *   생성 테스트는 타임스탬프 기반 고유 ID를 사용하며 DB에 누적된다.
 * - 시드 데이터: E2E-LIS-GW (ioType=I), E2E-CON-GW (ioType=O)
 */

import { test, expect } from '@playwright/test';

// GW ID 패턴: ^[A-Za-z0-9_]+$ — 하이픈 불가
const PREFIX = 'e2eGw';
let seq = 0;
function uniqueId() { return PREFIX + Date.now().toString(36) + (seq++); }

function gatewayData(id: string, overrides: Record<string, unknown> = {}) {
    return {
        gateway: {
            gwId: id,
            gwName: overrides.gwName ?? 'E2E Gateway',
            threadCount: overrides.threadCount ?? 1,
            ioType: overrides.ioType ?? 'O',
            gwDesc: overrides.gwDesc ?? 'E2E 테스트 Gateway',
            gwAppName: overrides.gwAppName ?? 'com.test.Gateway',
            gwProperties: overrides.gwProperties ?? null,
        },
    };
}

// ─── GET /api/gateways/page — 페이지네이션 조회 ──────────────────────────────

test.describe('GET /api/gateways/page — 페이지네이션 조회', () => {

    test('조회 시 HTTP 200과 PageResponse 스키마를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/page', { params: { page: 1, size: 5 } });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('content');
        expect(data).toHaveProperty('totalElements');
        expect(data).toHaveProperty('currentPage');
        expect(data).toHaveProperty('totalPages');
        expect(data).toHaveProperty('size');
        expect(Array.isArray(data.content)).toBe(true);
    });

    test('searchField=gwName 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/page', {
            params: { page: 1, size: 10, searchField: 'gwName', searchValue: 'E2E 리스너 GW' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const match = body.data.content.find((g: { gwId: string }) => g.gwId === 'E2E-LIS-GW');
        expect(match).toBeTruthy();
    });

    test('searchField=gwId 필터 적용 시 일치하는 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/page', {
            params: { page: 1, size: 10, searchField: 'gwId', searchValue: 'E2E-CON-GW' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const match = body.data.content.find((g: { gwId: string }) => g.gwId === 'E2E-CON-GW');
        expect(match).toBeTruthy();
    });

    test('ioType=I 필터 적용 시 리스너(수신) 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/page', {
            params: { page: 1, size: 20, ioType: 'I' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const items: Array<{ ioType: string }> = body.data.content;
        items.forEach(item => expect(item.ioType).toBe('I'));
    });

    test('ioType=O 필터 적용 시 어댑터(송신) 결과만 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/page', {
            params: { page: 1, size: 20, ioType: 'O' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const items: Array<{ ioType: string }> = body.data.content;
        items.forEach(item => expect(item.ioType).toBe('O'));
    });
});

// ─── GET /api/gateways/:gwId — 단건 조회 ─────────────────────────────────────

test.describe('GET /api/gateways/:gwId — 단건 조회', () => {

    test('존재하는 Gateway 조회 시 HTTP 200과 상세 정보(gateway + systems)를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/E2E-LIS-GW');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        const data = body.data;
        expect(data).toHaveProperty('gateway');
        expect(data).toHaveProperty('systems');
        expect(data.gateway.gwId).toBe('E2E-LIS-GW');
        expect(data.gateway.ioType).toBe('I');
        expect(Array.isArray(data.systems)).toBe(true);
    });

    test('존재하지 않는 Gateway 조회 시 HTTP 404를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/no-such-gateway');

        expect(res.status()).toBe(404);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── POST /api/gateways/with-systems — 생성/수정 (upsert) ────────────────────

test.describe('POST /api/gateways/with-systems — 저장 (upsert)', () => {

    test('신규 Gateway 저장 시 HTTP 200을 반환하고 조회 가능해야 한다', async ({ request }) => {
        const id = uniqueId();

        const res = await request.post('/api/gateways/with-systems', { data: gatewayData(id) });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        // 저장된 데이터 조회 확인 (cleanup 불가 — 누적 허용)
        const getRes = await request.get(`/api/gateways/${id}`);
        expect(getRes.status()).toBe(200);
        const getBody = await getRes.json();
        expect(getBody.data.gateway.gwId).toBe(id);
    });

    test('기존 Gateway 수정 시 HTTP 200을 반환하고 변경사항이 반영되어야 한다', async ({ request }) => {
        const id = uniqueId();
        // 먼저 생성
        await request.post('/api/gateways/with-systems', { data: gatewayData(id, { gwName: 'Before' }) });

        // 수정
        const res = await request.post('/api/gateways/with-systems', {
            data: gatewayData(id, { gwName: 'After' }),
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        // 변경된 이름 확인
        const getRes = await request.get(`/api/gateways/${id}`);
        expect(getRes.status()).toBe(200);
        const getBody = await getRes.json();
        expect(getBody.data.gateway.gwName).toBe('After');
    });

    test('필수 항목(gwId) 누락 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.post('/api/gateways/with-systems', {
            data: { gateway: { gwName: 'Missing ID', threadCount: 1, ioType: 'O' } },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('유효하지 않은 ioType 값 전송 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        const res = await request.post('/api/gateways/with-systems', {
            data: gatewayData(id, { ioType: 'X' }),
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('threadCount가 범위(1~999)를 벗어나면 HTTP 400을 반환해야 한다', async ({ request }) => {
        const id = uniqueId();

        const res = await request.post('/api/gateways/with-systems', {
            data: gatewayData(id, { threadCount: 1000 }),
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

// ─── GET /api/gateways/export — 엑셀 내보내기 ───────────────────────────────

test.describe('GET /api/gateways/export — 엑셀 내보내기', () => {

    test('요청 시 Content-Type xlsx와 attachment 헤더를 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/gateways/export');

        expect(res.status()).toBe(200);

        const contentType = res.headers()['content-type'] ?? '';
        expect(contentType).toContain('spreadsheetml');

        const disposition = res.headers()['content-disposition'] ?? '';
        expect(disposition).toContain('attachment');
        expect(disposition).toMatch(/\.xlsx/);
    });
});

// ─── 인증 검증 — 비인증 요청 ─────────────────────────────────────────────────

test.describe('인증 검증 — 비인증 요청', () => {

    test('비인증 상태로 목록 조회 시 HTTP 401 또는 302를 반환해야 한다', async () => {
        const res = await fetch('http://localhost:8080/api/gateways/page', { redirect: 'manual' });
        expect([302, 401]).toContain(res.status);
    });
});
