/**
 * E2E Test Accounts — Single source of truth.
 *
 * These credentials match the seed data in e2e/docker/e2e-seed.sql (Oracle).
 * Both files MUST stay in sync. If you change a value here, update e2e-seed.sql too.
 */

export const ADMIN = {
    userId: 'e2e-admin',
    userName: 'E2E 관리자',
    password: 'Test1234!',
    roleId: 'ADMIN',
} as const;

export const READONLY = {
    userId: 'e2e-readonly',
    userName: 'E2E 읽기전용',
    password: 'Test1234!',
} as const;
