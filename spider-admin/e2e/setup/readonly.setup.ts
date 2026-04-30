/**
 * Readonly auth setup — e2e-readonly 계정 세션을 저장한다.
 * Credentials: e2e/fixtures/test-accounts.ts (synced with e2e/docker/e2e-seed.sql).
 */

import { test as setup } from '@playwright/test';
import { READONLY } from '../fixtures/test-accounts';

const AUTH_FILE = 'e2e/.auth/session-readonly.json';

setup('authenticate as readonly', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[name="userId"]').fill(READONLY.userId);
    await page.locator('input[name="password"]').fill(READONLY.password);

    await Promise.all([
        page.waitForURL(/\/home/, { timeout: 8000 }),
        page.locator('button[type="submit"]').click(),
    ]);

    await page.context().storageState({ path: AUTH_FILE });
});
