import { defineConfig, devices } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// Load .env file (no dotenv dependency needed)
const envPath = path.resolve(__dirname, '.env');
if (fs.existsSync(envPath)) {
    for (const line of fs.readFileSync(envPath, 'utf-8').split('\n')) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const eqIdx = trimmed.indexOf('=');
        if (eqIdx === -1) continue;
        const key = trimmed.slice(0, eqIdx).trim();
        const val = trimmed.slice(eqIdx + 1).trim();
        if (!process.env[key]) {
            process.env[key] = val;
        }
    }
}

export default defineConfig({
    testDir: './e2e',
    timeout: 10_000,
    expect: { timeout: 5_000 },
    fullyParallel: false,   // 세션 공유 때문에 순차 실행
    retries: 0,
    reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }], ['junit', { outputFile: 'playwright-results/results.xml' }]],

    use: {
        baseURL: 'http://localhost:8080',
        trace: 'on-first-retry',
    },

    projects: [
        // 1) Auth setup — runs first, saves storageState
        {
            name: 'auth-setup',
            testDir: './e2e/setup',
            testMatch: /auth\.setup\.ts/,
            use: { ...devices['Desktop Chrome'] },
        },
        // 2) Smoke tests — depend on auth
        {
            name: 'smoke',
            testDir: './e2e/smoke',
            dependencies: ['auth-setup'],
            use: {
                ...devices['Desktop Chrome'],
                storageState: 'e2e/.auth/session.json',
            },
        },
        // 3) API contract tests — depend on smoke (no browser needed)
        {
            name: 'api',
            testDir: './e2e/api',
            dependencies: ['smoke', 'readonly-setup'],
            use: {
                storageState: 'e2e/.auth/session.json',
            },
        },
        // 4) Readonly auth setup
        {
            name: 'readonly-setup',
            testDir: './e2e/setup',
            testMatch: /readonly\.setup\.ts/,
            use: { ...devices['Desktop Chrome'] },
        },
        // 5) Page tests — depend on smoke + readonly-setup
        {
            name: 'pages',
            testDir: './e2e/pages',
            dependencies: ['smoke', 'readonly-setup'],
            use: {
                ...devices['Desktop Chrome'],
                storageState: 'e2e/.auth/session.json',
            },
        },
    ],
});
