/**
 * Global auth setup — runs once, saves storageState for all downstream tests.
 * Credentials come from e2e/fixtures/test-accounts.ts (synced with e2e/docker/e2e-seed.sql).
 */

import { test as setup } from '@playwright/test';
import { ADMIN } from '../fixtures/test-accounts';

const AUTH_FILE = 'e2e/.auth/session.json';

setup('authenticate', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[name="userId"]').fill(ADMIN.userId);
    await page.locator('input[name="password"]').fill(ADMIN.password);

    await Promise.all([
        page.waitForURL(/\/home/, { timeout: 8000 }),
        page.locator('button[type="submit"]').click(),
    ]);

    await page.context().storageState({ path: AUTH_FILE });
});
