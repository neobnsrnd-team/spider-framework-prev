// e2e/components/event-banner.spec.ts
// event-banner 컴포넌트 자동화 QA (Issue #322)

import { test, expect } from '@playwright/test';
import {
    runCommonChecks,
    checkNoHorizontalScroll,
    checkNotOutsideViewport,
    checkViewportLayouts,
    MOBILE_VIEWPORTS,
    WEB_VIEWPORTS,
} from '../helpers/component-checks';

// ── 인라인 CSS ────────────────────────────────────────────────────────────────

const EB_CSS = `
* { box-sizing: border-box; }
body { margin: 0; }
`;

const FONT_FAMILY = "-apple-system, BlinkMacSystemFont, 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif";

// ── 인라인 슬라이더 스크립트 (실제 컴포넌트와 동일) ─────────────────────────
// migrate-event-banner-to-html.ts 의 BANNER_SCRIPT 와 동기화
const BANNER_SCRIPT =
    `<script>` +
    `(function(){` +
    `var root=document.currentScript&&document.currentScript.closest('[data-spw-block]');` +
    `if(!root||root.getAttribute('data-banner-inited')==='1')return;` +
    `if(root.closest('.is-builder'))return;` +
    `root.setAttribute('data-banner-inited','1');` +
    `var track=root.querySelector('[data-banner-track]');` +
    `if(!track)return;` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;` +
    `-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;';` +
    `var styleId='eb-hide-'+Math.random().toString(36).slice(2,8);` +
    `track.setAttribute('data-eb-id',styleId);` +
    `var styleEl=document.createElement('style');` +
    `styleEl.textContent='[data-eb-id="'+styleId+'"]::-webkit-scrollbar{display:none}';` +
    `root.appendChild(styleEl);` +
    `var items=track.querySelectorAll('[data-banner-item]');` +
    `items.forEach(function(item){item.style.flex='0 0 100%';item.style.scrollSnapAlign='start';});` +
    `var total=items.length;` +
    `var current=0;` +
    `var indicator=root.querySelector('[data-banner-indicator]');` +
    `var prevBtn=root.querySelector('[data-banner-prev]');` +
    `var nextBtn=root.querySelector('[data-banner-next]');` +
    `var pauseBtn=root.querySelector('[data-banner-pause]');` +
    `var interval=parseInt(root.getAttribute('data-banner-interval')||'3000',10);` +
    `var timer=null;var paused=false;` +
    `function goTo(idx){` +
    `current=(idx%total+total)%total;` +
    `track.scrollTo({left:track.offsetWidth*current,behavior:'smooth'});` +
    `if(indicator)indicator.textContent=(current+1)+' / '+total;` +
    `}` +
    `function startTimer(){if(paused)return;timer=setInterval(function(){if(!document.contains(root)){stopTimer();return;}goTo(current+1);},interval);}` +
    `function stopTimer(){clearInterval(timer);timer=null;}` +
    `startTimer();` +
    `if(prevBtn)prevBtn.addEventListener('click',function(){stopTimer();goTo(current-1);startTimer();});` +
    `if(nextBtn)nextBtn.addEventListener('click',function(){stopTimer();goTo(current+1);startTimer();});` +
    `if(pauseBtn)pauseBtn.addEventListener('click',function(){` +
    `paused=!paused;` +
    `if(paused){stopTimer();pauseBtn.innerHTML='&#9654;';}` +
    `else{pauseBtn.innerHTML='&#10073;&#10073;';startTimer();}` +
    `});` +
    `root.addEventListener('mouseover',function(){stopTimer();});` +
    `root.addEventListener('mouseleave',function(){if(!paused)startTimer();});` +
    `})();` +
    `</script>`;

// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

interface BannerSlide {
    imageUrl: string;
    linkHref?: string;
    altText?: string;
    overlayTitle?: string;
    overlayDesc?: string;
}

const buildSlide = (slide: BannerSlide, idx: number): string => {
    const imgHtml = slide.imageUrl
        ? `<img src="${slide.imageUrl}" alt="${slide.altText ?? ''}" style="width:100%;aspect-ratio:16/9;object-fit:contain;display:block;">`
        : `<div style="width:100%;aspect-ratio:16/9;background:#E5E7EB;display:flex;align-items:center;justify-content:center;">` +
          `<span style="color:#9CA3AF;font-size:14px;">이미지를 추가하세요</span></div>`;

    const overlayHtml = (slide.overlayTitle || slide.overlayDesc)
        ? `<div data-banner-overlay style="position:absolute;bottom:0;left:0;right:0;padding:16px;` +
          `background:linear-gradient(to top,rgba(0,0,0,0.55) 0%,transparent 100%);">` +
          (slide.overlayTitle
              ? `<p data-banner-overlay-title style="margin:0 0 4px;font-size:16px;font-weight:700;color:#fff;">${slide.overlayTitle}</p>`
              : '') +
          (slide.overlayDesc
              ? `<p data-banner-overlay-desc style="margin:0;font-size:13px;color:rgba(255,255,255,0.85);">${slide.overlayDesc}</p>`
              : '') +
          `</div>`
        : '';

    return (
        `<div data-banner-item data-banner-idx="${idx}" style="flex-shrink:0;width:100%;">` +
        `<a href="${slide.linkHref ?? '#'}" style="display:block;position:relative;text-decoration:none;">` +
        imgHtml + overlayHtml +
        `</a></div>`
    );
};

type ViewMode = 'mobile' | 'web' | 'responsive';

const VIEW_MODE_EXTRA_STYLE: Record<ViewMode, string> = {
    mobile:     '',
    web:        'width:100%;box-sizing:border-box;',
    responsive: 'width:100%;box-sizing:border-box;',
};

const makeHtml = (slides: BannerSlide[], viewMode: ViewMode = 'mobile'): string => {
    const total = slides.length;
    const extraStyle = VIEW_MODE_EXTRA_STYLE[viewMode];
    return `<!DOCTYPE html><html><head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>${EB_CSS}</style>
</head><body>
  <div data-component-id="event-banner-${viewMode}" data-spw-block data-banner-interval="3000"
    style="font-family:${FONT_FAMILY};background:#ffffff;${extraStyle}">
    <div data-banner-track style="display:flex;flex-direction:column;">
      ${slides.map((s, i) => buildSlide(s, i)).join('')}
    </div>
    <div data-banner-pagination style="display:flex;align-items:center;justify-content:center;gap:8px;padding:8px 0;">
      <button data-banner-prev style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:14px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10094;</button>
      <span data-banner-indicator style="font-size:13px;color:#6B7280;min-width:40px;text-align:center;">1 / ${total}</span>
      <button data-banner-next style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:14px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10095;</button>
      <button data-banner-pause style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:16px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10073;&#10073;</button>
    </div>
    ${BANNER_SCRIPT}
  </div>
</body></html>`;
};

// ── HTML 상수 ─────────────────────────────────────────────────────────────────

const DEFAULT_SLIDES = [
    { imageUrl: '/test/banner1.jpg', altText: '이벤트 배너 1', linkHref: '#', overlayTitle: '이벤트 배너 제목', overlayDesc: '이벤트 설명 텍스트' },
    { imageUrl: '/test/banner2.jpg', altText: '이벤트 배너 2', linkHref: '#', overlayTitle: '두 번째 배너' },
];

// 정상 상태: 슬라이드 2개, 오버레이 텍스트 포함 (mobile 기준)
const NORMAL_HTML = makeHtml(DEFAULT_SLIDES);

// 뷰 모드별 HTML (반응형 뷰포트 레이아웃 테스트용)
const MOBILE_HTML     = makeHtml(DEFAULT_SLIDES, 'mobile');
const WEB_HTML        = makeHtml(DEFAULT_SLIDES, 'web');
const RESPONSIVE_HTML = makeHtml(DEFAULT_SLIDES, 'responsive');

// 슬라이드 1개
const SINGLE_HTML = makeHtml([
    { imageUrl: '/test/banner1.jpg', altText: '이벤트 배너 1', linkHref: '#', overlayTitle: '단일 배너' },
]);

// 이미지 URL 미설정 (플레이스홀더 케이스)
const EMPTY_URL_HTML = makeHtml([
    { imageUrl: '', altText: '', linkHref: '#', overlayTitle: '이미지 없는 배너' },
]);

// 세로형(portrait) 이미지 비율 처리 검증용
const PORTRAIT_HTML = makeHtml([
    { imageUrl: '/test/portrait.jpg', altText: '세로형 배너', linkHref: '#', overlayTitle: '세로 이미지 배너' },
]);

// ── 공통 체크 ─────────────────────────────────────────────────────────────────

test.describe('event-banner — 공통 체크', () => {
    test.beforeEach(async ({ page }) => {
        await page.setContent(NORMAL_HTML);
    });

    // eslint-disable-next-line playwright/expect-expect
    test('공통 레이아웃·접근성 기준 충족 (가로스크롤·뷰포트·폰트·터치·alt)', async ({ page }) => {
        await runCommonChecks(page, {
            componentIdPrefix: 'event-banner',
            // 슬라이드 링크 + 페이지네이션 제어 버튼(min-width/height 44px 보장) 터치 영역 체크
            buttonSelector: '[data-banner-item] a, [data-banner-prev], [data-banner-next], [data-banner-pause]',
            textSelector: '[data-banner-indicator]',
            minFontSize: 12,
        });
    });

    test('키보드 Tab 포커스 이동 가능 (슬라이드 링크 → 이전/다음/일시정지 버튼)', async ({ page }) => {
        // 슬라이드 링크가 포커스 가능한지 직접 확인
        await page.locator('[data-banner-item] a').first().focus();
        await expect(page.locator('[data-banner-item] a').first(), '슬라이드 링크에 포커스되어야 합니다').toBeFocused();

        // 마지막 슬라이드 링크 → 이전 버튼으로 Tab 이동
        await page.locator('[data-banner-item] a').last().focus();
        await page.keyboard.press('Tab');
        await expect(page.locator('[data-banner-prev]'), '이전 버튼에 Tab 포커스되어야 합니다').toBeFocused();

        // 이전 → 다음 → 일시정지 버튼 Tab 순서
        await page.keyboard.press('Tab');
        await expect(page.locator('[data-banner-next]'), '다음 버튼에 Tab 포커스되어야 합니다').toBeFocused();
        await page.keyboard.press('Tab');
        await expect(page.locator('[data-banner-pause]'), '일시정지 버튼에 Tab 포커스되어야 합니다').toBeFocused();
    });
});

// ── 정상 동작 ─────────────────────────────────────────────────────────────────

test.describe('event-banner — 정상 동작', () => {
    test.beforeEach(async ({ page }) => {
        await page.setContent(NORMAL_HTML);
    });

    test('루트 컴포넌트가 렌더링됨', async ({ page }) => {
        await expect(
            page.locator('[data-component-id^="event-banner"]'),
        ).toBeAttached();
    });

    test('슬라이드 트랙이 존재함', async ({ page }) => {
        await expect(
            page.locator('[data-component-id^="event-banner"] [data-banner-track]'),
        ).toBeAttached();
    });

    test('슬라이드 아이템 개수가 이미지 개수와 일치함 (2개)', async ({ page }) => {
        await expect(
            page.locator('[data-component-id^="event-banner"] [data-banner-item]'),
        ).toHaveCount(2);
    });

    test('인디케이터에 "1 / N" 형식으로 표시됨', async ({ page }) => {
        const text = await page.locator('[data-banner-indicator]').textContent();
        expect(text?.trim()).toMatch(/^1 \/ \d+$/);
    });

    test('이전/다음/일시정지 버튼이 존재함', async ({ page }) => {
        await expect(page.locator('[data-banner-prev]')).toBeAttached();
        await expect(page.locator('[data-banner-next]')).toBeAttached();
        await expect(page.locator('[data-banner-pause]')).toBeAttached();
    });

    test('슬라이드 링크에 href 속성이 있음', async ({ page }) => {
        const links = page.locator('[data-banner-item] a');
        await expect(links).toHaveCount(2);
        for (const link of await links.all()) {
            await expect(link).toHaveAttribute('href', /.+/);
        }
    });

    test('오버레이 제목 텍스트가 표시됨', async ({ page }) => {
        const title = page.locator('[data-banner-overlay-title]').first();
        await expect(title).toBeAttached();
        const text = await title.textContent();
        expect(text?.trim().length).toBeGreaterThan(0);
    });

    test('슬라이드 이미지에 alt 속성이 있음 (접근성)', async ({ page }) => {
        const images = page.locator('[data-banner-item] img');
        await expect(images).toHaveCount(2);
        for (const image of await images.all()) {
            await expect(image).toHaveAttribute('alt', /.*/);
        }
    });

    // eslint-disable-next-line playwright/expect-expect
    test('뷰포트 이탈 없음', async ({ page }) => {
        await checkNotOutsideViewport(page, '[data-component-id^="event-banner"]');
    });

    // eslint-disable-next-line playwright/expect-expect
    test('가로 스크롤 없음', async ({ page }) => {
        await checkNoHorizontalScroll(page);
    });
});

// ── 예외 처리 — 이미지 URL 미설정 ────────────────────────────────────────────

test.describe('event-banner — 예외 처리 (이미지 URL 미설정)', () => {
    test('플레이스홀더("이미지를 추가하세요") 텍스트가 표시됨', async ({ page }) => {
        await page.setContent(EMPTY_URL_HTML);
        const placeholder = page.locator('[data-banner-item] span');
        await expect(placeholder).toBeVisible();
        const text = await placeholder.textContent();
        expect(text?.trim()).toBe('이미지를 추가하세요');
    });

    test('이미지 없는 경우 — 컴포넌트 루트 존재, 가로 스크롤 없음', async ({ page }) => {
        await page.setContent(EMPTY_URL_HTML);
        await expect(
            page.locator('[data-component-id^="event-banner"]'),
        ).toBeAttached();
        await checkNoHorizontalScroll(page);
    });
});

// ── 예외 처리 — 슬라이드 1개 ─────────────────────────────────────────────────

test.describe('event-banner — 예외 처리 (슬라이드 1개)', () => {
    test('슬라이드 아이템 1개, 인디케이터 "1 / 1" 표시', async ({ page }) => {
        await page.setContent(SINGLE_HTML);
        await expect(
            page.locator('[data-banner-item]'),
        ).toHaveCount(1);
        const text = await page.locator('[data-banner-indicator]').textContent();
        expect(text?.trim()).toBe('1 / 1');
    });

    // eslint-disable-next-line playwright/expect-expect
    test('슬라이드 1개 — 가로 스크롤 없음', async ({ page }) => {
        await page.setContent(SINGLE_HTML);
        await checkNoHorizontalScroll(page);
    });
});

// ── 엣지 케이스 — 세로형(portrait) 이미지 ────────────────────────────────────

test.describe('event-banner — 세로형(portrait) 이미지 비율 처리', () => {
    test.beforeEach(async ({ page }) => {
        await page.setContent(PORTRAIT_HTML);
    });

    test('슬라이드 컨테이너가 16:9 비율을 유지함 (aspect-ratio CSS 강제)', async ({ page }) => {
        const img = page.locator('[data-banner-item] img').first();
        const box = await img.boundingBox();
        expect(box, '이미지 요소가 렌더링되어야 합니다').not.toBeNull();
        // aspect-ratio:16/9 → height/width ≈ 0.5625 (오차 ±0.01 허용)
        const ratio = box!.height / box!.width;
        expect(ratio, `16:9 비율(0.5625)을 유지해야 합니다 (실제: ${ratio.toFixed(4)})`).toBeCloseTo(9 / 16, 1);
    });

    test('이미지에 object-fit:contain이 적용됨 (비율 불일치 시 잘림 없이 전체 표시)', async ({ page }) => {
        const objectFit = await page.locator('[data-banner-item] img').first().evaluate(
            (el) => getComputedStyle(el).objectFit,
        );
        expect(objectFit, 'object-fit:contain으로 이미지 전체가 배너 영역 안에 표시되어야 합니다').toBe('contain');
    });

    // eslint-disable-next-line playwright/expect-expect
    test('세로형 이미지 — 가로 스크롤·뷰포트 이탈 없음', async ({ page }) => {
        await checkNoHorizontalScroll(page);
        await checkNotOutsideViewport(page, '[data-component-id^="event-banner"]');
    });
});

// ── 반응형 뷰어 ───────────────────────────────────────────────────────────────

test.describe('event-banner — 반응형 뷰어', () => {
    test('뷰어 — event-banner HTML이 올바르게 렌더링됨', async ({ page }) => {
        await page.setContent(NORMAL_HTML);
        await expect(
            page.locator('[data-component-id^="event-banner"]'),
        ).toBeAttached();
        await expect(
            page.locator('[data-banner-track]'),
        ).toBeVisible();
    });
});

// ── 모바일 뷰 레이아웃 (360~430px) ──────────────────────────────────────────

test.describe('event-banner — 모바일 뷰 레이아웃 (360~430px)', () => {
    // eslint-disable-next-line playwright/expect-expect
    test('Galaxy S / iPhone SE / iPhone Pro Max — 가로 스크롤·뷰포트 이탈 없음', async ({ page }) => {
        await page.setContent(MOBILE_HTML);
        await checkViewportLayouts(page, '[data-component-id^="event-banner"]', MOBILE_VIEWPORTS);
    });
});

// ── 웹 뷰 레이아웃 (767~1440px) ─────────────────────────────────────────────

test.describe('event-banner — 웹 뷰 레이아웃 (767~1440px)', () => {
    // eslint-disable-next-line playwright/expect-expect
    test('태블릿 경계~1440px 데스크탑 — 가로 스크롤·뷰포트 이탈 없음', async ({ page }) => {
        await page.setContent(WEB_HTML);
        await checkViewportLayouts(page, '[data-component-id^="event-banner"]', WEB_VIEWPORTS);
    });
});

// ── 반응형 뷰 레이아웃 (전체 구간) ──────────────────────────────────────────

test.describe('event-banner — 반응형 뷰 레이아웃 (360~1440px)', () => {
    // eslint-disable-next-line playwright/expect-expect
    test('전체 뷰포트 구간 — 가로 스크롤·뷰포트 이탈 없음', async ({ page }) => {
        await page.setContent(RESPONSIVE_HTML);
        await checkViewportLayouts(page, '[data-component-id^="event-banner"]');
    });
});
