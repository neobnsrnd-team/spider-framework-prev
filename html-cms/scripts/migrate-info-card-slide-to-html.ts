// scripts/migrate-info-card-slide-to-html.ts
// info-card-slide 정보 카드 슬라이드 컴포넌트 등록/업데이트 (Issue #274)
// 실행: npx tsx scripts/migrate-info-card-slide-to-html.ts

import 'dotenv/config';
import { getComponentById, updateComponent, createComponent } from '../src/db/repository/component.repository';
import { closePool } from '../src/db/connection';

const FONT_FAMILY = "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif";

// ── 데이터 모델 ──────────────────────────────────────────────────────────

interface CardButton {
    label: string;
    href?: string;
}

interface CardSlide {
    tag?: string;
    showMore?: boolean;
    moreHref?: string;
    title: string;
    copyable?: boolean;
    subtitle?: string;
    infoLines?: string[];
    buttons?: CardButton[];
}

type CardViewMode = 'mobile' | 'web' | 'responsive';

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

function getCardStyles(viewMode: CardViewMode) {
    if (viewMode === 'web') {
        return {
            tag:
                'display:inline-flex;align-items:center;max-width:100%;padding:6px 14px;border-radius:999px;background:linear-gradient(135deg,#E8F0FC 0%,#DCE8FF 100%);color:#0A4AA3;font-size:13px;font-weight:700;letter-spacing:-0.01em;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
            more:
                'color:#4B5563;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:128px;min-width:0;padding:8px 12px;border-radius:999px;background:#F3F6FB;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;font-weight:600;',
            header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:12px;',
            copyButton:
                'background:#F4F7FB;border:1px solid #E5EAF3;border-radius:10px;cursor:pointer;padding:6px;flex-shrink:0;display:flex;align-items:center;justify-content:center;',
            titleWrap: 'display:flex;align-items:flex-start;gap:8px;min-width:0;max-width:100%;',
            title:
                'display:block;font-size:26px;font-weight:800;color:#111827;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.28;letter-spacing:-0.03em;',
            subtitle:
                'display:block;max-width:100%;font-size:16px;color:#4B5563;overflow-wrap:anywhere;word-break:break-all;line-height:1.6;',
            infoLine:
                'display:block;max-width:100%;font-size:14px;color:#6B7280;text-align:left;overflow-wrap:anywhere;word-break:break-all;line-height:1.5;',
            buttonsWrap: 'display:flex;gap:8px;margin-top:6px;min-width:0;max-width:100%;flex-wrap:wrap;',
            button:
                'flex:1 1 140px;min-width:0;max-width:100%;text-align:center;padding:11px 12px;border-radius:10px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
            itemOuter: 'width:100%;max-width:100%;min-width:0;padding:0;box-sizing:border-box;',
            itemInner:
                'width:100%;max-width:100%;overflow:hidden;background:linear-gradient(180deg,#FFFFFF 0%,#FBFDFF 100%);border:1px solid #DCE4F2;border-radius:28px;padding:28px;display:flex;flex-direction:column;gap:14px;min-height:260px;box-sizing:border-box;box-shadow:0 20px 44px rgba(15,23,42,0.08);',
            track: 'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:20px;padding:12px 0 20px;scroll-padding:0 2%;',
        };
    }

    if (viewMode === 'responsive') {
        return {
            tag:
                'display:inline-flex;align-items:center;max-width:100%;padding:5px 12px;border-radius:999px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:700;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
            more:
                'color:#6B7280;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:108px;min-width:0;padding:6px 10px;border-radius:999px;background:#F5F7FA;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;font-weight:600;',
            header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:10px;',
            copyButton: 'background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;',
            titleWrap: 'display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;',
            title:
                'display:block;font-size:20px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.36;',
            subtitle:
                'display:block;max-width:100%;font-size:15px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.5;',
            infoLine:
                'display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:left;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
            buttonsWrap: 'display:flex;gap:8px;margin-top:6px;min-width:0;max-width:100%;flex-wrap:wrap;',
            button:
                'flex:1 1 140px;min-width:0;max-width:100%;text-align:center;padding:11px 12px;border-radius:10px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
            itemOuter: 'flex-shrink:0;width:100%;max-width:100%;padding:0 4px;box-sizing:border-box;',
            itemInner:
                'width:100%;max-width:100%;overflow:hidden;background:#fff;border:1px solid #E5E7EB;border-radius:20px;padding:22px;display:flex;flex-direction:column;gap:12px;min-height:220px;box-sizing:border-box;',
            track: 'display:flex;flex-direction:row;gap:14px;padding:10px 0 16px;',
        };
    }

    return {
        tag:
            'display:inline-block;max-width:100%;padding:4px 12px;border-radius:12px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:600;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
        more:
            'color:#9CA3AF;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:96px;min-width:0;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;',
        header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:8px;',
        copyButton: 'background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;',
        titleWrap: 'display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;',
        title:
            'display:block;font-size:18px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.4;',
        subtitle:
            'display:block;max-width:100%;font-size:14px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
        infoLine:
            'display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:right;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
        buttonsWrap: 'display:flex;gap:8px;margin-top:4px;min-width:0;max-width:100%;flex-wrap:wrap;',
        button:
            'flex:1 1 120px;min-width:0;max-width:100%;text-align:center;padding:10px;border-radius:8px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
        itemOuter: 'flex-shrink:0;width:100%;max-width:100%;padding:0 8px;box-sizing:border-box;',
        itemInner:
            'width:100%;max-width:100%;overflow:hidden;background:#fff;border:1px solid #E5E7EB;border-radius:16px;padding:20px;display:flex;flex-direction:column;gap:12px;min-height:180px;box-sizing:border-box;',
        track: 'display:flex;flex-direction:column;gap:12px;padding:8px 0;',
    };
}

// ── 카드 HTML 빌더 ───────────────────────────────────────────────────────

function buildCardHtml(card: CardSlide, idx: number): string {
    // 상단: 태그 + 더보기
    const tagHtml = card.tag
        ? `<span style="display:inline-block;max-width:100%;padding:4px 12px;border-radius:12px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:600;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;">${escapeHtml(card.tag)}</span>`
        : '';
    const moreHtml = card.showMore
        ? `<a href="${sanitizeHref(card.moreHref || '#')}" style="color:#9CA3AF;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:96px;min-width:0;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;">더보기</a>`
        : '';
    const headerHtml = (tagHtml || moreHtml)
        ? `<div style="display:flex;align-items:flex-start;justify-content:space-between;gap:8px;">${tagHtml}${moreHtml}</div>`
        : '';

    // 제목 + 복사 아이콘
    const copyBtnHtml = card.copyable
        ? `<button data-card-copy style="background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;">` +
          `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>` +
          `</button>`
        : '';
    const titleHtml = `<div style="display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;">` +
        `<span data-card-title style="display:block;font-size:18px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.4;">${escapeHtml(card.title)}</span>` +
        copyBtnHtml +
        `</div>`;

    // 부제목
    const subtitleHtml = card.subtitle
        ? `<span style="display:block;max-width:100%;font-size:14px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;">${escapeHtml(card.subtitle)}</span>`
        : '';

    // 보조 텍스트
    const infoHtml = (card.infoLines ?? [])
        .map((line) => `<span style="display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:right;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;">${escapeHtml(line)}</span>`)
        .join('');

    // 하단 버튼
    const buttonsHtml = (card.buttons ?? []).length > 0
        ? `<div style="display:flex;gap:8px;margin-top:4px;min-width:0;max-width:100%;flex-wrap:wrap;">` +
          (card.buttons ?? []).map((b) =>
              `<a href="${sanitizeHref(b.href || '#')}"` +
              ` style="flex:1;text-align:center;padding:10px;border-radius:8px;` +
              `background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;flex:1 1 120px;min-width:0;max-width:100%;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;">${escapeHtml(b.label)}</a>`,
          ).join('') +
          `</div>`
        : '';

    return (
        `<div data-card-item data-card-idx="${idx}"` +
        ` style="flex-shrink:0;width:100%;max-width:100%;padding:0 8px;box-sizing:border-box;">` +
            `<div style="background:#fff;border:1px solid #E5E7EB;border-radius:16px;` +
            `padding:20px;display:flex;flex-direction:column;gap:12px;min-height:180px;width:100%;max-width:100%;overflow:hidden;box-sizing:border-box;">` +
                headerHtml +
                titleHtml +
                subtitleHtml +
                infoHtml +
                buttonsHtml +
            `</div>` +
        `</div>`
    );
}

function buildCardHtmlByMode(card: CardSlide, idx: number, viewMode: CardViewMode): string {
    const styles = getCardStyles(viewMode);
    const tagHtml = card.tag ? `<span style="${styles.tag}">${escapeHtml(card.tag)}</span>` : '';
    const moreHtml = card.showMore
        ? `<a href="${sanitizeHref(card.moreHref || '#')}" data-card-more style="${styles.more}">더보기</a>`
        : '';
    const headerHtml = tagHtml || moreHtml ? `<div style="${styles.header}">${tagHtml}${moreHtml}</div>` : '';
    const copyBtnHtml = card.copyable
        ? `<button data-card-copy style="${styles.copyButton}">` +
          `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>` +
          `</button>`
        : '';
    const titleHtml = `<div style="${styles.titleWrap}">` +
        `<span data-card-title style="${styles.title}">${escapeHtml(card.title)}</span>` +
        copyBtnHtml +
        `</div>`;
    const subtitleHtml = card.subtitle ? `<span style="${styles.subtitle}">${escapeHtml(card.subtitle)}</span>` : '';
    const infoHtml = (card.infoLines ?? []).map((line) => `<span style="${styles.infoLine}">${escapeHtml(line)}</span>`).join('');
    const buttonsHtml = (card.buttons ?? []).length > 0
        ? `<div style="${styles.buttonsWrap}">` +
          (card.buttons ?? []).map((b) =>
              `<a href="${sanitizeHref(b.href || '#')}" style="${styles.button}">${escapeHtml(b.label)}</a>`,
          ).join('') +
          `</div>`
        : '';

    return (
        `<div data-card-item data-card-idx="${idx}"` +
        ` style="${styles.itemOuter}">` +
            `<div style="${styles.itemInner}">` +
                headerHtml +
                titleHtml +
                subtitleHtml +
                infoHtml +
                buttonsHtml +
            `</div>` +
        `</div>`
    );
}

// ── 인라인 스크립트 — scroll-snap 변환 + 복사 기능 ──────────────────────

const SLIDE_SCRIPT =
    `<script>` +
    `(function(){` +
    `var root=document.currentScript&&document.currentScript.closest('[data-spw-block]');` +
    `if(!root||root.getAttribute('data-card-slide-inited')==='1')return;` +
    `if(root.closest('.is-builder'))return;` +
    `root.setAttribute('data-card-slide-inited','1');` +
    `var mode=root.getAttribute('data-card-view-mode')||(/-web$/.test(root.getAttribute('data-component-id')||'')?'web':(/-responsive$/.test(root.getAttribute('data-component-id')||'')?'responsive':'mobile'));` +

    // scroll-snap 변환 — 세로 나열 → 가로 스와이프 + 가운데 정렬
    `var track=root.querySelector('[data-card-track]');` +
    `if(track){` +
    `if(mode==='web'){` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:20px;padding:12px 0 20px;scroll-padding:0 2%;';` +
    `}else if(mode==='responsive'){` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:14px;padding:10px 0 16px;scroll-padding:0 2%;';` +
    `}else{` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:0;padding:8px 0 12px;scroll-padding:0 4%;';` +
    `}` +
    // 카드 높이 균등화 — 가장 높은 카드 기준
    `var maxH=0;` +
    `track.querySelectorAll('[data-card-item] > div').forEach(function(inner){` +
    `inner.style.minHeight='0';` +
    `if(inner.scrollHeight>maxH)maxH=inner.scrollHeight;` +
    `});` +
    `track.querySelectorAll('[data-card-item] > div').forEach(function(inner){` +
    `inner.style.minHeight=maxH+'px';` +
    `});` +
    // 카드 너비 — 1개이면 100%(화면 꽉 채움), 2개 이상이면 92%(다음 카드 살짝 노출)
    `var cardItems=track.querySelectorAll('[data-card-item]');` +
    `var isSingle=cardItems.length===1;` +
    `cardItems.forEach(function(card){` +
    `if(mode==='web'){card.style.flex='0 0 min(480px,46vw)';card.style.width='min(480px,46vw)';card.style.maxWidth='';card.style.minWidth='0';card.style.scrollSnapAlign='start';}` +
    `else if(mode==='responsive'){card.style.flex='0 0 min(440px,78vw)';card.style.width='min(440px,78vw)';card.style.scrollSnapAlign='start';}` +
    `else{var w=isSingle?'100%':'92%';card.style.flex='0 0 '+w;card.style.width=w;card.style.scrollSnapAlign='center';}` +
    `});` +
    // 하단 버튼 텍스트 넘침 시 글자 크기 자동 축소
    `track.querySelectorAll('[data-card-item] a').forEach(function(btn){` +
    `if(!btn.style.borderRadius)return;` +
    `btn.style.minWidth='0';btn.style.maxWidth='100%';btn.style.whiteSpace='normal';btn.style.overflowWrap='anywhere';btn.style.wordBreak='break-all';btn.style.boxSizing='border-box';` +
    `});` +
    `if(!track.getAttribute('data-ics-id')){` +
    `var styleId='ics-hide-'+Math.random().toString(36).slice(2,8);` +
    `track.setAttribute('data-ics-id',styleId);` +
    `var styleEl=document.createElement('style');` +
    `styleEl.textContent='[data-ics-id=\"'+styleId+'\"]::-webkit-scrollbar{display:none}';` +
    `root.appendChild(styleEl);` +
    `}` +
    `}` +

    // 복사 버튼
    `root.querySelectorAll('[data-card-copy]').forEach(function(btn){` +
    `btn.addEventListener('click',function(e){` +
    `e.preventDefault();` +
    `var card=btn.closest('[data-card-item]');` +
    `var titleEl=card&&card.querySelector('[data-card-title]');` +
    `if(titleEl&&navigator.clipboard){` +
    `navigator.clipboard.writeText(titleEl.textContent||'');` +
    `var svg=btn.querySelector('svg');` +
    `if(svg){svg.setAttribute('stroke','#059669');setTimeout(function(){svg.setAttribute('stroke','#9CA3AF');},1500);}` +
    `}` +
    `});` +
    `});` +
    `})();` +
    `</script>`;

// ── 전체 HTML 조립 ───────────────────────────────────────────────────────

function buildInfoCardSlideHtml(slides: CardSlide[], componentId: string, extraStyle: string): string {
    const viewMode = componentId.endsWith('-web') ? 'web' : componentId.endsWith('-responsive') ? 'responsive' : 'mobile';
    const slidesJson = JSON.stringify(slides).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
    const cardsHtml = slides.map((card, i) => buildCardHtmlByMode(card, i, viewMode)).join('');

    return (
        `<div data-component-id="${componentId}" data-spw-block` +
        ` data-card-slides="${slidesJson}"` +
        ` data-card-view-mode="${viewMode}"` +
        ` style="font-family:${FONT_FAMILY};background:#ffffff;${extraStyle}">` +
            `<div data-card-track style="${getCardStyles(viewMode).track}">` +
                cardsHtml +
            `</div>` +
            SLIDE_SCRIPT +
        `</div>`
    );
}

// ── 기본 프리셋 데이터 ───────────────────────────────────────────────────

const DEFAULT_SLIDES: CardSlide[] = [
    {
        tag: '이벤트',
        showMore: true,
        moreHref: '#',
        title: 'IBK 신용카드 혜택',
        copyable: true,
        subtitle: '연회비 무료 이벤트 진행 중',
        infoLines: ['유효기간: 2024.12.31'],
        buttons: [{ label: '자세히 보기' }, { label: '신청하기' }],
    },
    {
        tag: '안내',
        title: '체크카드 캐시백 안내',
        copyable: false,
        subtitle: '월 30만원 이상 이용 시 1% 캐시백',
        infoLines: ['적용일: 2024.01.01', '한도: 월 5,000원'],
        buttons: [{ label: '상세 조건 보기' }],
    },
    {
        tag: '혜택',
        showMore: true,
        title: '적금 우대금리 제공',
        copyable: true,
        subtitle: '최대 연 4.5% 금리 혜택',
        buttons: [{ label: '가입하기' }, { label: '금리 비교' }, { label: '상담 신청' }],
    },
];

// ── 3 variant ────────────────────────────────────────────────────────────

const VIEW_MODES = ['mobile', 'web', 'responsive'] as const;

const EXTRA_STYLES: Record<string, string> = {
    mobile: '',
    web: 'width:100%;padding:8px 24px 16px;box-sizing:border-box;',
    responsive: 'width:100%;box-sizing:border-box;',
};

const VARIANTS = VIEW_MODES.map((viewMode) => ({
    id: `info-card-slide-${viewMode}`,
    html: buildInfoCardSlideHtml(DEFAULT_SLIDES, `info-card-slide-${viewMode}`, EXTRA_STYLES[viewMode]),
    viewMode,
}));

const COMPONENT_LABEL = '정보 카드 슬라이드';
const COMPONENT_DESC = '좌우 스와이프 카드형 정보 UI';

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
                    id:          'info-card-slide',
                    label:       COMPONENT_LABEL,
                    description: COMPONENT_DESC,
                    preview:     '/assets/minimalist-blocks/preview/info-card-slide.svg',
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
                componentThumbnail: '/assets/minimalist-blocks/preview/info-card-slide.svg',
                data: {
                    id:          'info-card-slide',
                    label:       COMPONENT_LABEL,
                    description: COMPONENT_DESC,
                    preview:     '/assets/minimalist-blocks/preview/info-card-slide.svg',
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
