/**
 * 로그레벨조정 API 계약 테스트 — /api/log-level
 *
 * 검증 범위:
 * - 전체 로거 목록 조회 (스키마 검증)
 * - 로그 레벨 변경 (모든 유효 레벨 / null(상속) / 유효하지 않은 레벨 / logName 빈값)
 * - Additivity 변경 (성공 / 유효하지 않은 값 / logName 빈값)
 * - 신규(미존재) 로거 레벨 설정
 * - WAS 동기화 내성 — spider-link 미기동 시에도 Admin API 정상 동작
 *
 * 주의: 변경은 런타임 상태에만 적용되고 DB에 저장되지 않으므로
 *       teardown이 불필요하다.
 */

import { test, expect } from '@playwright/test';

const TEST_LOGGER = 'com.example.admin_demo';
const VALID_LEVELS = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'OFF'] as const;

test.describe('GET /api/log-level — 전체 로거 목록 조회', () => {

    test('HTTP 200과 배열 응답을 반환해야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
        expect(Array.isArray(body.data)).toBe(true);
    });

    test('각 로거 항목은 logName · logLevel · effectiveLevel · parentEffectiveLevel · additivity · appender 필드를 가져야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');
        const body = await res.json();

        expect(body.data.length).toBeGreaterThan(0);
        const first = body.data[0];
        expect(first).toHaveProperty('logName');
        expect(first).toHaveProperty('logLevel');             // null 허용 (상속 상태)
        expect(first).toHaveProperty('effectiveLevel');       // 항상 존재 (실제 적용 레벨)
        expect(first).toHaveProperty('parentEffectiveLevel'); // ROOT는 null, 나머지는 문자열
        expect(first).toHaveProperty('additivity');
        expect(first).toHaveProperty('appender');
    });

    test('ROOT 로거가 목록에 포함되어야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');
        const body = await res.json();

        const root = body.data.find((l: { logName: string }) => l.logName === 'ROOT');
        expect(root).toBeTruthy();
    });

    test('ROOT 로거의 effectiveLevel은 항상 값이 있어야 하고 parentEffectiveLevel은 null이어야 한다', async ({ request }) => {
        const res = await request.get('/api/log-level');
        const body = await res.json();

        const root = body.data.find((l: { logName: string }) => l.logName === 'ROOT');
        expect(root.effectiveLevel).toBeTruthy();
        expect(root.parentEffectiveLevel).toBeNull();
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

    test('level을 null로 변경하면 상속 상태로 복원되어야 한다', async ({ request }) => {
        // 먼저 명시적 레벨 설정
        await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'DEBUG' },
        });

        // null로 변경하여 상속 복원
        const res = await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: null },
        });
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);

        // 목록에서 logLevel이 null(상속)인지 확인
        const listRes = await request.get('/api/log-level');
        const listBody = await listRes.json();
        const logger = listBody.data.find((l: { logName: string }) => l.logName === TEST_LOGGER);
        expect(logger?.logLevel).toBeNull();
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

    // 모든 유효 레벨(6종) 각각 변경 가능한지 확인
    for (const level of VALID_LEVELS) {
        test(`${level} 레벨로 변경 시 HTTP 200과 목록 반영이 되어야 한다`, async ({ request }) => {
            const res = await request.patch('/api/log-level/level', {
                data: { logName: TEST_LOGGER, level },
            });
            expect(res.status()).toBe(200);

            const listRes = await request.get('/api/log-level');
            const listBody = await listRes.json();
            const logger = listBody.data.find((l: { logName: string }) => l.logName === TEST_LOGGER);
            expect(logger?.logLevel).toBe(level);
        });
    }

    test('존재하지 않는 로거에 레벨을 설정하면 Logback이 자동 생성하고 HTTP 200을 반환해야 한다', async ({ request }) => {
        // 타임스탬프로 충돌 없는 유일한 로거명 생성
        const newLogger = `com.example.e2e.new.${Date.now()}`;

        const res = await request.patch('/api/log-level/level', {
            data: { logName: newLogger, level: 'DEBUG' },
        });
        expect(res.status()).toBe(200);

        // 새 로거가 목록에 포함되어야 한다
        const listRes = await request.get('/api/log-level');
        const listBody = await listRes.json();
        const logger = listBody.data.find((l: { logName: string }) => l.logName === newLogger);
        expect(logger?.logLevel).toBe('DEBUG');
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

/**
 * WAS 동기화 내성 테스트
 *
 * Admin은 레벨 변경 후 spider-link에 동기화를 시도하지만 (SpiderLogLevelClient),
 * spider-link가 미기동 상태이거나 네트워크 오류가 발생해도 Admin API는
 * 정상적으로 HTTP 200을 반환해야 한다 (fire-and-forget, 예외 비전파).
 *
 * CI 환경에서 spider-link가 기동되지 않은 상태에서도 이 테스트가 통과해야 한다.
 */
test.describe('WAS 동기화 내성 — spider-link 미기동 시에도 Admin API 정상 동작', () => {

    test('레벨 변경 API는 spider-link 동기화 결과와 무관하게 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: 'INFO' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('Additivity 변경 API는 spider-link 동기화 결과와 무관하게 HTTP 200을 반환해야 한다', async ({ request }) => {
        const res = await request.patch('/api/log-level/additivity', {
            data: { logName: TEST_LOGGER, additivity: 'Y' },
        });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.success).toBe(true);
    });

    test('레벨 변경 후 Admin 자신의 로거에는 즉시 반영되어야 한다', async ({ request }) => {
        const targetLevel = 'TRACE';
        await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: targetLevel },
        });

        // spider-link 동기화 여부와 무관하게 Admin 자신의 Logback에는 반영됨
        const listRes = await request.get('/api/log-level');
        const listBody = await listRes.json();
        const logger = listBody.data.find((l: { logName: string }) => l.logName === TEST_LOGGER);
        expect(logger?.logLevel).toBe(targetLevel);

        // teardown
        await request.patch('/api/log-level/level', {
            data: { logName: TEST_LOGGER, level: null },
        });
    });
});
