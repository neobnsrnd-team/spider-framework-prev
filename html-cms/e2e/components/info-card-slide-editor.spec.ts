// e2e/components/info-card-slide-editor.spec.ts
// InfoCardSlideEditor E2E via dedicated harness page

import { expect, test } from '@playwright/test';

test.describe('InfoCardSlideEditor', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/dev/info-card-slide-editor-e2e');
        await page.waitForLoadState('domcontentloaded');
    });

    test('shows empty card width and height by default', async ({ page }) => {
        await page.getByTestId('open-info-card-slide-editor').click();
        await expect(page.getByTestId('info-card-slide-editor')).toBeVisible();

        // liveWidth/liveHeight fallback 제거 — 명시적으로 입력하지 않으면 빈 값
        // (렌더링 너비를 자동 저장하면 inner div에 고정 px가 박혀 100% 확장을 막는 부작용 있었음)
        await expect(page.getByTestId('card-width-0')).toHaveValue('');
        await expect(page.getByTestId('card-height-0')).toHaveValue('');
    });

    test('reflects latest canvas text when reopening the editor', async ({ page }) => {
        await page.getByTestId('info-card-slide-block').evaluate((el) => {
            const directTitle = el.querySelector('[data-card-item] [data-card-title]');
            if (directTitle) directTitle.textContent = '캔버스 최신 제목';
        });

        await page.getByTestId('open-info-card-slide-editor').click();
        await expect(page.getByTestId('info-card-slide-editor')).toBeVisible();
        await expect(page.locator('input[placeholder="제목"]').first()).toHaveValue('캔버스 최신 제목');
    });

    test('keeps existing content when only card size is changed', async ({ page }) => {
        await page.getByTestId('open-info-card-slide-editor').click();
        await expect(page.getByTestId('info-card-slide-editor')).toBeVisible();

        await page.getByTestId('card-width-0').fill('320');
        await page.getByTestId('card-height-0').fill('260');
        await page.getByTestId('apply-info-card-slide-editor').click();

        const payload = await page.getByTestId('info-card-slide-block').evaluate((el) => {
            const raw = el.getAttribute('data-card-slides');
            return raw ? JSON.parse(raw) : null;
        });

        expect(payload).not.toBeNull();
        expect(payload[0].title).toBe('IBK 신용카드 혜택');
        expect(payload[0].subtitle).toBe('연회비 무료 이벤트 진행 중');
        expect(payload[0].infoLines).toEqual(['유효기간: 2024.12.31']);
        expect(payload[0].buttons.map((button: { label: string }) => button.label)).toEqual(['자세히 보기', '신청하기']);
        expect(payload[0].widthPx).toBe(320);
        expect(payload[0].heightPx).toBe(260);

        const rendered = await page.getByTestId('info-card-slide-block').evaluate((el) => {
            const firstCardItem = el.querySelector('[data-card-item]') as HTMLElement | null;
            const firstCard = firstCardItem?.querySelector(':scope > div') as HTMLElement | null;
            const title = firstCardItem?.querySelector('[data-card-title]')?.textContent?.trim() ?? '';
            const subtitle =
                firstCardItem?.querySelector('span[style*="font-size:14px"], span[style*="font-size:15px"], span[style*="font-size:16px"]')
                    ?.textContent?.trim() ?? '';
            const infoLines = Array.from(
                firstCardItem?.querySelectorAll('span[style*="text-align:right"], span[style*="text-align:left"]') ?? [],
            ).map(
                (node) => node.textContent?.trim() ?? '',
            );
            const buttons = Array.from(
                firstCardItem?.querySelectorAll('a[style*="border-radius:8px"], a[style*="border-radius:10px"]') ?? [],
            ).map(
                (node) => node.textContent?.trim() ?? '',
            );

            return {
                title,
                subtitle,
                infoLines,
                buttons,
                minHeight: firstCard?.style.minHeight ?? '',
                widthStyle: firstCard?.style.width ?? '',
            };
        });

        expect(rendered.title).toBe('IBK 신용카드 혜택');
        expect(rendered.subtitle).toBe('연회비 무료 이벤트 진행 중');
        expect(rendered.infoLines).toContain('유효기간: 2024.12.31');
        expect(rendered.buttons).toEqual(['자세히 보기', '신청하기']);
        expect(rendered.minHeight).toBe('260px');
        expect(rendered.widthStyle.replace(/\s+/g, '')).toBe('min(100%,320px)');
    });
});
