# E2E Tests

Playwright E2E tests running against a real Oracle database (`gvenzl/oracle-free:23-slim-faststart`).

## Prerequisites

- Docker Desktop (Oracle container)
- Node.js 20+
- JDK 17 + Maven (Spring Boot)

## Quick Start

```bash
# 1. Start Oracle container (~60s first time)
docker compose -f e2e/docker/docker-compose.yml up -d

# 2. Start Spring Boot with Oracle connection
mvn spring-boot:run -Dspring-boot.run.profiles=ci

# 3. Install and run tests
npm ci
npx playwright install --with-deps chromium
npm run test:e2e
```

## Structure

```
e2e/
├── setup/          auth.setup.ts              # Login + save storageState
├── smoke/          system.spec.ts             # Server, auth, SPA verification
├── pages/user/     user-list.spec.ts          # Search, table, sort, pagination
│                   user-crud.spec.ts          # Create, edit, delete
│                   user-actions.spec.ts       # Row action buttons
│                   user-menu.spec.ts          # Menu permission modal
├── fixtures/       test-accounts.ts           # Test credentials (sync with e2e-seed.sql)
│                   locale.ts                  # UI text labels
└── docker/         docker-compose.yml         # Oracle container config
                    init-oracle.sh             # Schema + seed data init
                    e2e-seed.sql               # E2E test account + permissions
```

## Test Account

| Field | Value |
|-------|-------|
| userId | `e2e-admin` |
| password | `Test1234!` |
| role | ADMIN (all menus, WRITE) |

Defined in `e2e/docker/e2e-seed.sql`, referenced from `e2e/fixtures/test-accounts.ts`.

## Playwright Projects

Tests run in dependency order: `auth-setup` → `smoke` → `pages`.

```bash
npm run test:e2e          # All projects (headless)
npm run test:e2e:headed   # With browser visible
npm run test:e2e:report   # Open HTML report
```

## Oracle Container

```bash
docker compose -f e2e/docker/docker-compose.yml up -d    # Start
docker compose -f e2e/docker/docker-compose.yml down      # Stop
docker compose -f e2e/docker/docker-compose.yml down -v   # Stop + delete data
```

Init process:
1. `init-oracle.sh` grants `CREATE TABLE` + `UNLIMITED TABLESPACE` to `spider` user
2. Executes DDL (`01_create_tables.sql`) → seed data (`03_insert_initial_data.sql`) → E2E seed (`e2e-seed.sql`)
3. CRLF→LF conversion handles Windows line endings automatically

## CI

GitHub Actions runs the same Oracle container as a service. See `.github/workflows/ci.yml` (`playwright` job).
GHA doesn't support volume mounts for service containers, so `docker cp` + `docker exec` replicate the init-oracle.sh logic.

## Adding Tests for New Pages

1. Create `e2e/pages/{domain}/{domain}-*.spec.ts`
2. Tests auto-discovered by the `pages` project in `playwright.config.ts`
3. Use `e2e/fixtures/locale.ts` for UI text, `test-accounts.ts` for credentials
