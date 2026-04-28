// scripts/migrate-event-banner-to-html.ts
// event-banner 이벤트 배너 컴포넌트 등록/업데이트 (Issue #293)
// 실행: npx tsx scripts/migrate-event-banner-to-html.ts

import 'dotenv/config';
import { getComponentById, updateComponent, createComponent } from '../src/db/repository/component.repository';
import { closePool } from '../src/db/connection';

const FONT_FAMILY = "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif";

// ── 데이터 모델 ──────────────────────────────────────────────────────────

interface BannerSlide {
    imageUrl: string;
    linkHref?: string;
    altText?: string;
    overlayTitle?: string;
    overlayDesc?: string;
}

// ── href 보안 처리 ───────────────────────────────────────────────────────

function sanitizeHref(url: string): string {
    const trimmed = url.trim();
    if (/^(https?:\/\/|\/|#)/.test(trimmed)) {
        return trimmed.replace(/"/g, '&quot;');
    }
    return '#';
}

/** HTML 특수문자 이스케이프 — XSS 방지 */
function escapeHtml(str: string): string {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── 슬라이드 HTML 빌더 ───────────────────────────────────────────────────

function buildSlideHtml(slide: BannerSlide, idx: number): string {
    // 이미지 영역 (없으면 플레이스홀더)
    const imgHtml = slide.imageUrl
        ? `<img src="${escapeHtml(slide.imageUrl)}" alt="${escapeHtml(slide.altText ?? '')}" style="width:100%;aspect-ratio:16/9;object-fit:contain;display:block;" />`
        : `<div style="width:100%;aspect-ratio:16/9;background:#E5E7EB;display:flex;align-items:center;justify-content:center;"><span style="color:#9CA3AF;font-size:14px;">이미지를 추가하세요</span></div>`;

    // 오버레이 텍스트 (선택)
    const overlayHtml = (slide.overlayTitle || slide.overlayDesc)
        ? `<div data-banner-overlay style="position:absolute;bottom:0;left:0;right:0;padding:16px;` +
          `background:linear-gradient(to top,rgba(0,0,0,0.55) 0%,transparent 100%);">` +
          (slide.overlayTitle
              ? `<p data-banner-overlay-title style="margin:0 0 4px;font-size:16px;font-weight:700;color:#fff;">${escapeHtml(slide.overlayTitle)}</p>`
              : '') +
          (slide.overlayDesc
              ? `<p data-banner-overlay-desc style="margin:0;font-size:13px;color:rgba(255,255,255,0.85);">${escapeHtml(slide.overlayDesc)}</p>`
              : '') +
          `</div>`
        : '';

    return (
        `<div data-banner-item data-banner-idx="${idx}" style="flex-shrink:0;width:100%;">` +
            `<a href="${sanitizeHref(slide.linkHref ?? '#')}" style="display:block;position:relative;text-decoration:none;">` +
                imgHtml +
                overlayHtml +
            `</a>` +
        `</div>`
    );
}

// ── 인라인 스크립트 — 가로 스크롤 변환 + 자동재생 + 페이지네이션 ────────

const BANNER_SCRIPT =
    `<script>` +
    `(function(){` +
    `var root=document.currentScript&&document.currentScript.closest('[data-spw-block]');` +
    `if(!root||root.getAttribute('data-banner-inited')==='1')return;` +
    `if(root.closest('.is-builder'))return;` +
    `root.setAttribute('data-banner-inited','1');` +
    `var track=root.querySelector('[data-banner-track]');` +
    `if(!track)return;` +
    // 세로 나열 → 가로 스크롤 변환
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;` +
    `-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;';` +
    // 스크롤바 완전 숨김 (webkit)
    `var styleId='eb-hide-'+Math.random().toString(36).slice(2,8);` +
    `track.setAttribute('data-eb-id',styleId);` +
    `var styleEl=document.createElement('style');` +
    `styleEl.textContent='[data-eb-id=\"'+styleId+'\"]::-webkit-scrollbar{display:none}';` +
    `root.appendChild(styleEl);` +
    // 각 슬라이드: 100% 너비 + snap start (한 번에 하나만 표시)
    `var items=track.querySelectorAll('[data-banner-item]');` +
    `items.forEach(function(item){item.style.flex='0 0 100%';item.style.scrollSnapAlign='start';});` +
    `var total=items.length;` +
    `var current=0;` +
    // 페이지네이션 요소
    `var indicator=root.querySelector('[data-banner-indicator]');` +
    `var prevBtn=root.querySelector('[data-banner-prev]');` +
    `var nextBtn=root.querySelector('[data-banner-next]');` +
    `var pauseBtn=root.querySelector('[data-banner-pause]');` +
    `var interval=parseInt(root.getAttribute('data-banner-interval')||'3000',10);` +
    `var timer=null;var paused=false;` +
    // 슬라이드 이동
    `function goTo(idx){` +
    `current=(idx%total+total)%total;` +
    `track.scrollTo({left:track.offsetWidth*current,behavior:'smooth'});` +
    `if(indicator)indicator.textContent=(current+1)+' / '+total;` +
    `}` +
    // 자동재생 제어
    `function startTimer(){if(paused)return;timer=setInterval(function(){if(!document.contains(root)){stopTimer();return;}goTo(current+1);},interval);}` +
    `function stopTimer(){clearInterval(timer);timer=null;}` +
    `startTimer();` +
    // 이전/다음 버튼
    `if(prevBtn)prevBtn.addEventListener('click',function(){stopTimer();goTo(current-1);startTimer();});` +
    `if(nextBtn)nextBtn.addEventListener('click',function(){stopTimer();goTo(current+1);startTimer();});` +
    // 일시정지/재생 토글
    `if(pauseBtn)pauseBtn.addEventListener('click',function(){` +
    `paused=!paused;` +
    `if(paused){stopTimer();pauseBtn.innerHTML='&#9654;';}` +
    `else{pauseBtn.innerHTML='&#10073;&#10073;';startTimer();}` +
    `});` +
    // 호버 시 자동재생 일시정지
    `root.addEventListener('mouseover',function(){stopTimer();});` +
    `root.addEventListener('mouseleave',function(){if(!paused)startTimer();});` +
    `})();` +
    `</script>`;

// ── 전체 HTML 조립 ───────────────────────────────────────────────────────

function buildEventBannerHtml(slides: BannerSlide[], componentId: string, interval: number, extraStyle: string): string {
    const slidesJson = escapeHtml(JSON.stringify(slides));
    const slidesHtml = slides.map((s, i) => buildSlideHtml(s, i)).join('');
    const total = slides.length;

    return (
        `<div data-component-id="${componentId}" data-spw-block` +
        ` data-banner-slides="${slidesJson}"` +
        ` data-banner-interval="${interval}"` +
        ` style="font-family:${FONT_FAMILY};background:#ffffff;${extraStyle}">` +
            `<div data-banner-track style="display:flex;flex-direction:column;">` +
                slidesHtml +
            `</div>` +
            `<div data-banner-pagination style="display:flex;align-items:center;justify-content:center;gap:8px;padding:8px 0;">` +
                `<button data-banner-prev style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:14px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10094;</button>` +
                `<span data-banner-indicator style="font-size:13px;color:#6B7280;min-width:40px;text-align:center;">1 / ${total}</span>` +
                `<button data-banner-next style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:14px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10095;</button>` +
                `<button data-banner-pause style="background:none;border:none;cursor:pointer;min-width:44px;min-height:44px;font-size:16px;color:#6B7280;display:inline-flex;align-items:center;justify-content:center;">&#10073;&#10073;</button>` +
            `</div>` +
            BANNER_SCRIPT +
        `</div>`
    );
}

// ── 기본 프리셋 데이터 ───────────────────────────────────────────────────

const DEFAULT_SLIDES: BannerSlide[] = [
    { imageUrl: '', linkHref: '#', overlayTitle: '이벤트 배너 제목', overlayDesc: '이벤트 설명 텍스트' },
    { imageUrl: '', linkHref: '#', overlayTitle: '두 번째 배너' },
    { imageUrl: '', linkHref: '#' },
];

const DEFAULT_INTERVAL = 3000;

// ── 3 variant ────────────────────────────────────────────────────────────

const VIEW_MODES = ['mobile', 'web', 'responsive'] as const;

const EXTRA_STYLES: Record<string, string> = {
    mobile: '',
    web: 'width:100%;box-sizing:border-box;',
    responsive: 'width:100%;box-sizing:border-box;',
};

const VARIANTS = VIEW_MODES.map((viewMode) => ({
    id: `event-banner-${viewMode}`,
    html: buildEventBannerHtml(DEFAULT_SLIDES, `event-banner-${viewMode}`, DEFAULT_INTERVAL, EXTRA_STYLES[viewMode]),
    viewMode,
}));

const COMPONENT_LABEL = '이벤트 배너';
const COMPONENT_DESC = '자동재생 이미지 배너 슬라이드';

// ── DB 등록 ───────────────────────────────────────────────────────────────

async function main() {
    for (const variant of VARIANTS) {
        const existing = await getComponentById(variant.id);

        if (existing) {
            await updateComponent({
                componentId:        variant.id,
                componentType:      existing.COMPONENT_TYPE,
                viewMode:           existing.VIEW_MODE,
                componentThumbnail: existing.COMPONENT_THUMBNAIL ?? undefined,
                data: {
                    ...(existing.DATA ?? {}) as Record<string, unknown>,
                    id:          'event-banner',
                    label:       COMPONENT_LABEL,
                    description: COMPONENT_DESC,
                    preview:     '/assets/minimalist-blocks/preview/event-banner.svg',
                    html:        variant.html,
                    viewMode:    variant.viewMode,
                },
                lastModifierId: 'system',
            });
            console.log(`✅ ${variant.id} — UPDATE 완료`);
        } else {
            await createComponent({
                componentId:        variant.id,
                componentType:      'finance',
                viewMode:           variant.viewMode,
                componentThumbnail: '/assets/minimalist-blocks/preview/event-banner.svg',
                data: {
                    id:          'event-banner',
                    label:       COMPONENT_LABEL,
                    description: COMPONENT_DESC,
                    preview:     '/assets/minimalist-blocks/preview/event-banner.svg',
                    html:        variant.html,
                    viewMode:    variant.viewMode,
                },
                createUserId:   'system',
                createUserName: '시스템',
            });
            console.log(`✅ ${variant.id} — INSERT 완료`);
        }
    }
    await closePool();
}

main().catch((err: unknown) => { console.error('실패:', err); process.exit(1); });
