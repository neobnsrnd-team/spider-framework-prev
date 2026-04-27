/**
 * 로그레벨조정 API 계약 테스트 — /api/log-level
 *
 * 검증 범위:
 * - 전체 로거 목록 조회 (스키마 검증)
 * - 로그 레벨 변경 (성공 / 유효하지 않은 레벨 / logName 빈값)
 * - Additivity 변경 (성공 / 유효하지 않은 값 / logName 빈값)
 *
 * 주의: 변경은 런타임 상태에만 적용되고 DB에 저장되지 않으므로
 *       teardown이 불필요하다.
 */

import { test, expect } from '@playwright/test';

const TEST_LOGGER = 'com.example.admin_demo';

test.describe('GET /api/log-level — 전체 로거 목록 조회', () => {

    test('HTTP 200과 배열 응답을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('각 로거 항목은 logName · logLevel · additivity · appender 필드를 가져야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');
        const body = await res.json();

        expect(body.data.length).toBeGreaterThan(0);
        const first = body.data[0];
        expect(first).toHaveProperty('logName');
        expect(first).toHaveProperty('logLevel');   // null 허용 (상속 상태)
        expect(first).toHaveProperty('additivity');
        expect(first).toHaveProperty('appender');
    });

    test('ROOT 로거가 목록에 포함되어야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');
        const body = await res.json();

        const root = body.data.find((l: { logName: string }) => l.logName === 'ROOT');
        expect(root).toBeTruthy();
    });
});

test.describe('PATCH /api/log-level/level — 로그 레벨 변경', () => {

    test('유효한 레벨로 변경 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'DEBUG' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('변경 후 목록 조회 시 새 레벨이 반영되어야 한다', async ({ request }) => {
        await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'WARN' },
        });

        const res = await request.get('/api/log-level');
        const body = await res.json();
        const logger = body.data.find((l: { logName: string }) => l.logName === TEST_LOGGER);
        expect(logger?.logLevel).toBe('WARN');
    });

    test('유효하지 않은 레벨 입력 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'INVALID' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('logName 빈값 입력 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/level', {
            data: { logName: '', level: 'INFO' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});

test.describe('PATCH /api/log-level/additivity — Additivity 변경', () => {

    test('Y로 변경 시 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/additivity', {
            data: { logName: TEST_LOGGER, additivity: 'Y' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('N으로 변경 후 목록 조회 시 반영되어야 한다', async ({ request }) => {
        await request.patch('/api/log-level/additivity', {
            data: { logName: TEST_LOGGER, additivity: 'N' },
        });

        const res = await request.get('/api/log-level');
        const body = await res.json();
        const logger = body.data.find((l: { logName: string }) => l.logName === TEST_LOGGER);
        expect(logger?.additivity).toBe('N');

        // teardown: 기본값(Y)으로 복원
        await request.patch('/api/log-level/additivity', {
            data: { logName: TEST_LOGGER, additivity: 'Y' },
        });
    });

    test('Y/N 이외의 값 입력 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/additivity', {
            data: { logName: TEST_LOGGER, additivity: 'X' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });

    test('logName 빈값 입력 시 HTTP 400을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/additivity', {
            data: { logName: '', additivity: 'Y' },
        });

        expect(res.status()).toBe(400);
        const body = await res.json();
        expect(body.success).toBe(false);
    });
});
