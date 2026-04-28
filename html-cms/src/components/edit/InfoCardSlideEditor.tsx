// src/components/edit/InfoCardSlideEditor.tsx
// info-card-slide 정보 카드 슬라이드 편집 모달 (Issue #274)
// 카드 추가·삭제·순서변경, 슬롯별 편집 (태그/제목/복사/부제목/보조텍스트/버튼)

'use client';

import { useEffect, useRef, useState } from 'react';

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
    widthPx?: number;
    heightPx?: number;
    copyable?: boolean;
    subtitle?: string;
    infoLines?: string[];
    buttons?: CardButton[];
}

interface Props {
    blockEl: HTMLElement;
    onClose: () => void;
}

type CardViewMode = 'mobile' | 'web' | 'responsive';

const FONT_FAMILY = "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif";

function getViewModeFromComponentId(componentId?: string | null): CardViewMode {
    if (componentId?.endsWith('-web')) return 'web';
    if (componentId?.endsWith('-responsive')) return 'responsive';
    return 'mobile';
}

function getCardStyles(viewMode: CardViewMode) {
    if (viewMode === 'web') {
        return {
            tag: 'display:inline-flex;align-items:center;max-width:100%;padding:6px 14px;border-radius:999px;background:linear-gradient(135deg,#E8F0FC 0%,#DCE8FF 100%);color:#0A4AA3;font-size:13px;font-weight:700;letter-spacing:-0.01em;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
            more: 'color:#4B5563;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:128px;min-width:0;padding:8px 12px;border-radius:999px;background:#F3F6FB;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;font-weight:600;',
            header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:12px;',
            copyButton:
                'background:#F4F7FB;border:1px solid #E5EAF3;border-radius:10px;cursor:pointer;padding:6px;flex-shrink:0;display:flex;align-items:center;justify-content:center;',
            titleWrap: 'display:flex;align-items:flex-start;gap:8px;min-width:0;max-width:100%;',
            title: 'display:block;font-size:26px;font-weight:800;color:#111827;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.28;letter-spacing:-0.03em;',
            subtitle:
                'display:block;max-width:100%;font-size:16px;color:#4B5563;overflow-wrap:anywhere;word-break:break-all;line-height:1.6;',
            infoLine:
                'display:block;max-width:100%;font-size:14px;color:#6B7280;text-align:left;overflow-wrap:anywhere;word-break:break-all;line-height:1.5;',
            buttonsWrap: 'display:flex;gap:8px;margin-top:6px;min-width:0;max-width:100%;flex-wrap:wrap;',
            button: 'flex:1 1 140px;min-width:0;max-width:100%;text-align:center;padding:11px 12px;border-radius:10px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
            itemOuter: 'width:100%;max-width:100%;min-width:0;padding:0;box-sizing:border-box;',
            itemInner:
                'width:100%;max-width:100%;overflow:hidden;background:linear-gradient(180deg,#FFFFFF 0%,#FBFDFF 100%);border:1px solid #DCE4F2;border-radius:28px;padding:28px;display:flex;flex-direction:column;gap:14px;min-height:260px;box-sizing:border-box;box-shadow:0 20px 44px rgba(15,23,42,0.08);',
            track: 'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:20px;padding:12px 0 20px;scroll-padding:0 2%;',
        };
    }

    if (viewMode === 'responsive') {
        return {
            tag: 'display:inline-flex;align-items:center;max-width:100%;padding:5px 12px;border-radius:999px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:700;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
            more: 'color:#6B7280;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:108px;min-width:0;padding:6px 10px;border-radius:999px;background:#F5F7FA;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;font-weight:600;',
            header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:10px;',
            copyButton:
                'background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;',
            titleWrap: 'display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;',
            title: 'display:block;font-size:20px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.36;',
            subtitle:
                'display:block;max-width:100%;font-size:15px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.5;',
            infoLine:
                'display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:left;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
            buttonsWrap: 'display:flex;gap:8px;margin-top:6px;min-width:0;max-width:100%;flex-wrap:wrap;',
            button: 'flex:1 1 140px;min-width:0;max-width:100%;text-align:center;padding:11px 12px;border-radius:10px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
            itemOuter: 'flex-shrink:0;width:100%;max-width:100%;padding:0 4px;box-sizing:border-box;',
            itemInner:
                'width:100%;max-width:100%;overflow:hidden;background:#fff;border:1px solid #E5E7EB;border-radius:20px;padding:22px;display:flex;flex-direction:column;gap:12px;min-height:220px;box-sizing:border-box;',
            track: 'display:flex;flex-direction:row;gap:14px;padding:10px 0 16px;',
        };
    }

    return {
        tag: 'display:inline-block;max-width:100%;padding:4px 12px;border-radius:12px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:600;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;',
        more: 'color:#9CA3AF;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:96px;min-width:0;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;',
        header: 'display:flex;align-items:flex-start;justify-content:space-between;gap:8px;',
        copyButton:
            'background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;',
        titleWrap: 'display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;',
        title: 'display:block;font-size:18px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.4;',
        subtitle:
            'display:block;max-width:100%;font-size:14px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
        infoLine:
            'display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:right;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;',
        buttonsWrap: 'display:flex;gap:8px;margin-top:4px;min-width:0;max-width:100%;flex-wrap:wrap;',
        button: 'flex:1 1 120px;min-width:0;max-width:100%;text-align:center;padding:10px;border-radius:8px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;',
        itemOuter: 'flex-shrink:0;width:100%;max-width:100%;padding:0 8px;box-sizing:border-box;',
        itemInner:
            'width:100%;max-width:100%;overflow:hidden;background:#fff;border:1px solid #E5E7EB;border-radius:16px;padding:20px;display:flex;flex-direction:column;gap:12px;min-height:180px;box-sizing:border-box;',
        track: 'display:flex;flex-direction:column;gap:12px;padding:8px 0;',
    };
}

// ── href 보안 처리 ───────────────────────────────────────────────────────

function sanitizeHref(url: string): string {
    const trimmed = url.trim();
    if (/^(https?:\/\/|\/|#)/.test(trimmed)) {
        return trimmed.replace(/"/g, '&quot;');
    }
    return '#';
}

function escapeHtml(str: string): string {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── 카드 HTML 빌더 (마이그레이션 스크립트와 동기화) ──────────────────────

function buildCardHtml(card: CardSlide, idx: number): string {
    const tagHtml = card.tag
        ? `<span style="display:inline-block;max-width:100%;padding:4px 12px;border-radius:12px;background:#E8F0FC;color:#0046A4;font-size:12px;font-weight:600;overflow-wrap:anywhere;word-break:break-all;box-sizing:border-box;">${escapeHtml(card.tag)}</span>`
        : '';
    const moreHtml = card.showMore
        ? `<a href="${sanitizeHref(card.moreHref || '#')}" style="color:#9CA3AF;font-size:13px;text-decoration:none;line-height:1.4;display:inline-flex;align-items:center;justify-content:flex-end;max-width:96px;min-width:0;overflow-wrap:anywhere;word-break:break-word;text-align:right;flex-shrink:1;">더보기</a>`
        : '';
    const headerHtml =
        tagHtml || moreHtml
            ? `<div style="display:flex;align-items:flex-start;justify-content:space-between;gap:8px;">${tagHtml}${moreHtml}</div>`
            : '';

    const copyBtnHtml = card.copyable
        ? `<button data-card-copy style="background:none;border:none;cursor:pointer;padding:2px;flex-shrink:0;display:flex;align-items:center;">` +
          `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>` +
          `</button>`
        : '';
    const titleHtml =
        `<div style="display:flex;align-items:flex-start;gap:4px;min-width:0;max-width:100%;">` +
        `<span data-card-title style="display:block;font-size:18px;font-weight:700;color:#1A1A2E;flex:1;min-width:0;max-width:100%;overflow-wrap:anywhere;word-break:break-all;line-height:1.4;">${escapeHtml(card.title)}</span>` +
        copyBtnHtml +
        `</div>`;

    const subtitleHtml = card.subtitle
        ? `<span style="display:block;max-width:100%;font-size:14px;color:#6B7280;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;">${escapeHtml(card.subtitle)}</span>`
        : '';

    const infoHtml = (card.infoLines ?? [])
        .map(
            (line) =>
                `<span style="display:block;max-width:100%;font-size:13px;color:#6B7280;text-align:right;overflow-wrap:anywhere;word-break:break-all;line-height:1.45;">${escapeHtml(line)}</span>`,
        )
        .join('');

    const buttonsHtml =
        (card.buttons ?? []).length > 0
            ? `<div style="display:flex;gap:8px;margin-top:4px;min-width:0;max-width:100%;flex-wrap:wrap;">` +
              (card.buttons ?? [])
                  .map(
                      (b) =>
                          `<a href="${sanitizeHref(b.href || '#')}"` +
                          ` style="flex:1 1 120px;min-width:0;max-width:100%;text-align:center;padding:10px;border-radius:8px;background:#F5F7FA;color:#1A1A2E;font-size:13px;font-weight:600;text-decoration:none;white-space:normal;overflow-wrap:anywhere;word-break:break-all;line-height:1.35;box-sizing:border-box;">${escapeHtml(b.label)}</a>`,
                  )
                  .join('') +
              `</div>`
            : '';

    return (
        `<div data-card-item data-card-idx="${idx}"` +
        ` style="flex-shrink:0;width:100%;max-width:100%;padding:0 8px;box-sizing:border-box;">` +
        `<div style="width:100%;max-width:100%;overflow:hidden;background:#fff;border:1px solid #E5E7EB;border-radius:16px;padding:20px;display:flex;flex-direction:column;gap:12px;min-height:180px;box-sizing:border-box;">` +
        headerHtml +
        titleHtml +
        subtitleHtml +
        infoHtml +
        buttonsHtml +
        `</div></div>`
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
    const titleHtml =
        `<div style="${styles.titleWrap}">` +
        `<span data-card-title style="${styles.title}">${escapeHtml(card.title)}</span>` +
        copyBtnHtml +
        `</div>`;
    const subtitleHtml = card.subtitle ? `<span style="${styles.subtitle}">${escapeHtml(card.subtitle)}</span>` : '';
    const infoHtml = (card.infoLines ?? [])
        .map((line) => `<span style="${styles.infoLine}">${escapeHtml(line)}</span>`)
        .join('');
    const buttonsHtml =
        (card.buttons ?? []).length > 0
            ? `<div style="${styles.buttonsWrap}">` +
              (card.buttons ?? [])
                  .map(
                      (b) =>
                          `<a href="${sanitizeHref(b.href || '#')}" style="${styles.button}">${escapeHtml(b.label)}</a>`,
                  )
                  .join('') +
              `</div>`
            : '';

    return (
        `<div data-card-item data-card-idx="${idx}"` +
        ` style="${getCardOuterStyle(styles.itemOuter, card, viewMode)}">` +
        `<div style="${getCardInnerStyle(styles.itemInner, card)}">` +
        headerHtml +
        titleHtml +
        subtitleHtml +
        infoHtml +
        buttonsHtml +
        `</div></div>`
    );
}

// ── 인라인 스크립트 (마이그레이션 스크립트와 동기화) ─────────────────────

const SLIDE_SCRIPT =
    `<script>` +
    `(function(){` +
    `var root=document.currentScript&&document.currentScript.closest('[data-spw-block]');` +
    `if(!root||root.getAttribute('data-card-slide-inited')==='1')return;` +
    `if(root.closest('.is-builder'))return;` +
    `root.setAttribute('data-card-slide-inited','1');` +
    `var mode=root.getAttribute('data-card-view-mode')||(/-web$/.test(root.getAttribute('data-component-id')||'')?'web':(/-responsive$/.test(root.getAttribute('data-component-id')||'')?'responsive':'mobile'));` +
    `var track=root.querySelector('[data-card-track]');` +
    `if(track){` +
    `if(mode==='web'){` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:20px;padding:12px 0 20px;scroll-padding:0 2%;';` +
    `}else if(mode==='responsive'){` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:14px;padding:10px 0 16px;scroll-padding:0 2%;';` +
    `}else{` +
    `track.style.cssText='display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;gap:0;padding:8px 0 12px;scroll-padding:0 4%;';` +
    `}` +
    `var maxH=0;` +
    `track.querySelectorAll('[data-card-item] > div').forEach(function(inner){` +
    `inner.style.minHeight='0';` +
    `if(inner.scrollHeight>maxH)maxH=inner.scrollHeight;` +
    `});` +
    `track.querySelectorAll('[data-card-item] > div').forEach(function(inner){` +
    `inner.style.minHeight=maxH+'px';` +
    `});` +
    `var cardItems=track.querySelectorAll('[data-card-item]');` +
    // 카드 1개일 때는 100%로 화면을 꽉 채움 — 2개 이상이면 92%로 다음 카드를 살짝 노출
    `var isSingle=cardItems.length===1;` +
    `cardItems.forEach(function(card){` +
    `if(mode==='web'){card.style.flex='0 0 min(480px,46vw)';card.style.width='min(480px,46vw)';card.style.maxWidth='';card.style.minWidth='0';card.style.scrollSnapAlign='start';}` +
    `else if(mode==='responsive'){card.style.flex='0 0 min(440px,78vw)';card.style.width='min(440px,78vw)';card.style.scrollSnapAlign='start';}` +
    `else{var w=isSingle?'100%':'92%';card.style.flex='0 0 '+w;card.style.width=w;card.style.scrollSnapAlign='center';}` +
    `});` +
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

// ── DOM 조작 함수 ────────────────────────────────────────────────────────

function applyToBlock(blockEl: HTMLElement, slides: CardSlide[]) {
    const viewMode = getViewModeFromComponentId(blockEl.getAttribute('data-component-id'));
    blockEl.setAttribute('data-card-slides', JSON.stringify(slides));
    blockEl.setAttribute('data-card-view-mode', viewMode);

    const track = blockEl.querySelector<HTMLElement>('[data-card-track]');
    if (track) {
        track.setAttribute('style', getCardStyles(viewMode).track);
        track.innerHTML = slides.map((card, i) => buildCardHtmlByMode(card, i, viewMode)).join('');
    }

    blockEl.querySelectorAll('script').forEach((el) => el.remove());
    blockEl.querySelectorAll('style').forEach((el) => el.remove());
    blockEl.removeAttribute('data-card-slide-inited');
    blockEl.insertAdjacentHTML('beforeend', SLIDE_SCRIPT);
}

function parseSlides(blockEl: HTMLElement): CardSlide[] {
    let rawSlides: CardSlide[] = [];
    const rawJson = blockEl.getAttribute('data-card-slides');
    if (rawJson) {
        try {
            rawSlides = JSON.parse(rawJson) as CardSlide[];
        } catch {
            rawSlides = [];
        }
    }
    // 1순위: 현재 캔버스 DOM에서 추출
    const items = blockEl.querySelectorAll('[data-card-item]');
    if (items.length > 0) {
        return Array.from(items).map((item) => {
            const idx = Number.parseInt(item.getAttribute('data-card-idx') || '0', 10);
            const fallback = rawSlides[idx] ?? { title: '제목' };
            const innerCard = item.querySelector<HTMLElement>(':scope > div');
            const tagEl = item.querySelector('span[style*="border-radius:12px"]');
            const tag = tagEl?.textContent?.trim() || fallback.tag;
            const moreEl = item.querySelector('a[style*="justify-content:flex-end"]');
            const showMore = moreEl ? true : !!fallback.showMore;
            const moreHref = moreEl?.getAttribute('href') || fallback.moreHref;
            const title = item.querySelector('[data-card-title]')?.textContent?.trim() ?? '제목';
            const widthMatch = item.getAttribute('style')?.match(/(?:^|;)width:min\(100%,(\d+)px\)/i);
            const innerStyle = innerCard?.getAttribute('style') || '';
            const innerWidthMatch = innerStyle.match(/(?:^|;)width:min\(100%,(\d+)px\)/i);
            const heightMatch = innerStyle.match(/(?:^|;)min-height:(\d+)px/i);
            const liveWidth = innerCard?.getBoundingClientRect().width || item.getBoundingClientRect().width || 0;
            const liveHeight = innerCard?.getBoundingClientRect().height || 0;
            const widthPx = Number.parseInt(widthMatch?.[1] || innerWidthMatch?.[1] || '', 10);
            const heightPx = Number.parseInt(heightMatch?.[1] || '', 10);
            const copyable = !!item.querySelector('[data-card-copy]') || !!fallback.copyable;
            const subtitleEl =
                item.querySelector('span[style*="font-size:14px"]') ??
                item.querySelector('span[style*="font-size:15px"]') ??
                item.querySelector('span[style*="font-size:16px"]');
            const subtitle = subtitleEl?.textContent?.trim() || fallback.subtitle;
            const infoEls = item.querySelectorAll('span[style*="text-align:right"]');
            const infoElsLeft = item.querySelectorAll('span[style*="text-align:left"]');
            const infoLines =
                (infoEls.length > 0 ? infoEls : infoElsLeft).length > 0
                    ? Array.from(infoEls.length > 0 ? infoEls : infoElsLeft)
                          .map((el) => el.textContent?.trim() ?? '')
                          .filter(Boolean)
                    : fallback.infoLines;
            const btnEls8 = item.querySelectorAll('a[style*="border-radius:8px"]');
            const btnEls10 = item.querySelectorAll('a[style*="border-radius:10px"]');
            const btnEls = btnEls8.length > 0 ? btnEls8 : btnEls10;
            const buttons =
                btnEls.length > 0
                    ? Array.from(btnEls).map((el, buttonIdx) => ({
                          label: el.textContent?.trim() ?? fallback.buttons?.[buttonIdx]?.label ?? '',
                          href: el.getAttribute('href') || fallback.buttons?.[buttonIdx]?.href,
                      }))
                    : fallback.buttons;
            const safeTitle = title || fallback.title || '제목';
            return {
                tag,
                showMore,
                moreHref,
                title: safeTitle,
                // liveWidth/liveHeight 자동 저장 제거 — 렌더링 너비를 고정값으로 박으면
                // SLIDE_SCRIPT가 너비를 100%로 바꿔도 inner div가 고정 px로 막히는 부작용 발생.
                // widthPx/heightPx는 사용자가 에디터 입력 필드에 명시적으로 입력한 값만 보존.
                widthPx: Number.isFinite(widthPx) ? widthPx : undefined,
                heightPx: Number.isFinite(heightPx) ? heightPx : undefined,
                copyable,
                subtitle,
                infoLines,
                buttons,
            };
        });
    }

    // 2순위: data-card-slides JSON
    const raw = blockEl.getAttribute('data-card-slides');
    if (raw) {
        try {
            return JSON.parse(raw) as CardSlide[];
        } catch {
            // 파싱 실패 시 기본값 사용
        }
    }

    return [{ title: '새 카드' }];
}

function resizeStringArray(
    values: string[] | undefined,
    nextLength: number,
    fallbackValue: string,
): string[] | undefined {
    const safeLength = Math.max(0, nextLength);
    if (safeLength === 0) return undefined;

    const source = values ? [...values] : [];
    while (source.length < safeLength) {
        source.push(fallbackValue);
    }

    return source.slice(0, safeLength);
}

function resizeButtons(values: CardButton[] | undefined, nextLength: number): CardButton[] | undefined {
    const safeLength = Math.max(0, Math.min(3, nextLength));
    if (safeLength === 0) return undefined;

    const source = values ? [...values] : [];
    while (source.length < safeLength) {
        source.push({ label: `버튼 ${source.length + 1}` });
    }

    return source.slice(0, safeLength);
}

function normalizePositivePx(value: number | undefined): number | undefined {
    if (!Number.isFinite(value) || !value) return undefined;
    return Math.max(1, Math.round(value));
}

function getCardOuterStyle(baseStyle: string, card: CardSlide, viewMode: CardViewMode): string {
    const widthPx = normalizePositivePx(card.widthPx);
    if (!widthPx) return baseStyle;

    if (viewMode === 'web') {
        return `${baseStyle}max-width:min(100%,${widthPx}px);`;
    }

    return `${baseStyle}width:min(100%,${widthPx}px);`;
}

function getCardInnerStyle(baseStyle: string, card: CardSlide): string {
    const widthPx = normalizePositivePx(card.widthPx);
    const heightPx = normalizePositivePx(card.heightPx);

    return baseStyle + (widthPx ? `width:min(100%,${widthPx}px);` : '') + (heightPx ? `min-height:${heightPx}px;` : '');
}

// ── 스타일 상수 ───────────────────────────────────────────────────────────

const S = {
    overlay: {
        position: 'fixed' as const,
        inset: 0,
        zIndex: 99998,
        background: 'transparent',
    },
    panel: {
        position: 'fixed' as const,
        left: '50%',
        top: '50%',
        transform: 'translate(-50%, -50%)',
        width: 500,
        maxHeight: '85vh',
        display: 'flex',
        flexDirection: 'column' as const,
        zIndex: 99999,
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: 12,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
        fontFamily: FONT_FAMILY,
        fontSize: 13,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 14px',
        borderBottom: '1px solid #f3f4f6',
        borderRadius: '12px 12px 0 0',
        background: '#fafafa',
        flexShrink: 0,
    },
    body: {
        overflowY: 'auto' as const,
        flex: 1,
        padding: '12px 14px',
        display: 'flex',
        flexDirection: 'column' as const,
        gap: 10,
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 6,
        padding: '10px 14px 14px',
        borderTop: '1px solid #f3f4f6',
        flexShrink: 0,
    },
    iconBtn: {
        width: 24,
        height: 24,
        border: '1px solid #e5e7eb',
        borderRadius: 5,
        background: '#fff',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        padding: 0,
    } as React.CSSProperties,
    deleteBtn: {
        width: 24,
        height: 24,
        border: '1px solid #fca5a5',
        borderRadius: 5,
        background: '#fff',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        padding: 0,
        color: '#ef4444',
    } as React.CSSProperties,
    input: {
        flex: 1,
        padding: '5px 8px',
        border: '1px solid #e5e7eb',
        borderRadius: 5,
        fontSize: 12,
        color: '#111827',
        background: '#fff',
        fontFamily: FONT_FAMILY,
        outline: 'none',
        minWidth: 0,
        boxSizing: 'border-box' as const,
    },
    label: { fontSize: 11, fontWeight: 600, color: '#6B7280', minWidth: 40, flexShrink: 0 } as React.CSSProperties,
    row: { display: 'flex', alignItems: 'center', gap: 6 } as React.CSSProperties,
    addBtn: {
        width: '100%',
        padding: '8px',
        border: '1.5px dashed #c7d8f4',
        borderRadius: 8,
        background: '#f0f4ff',
        color: '#0046A4',
        fontSize: 13,
        fontWeight: 600,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
    } as React.CSSProperties,
    cancelBtn: {
        padding: '6px 14px',
        border: '1px solid #e5e7eb',
        borderRadius: 6,
        background: '#fff',
        fontSize: 12,
        color: '#374151',
        cursor: 'pointer',
        fontWeight: 600,
    },
    applyBtn: {
        padding: '6px 16px',
        border: 'none',
        borderRadius: 6,
        background: '#0046A4',
        fontSize: 12,
        color: '#fff',
        cursor: 'pointer',
        fontWeight: 600,
    },
    check: { width: 14, height: 14, cursor: 'pointer', accentColor: '#0046A4' } as React.CSSProperties,
};

// ── 컴포넌트 ──────────────────────────────────────────────────────────────

export default function InfoCardSlideEditor({ blockEl, onClose }: Props) {
    const [slides, setSlides] = useState<CardSlide[]>(() => parseSlides(blockEl));
    const [panelPos, setPanelPos] = useState<{ left: number; top: number } | null>(null);
    const dragStateRef = useRef<{ offsetX: number; offsetY: number } | null>(null);

    useEffect(() => {
        setSlides(parseSlides(blockEl));
        setPanelPos(null);
        dragStateRef.current = null;
    }, [blockEl]);

    useEffect(() => {
        const handleMouseMove = (e: MouseEvent) => {
            const dragState = dragStateRef.current;
            if (!dragState) return;

            setPanelPos({
                left: Math.max(12, e.clientX - dragState.offsetX),
                top: Math.max(12, e.clientY - dragState.offsetY),
            });
        };

        const handleMouseUp = () => {
            dragStateRef.current = null;
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);

        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, []);

    const updateSlide = (idx: number, patch: Partial<CardSlide>) => {
        setSlides((prev) => prev.map((s, i) => (i === idx ? { ...s, ...patch } : s)));
    };

    const handleAdd = () => {
        setSlides((prev) => [...prev, { title: '새 카드' }]);
    };

    const handleDelete = (idx: number) => {
        if (slides.length <= 1) return;
        setSlides((prev) => prev.filter((_, i) => i !== idx));
    };

    const handleMoveUp = (idx: number) => {
        if (idx === 0) return;
        const next = [...slides];
        [next[idx - 1], next[idx]] = [next[idx], next[idx - 1]];
        setSlides(next);
    };

    const handleMoveDown = (idx: number) => {
        if (idx === slides.length - 1) return;
        const next = [...slides];
        [next[idx], next[idx + 1]] = [next[idx + 1], next[idx]];
        setSlides(next);
    };

    const handleApply = () => {
        applyToBlock(blockEl, slides);
        onClose();
    };

    const handleHeaderMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
        if (e.button !== 0) return;
        if ((e.target as HTMLElement).closest('button')) return;

        const panelEl = e.currentTarget.parentElement as HTMLDivElement | null;
        if (!panelEl) return;

        const rect = panelEl.getBoundingClientRect();
        setPanelPos({ left: rect.left, top: rect.top });
        dragStateRef.current = {
            offsetX: e.clientX - rect.left,
            offsetY: e.clientY - rect.top,
        };
    };

    return (
        <>
            <div onClick={onClose} style={S.overlay} />
            <div
                data-testid="info-card-slide-editor"
                onClick={(e) => e.stopPropagation()}
                style={{
                    ...S.panel,
                    left: panelPos ? panelPos.left : S.panel.left,
                    top: panelPos ? panelPos.top : S.panel.top,
                    transform: panelPos ? 'none' : S.panel.transform,
                }}
            >
                {/* 헤더 */}
                <div onMouseDown={handleHeaderMouseDown} style={{ ...S.header, cursor: 'move' }}>
                    <span style={{ fontWeight: 700, color: '#111827' }}>정보 카드 편집</span>
                    <button
                        onClick={onClose}
                        style={{
                            width: 24,
                            height: 24,
                            border: 'none',
                            background: 'none',
                            cursor: 'pointer',
                            color: '#6b7280',
                            fontSize: 18,
                            padding: 0,
                            lineHeight: 1,
                        }}
                    >
                        ×
                    </button>
                </div>

                {/* 본문 */}
                <div style={S.body}>
                    {slides.map((card, idx) => (
                        <div
                            key={idx}
                            style={{
                                border: '1px solid #e5e7eb',
                                borderRadius: 8,
                                background: '#fafafa',
                                overflow: 'visible',
                            }}
                        >
                            {/* 카드 헤더 */}
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 6,
                                    padding: '8px 10px',
                                    borderBottom: '1px solid #f3f4f6',
                                }}
                            >
                                <span style={{ fontSize: 11, fontWeight: 700, color: '#374151' }}>카드 {idx + 1}</span>
                                <div style={{ marginLeft: 'auto', display: 'flex', gap: 4 }}>
                                    <button
                                        type="button"
                                        title="위로"
                                        disabled={idx === 0}
                                        onClick={() => handleMoveUp(idx)}
                                        style={{ ...S.iconBtn, opacity: idx === 0 ? 0.35 : 1 }}
                                    >
                                        <svg
                                            viewBox="0 0 24 24"
                                            width="11"
                                            height="11"
                                            fill="none"
                                            stroke="#374151"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                        >
                                            <path d="m18 15-6-6-6 6" />
                                        </svg>
                                    </button>
                                    <button
                                        type="button"
                                        title="아래로"
                                        disabled={idx === slides.length - 1}
                                        onClick={() => handleMoveDown(idx)}
                                        style={{ ...S.iconBtn, opacity: idx === slides.length - 1 ? 0.35 : 1 }}
                                    >
                                        <svg
                                            viewBox="0 0 24 24"
                                            width="11"
                                            height="11"
                                            fill="none"
                                            stroke="#374151"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                        >
                                            <path d="m6 9 6 6 6-6" />
                                        </svg>
                                    </button>
                                    <button
                                        type="button"
                                        title="카드 삭제"
                                        disabled={slides.length <= 1}
                                        onClick={() => handleDelete(idx)}
                                        style={{ ...S.deleteBtn, opacity: slides.length <= 1 ? 0.35 : 1 }}
                                    >
                                        <svg
                                            viewBox="0 0 24 24"
                                            width="11"
                                            height="11"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="2.5"
                                            strokeLinecap="round"
                                        >
                                            <path d="M18 6 6 18M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                            </div>

                            {/* 슬롯 편집 */}
                            <div style={{ padding: '8px 10px', display: 'flex', flexDirection: 'column', gap: 6 }}>
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 8,
                                        padding: '6px 8px',
                                        border: '1px solid #dbe4f0',
                                        borderRadius: 8,
                                        background: '#f8fbff',
                                    }}
                                >
                                    <span
                                        style={{
                                            fontSize: 11,
                                            fontWeight: 700,
                                            color: '#4b6baf',
                                            minWidth: 32,
                                            flexShrink: 0,
                                        }}
                                    >
                                        구조
                                    </span>
                                    <label
                                        style={{
                                            fontSize: 11,
                                            color: '#6B7280',
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 4,
                                        }}
                                    >
                                        가로(px)
                                        <input
                                            data-testid={`card-width-${idx}`}
                                            type="number"
                                            min={0}
                                            value={card.widthPx ?? ''}
                                            onChange={(e) =>
                                                updateSlide(idx, {
                                                    widthPx: normalizePositivePx(
                                                        Number.parseInt(e.target.value || '0', 10),
                                                    ),
                                                })
                                            }
                                            style={{ ...S.input, width: 68, flex: '0 0 68px', padding: '4px 6px' }}
                                            placeholder="auto"
                                        />
                                    </label>
                                    <label
                                        style={{
                                            fontSize: 11,
                                            color: '#6B7280',
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 4,
                                        }}
                                    >
                                        세로(px)
                                        <input
                                            data-testid={`card-height-${idx}`}
                                            type="number"
                                            min={0}
                                            value={card.heightPx ?? ''}
                                            onChange={(e) =>
                                                updateSlide(idx, {
                                                    heightPx: normalizePositivePx(
                                                        Number.parseInt(e.target.value || '0', 10),
                                                    ),
                                                })
                                            }
                                            style={{ ...S.input, width: 68, flex: '0 0 68px', padding: '4px 6px' }}
                                            placeholder="auto"
                                        />
                                    </label>
                                </div>
                                {/* 태그 */}
                                <div style={S.row}>
                                    <span style={S.label}>태그</span>
                                    <input
                                        type="checkbox"
                                        checked={!!card.tag}
                                        onChange={(e) =>
                                            updateSlide(idx, { tag: e.target.checked ? '태그' : undefined })
                                        }
                                        style={S.check}
                                    />
                                    {card.tag !== undefined && (
                                        <input
                                            type="text"
                                            value={card.tag ?? ''}
                                            onChange={(e) => updateSlide(idx, { tag: e.target.value })}
                                            style={S.input}
                                            placeholder="태그 텍스트"
                                        />
                                    )}
                                </div>

                                {/* 더보기 */}
                                <div style={S.row}>
                                    <span style={S.label}>더보기</span>
                                    <input
                                        type="checkbox"
                                        checked={!!card.showMore}
                                        onChange={(e) => updateSlide(idx, { showMore: e.target.checked })}
                                        style={S.check}
                                    />
                                    {card.showMore && (
                                        <input
                                            type="text"
                                            value={card.moreHref ?? ''}
                                            onChange={(e) =>
                                                updateSlide(idx, { moreHref: e.target.value || undefined })
                                            }
                                            style={S.input}
                                            placeholder="https://..."
                                        />
                                    )}
                                </div>

                                {/* 제목 + 복사 */}
                                <div style={S.row}>
                                    <span style={S.label}>제목</span>
                                    <input
                                        type="text"
                                        value={card.title}
                                        onChange={(e) => updateSlide(idx, { title: e.target.value })}
                                        style={{ ...S.input, fontWeight: 600 }}
                                        placeholder="제목"
                                    />
                                    <label
                                        style={{
                                            fontSize: 10,
                                            color: '#6B7280',
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 2,
                                            flexShrink: 0,
                                        }}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={!!card.copyable}
                                            onChange={(e) => updateSlide(idx, { copyable: e.target.checked })}
                                            style={S.check}
                                        />
                                        복사
                                    </label>
                                </div>

                                {/* 부제목 */}
                                <div style={S.row}>
                                    <span style={S.label}>부제목</span>
                                    <input
                                        type="text"
                                        value={card.subtitle ?? ''}
                                        onChange={(e) => updateSlide(idx, { subtitle: e.target.value || undefined })}
                                        style={S.input}
                                        placeholder="부제목 (선택)"
                                    />
                                </div>

                                {/* 보조 텍스트 */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                                    <div style={S.row}>
                                        <span style={S.label}>보조</span>
                                        <label
                                            style={{
                                                fontSize: 10,
                                                color: '#6B7280',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 4,
                                            }}
                                        >
                                            행 수
                                            <input
                                                type="number"
                                                min={0}
                                                max={6}
                                                value={card.infoLines?.length ?? 0}
                                                onChange={(e) =>
                                                    updateSlide(idx, {
                                                        infoLines: resizeStringArray(
                                                            card.infoLines,
                                                            Number.parseInt(e.target.value || '0', 10),
                                                            '새 텍스트',
                                                        ),
                                                    })
                                                }
                                                style={{ ...S.input, width: 54, flex: '0 0 54px', padding: '4px 6px' }}
                                            />
                                        </label>
                                        <button
                                            type="button"
                                            onClick={() =>
                                                updateSlide(idx, {
                                                    infoLines: [...(card.infoLines ?? []), '새 텍스트'],
                                                })
                                            }
                                            style={{
                                                fontSize: 10,
                                                color: '#0046A4',
                                                background: 'none',
                                                border: 'none',
                                                cursor: 'pointer',
                                            }}
                                        >
                                            + 행 추가
                                        </button>
                                    </div>
                                    {(card.infoLines ?? []).map((line, li) => (
                                        <div key={li} style={{ ...S.row, paddingLeft: 46 }}>
                                            <input
                                                type="text"
                                                value={line}
                                                onChange={(e) => {
                                                    const newLines = [...(card.infoLines ?? [])];
                                                    newLines[li] = e.target.value;
                                                    updateSlide(idx, { infoLines: newLines });
                                                }}
                                                style={S.input}
                                                placeholder="보조 텍스트"
                                            />
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    updateSlide(idx, {
                                                        infoLines: (card.infoLines ?? []).filter((_, i) => i !== li),
                                                    })
                                                }
                                                style={{ ...S.deleteBtn, width: 20, height: 20 }}
                                            >
                                                <svg
                                                    viewBox="0 0 24 24"
                                                    width="9"
                                                    height="9"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    strokeWidth="2.5"
                                                    strokeLinecap="round"
                                                >
                                                    <path d="M18 6 6 18M6 6l12 12" />
                                                </svg>
                                            </button>
                                        </div>
                                    ))}
                                </div>

                                {/* 하단 버튼 (0~3개) */}
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                                    <div style={S.row}>
                                        <span style={S.label}>버튼</span>
                                        <label
                                            style={{
                                                fontSize: 10,
                                                color: '#6B7280',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 4,
                                            }}
                                        >
                                            열 수
                                            <input
                                                type="number"
                                                min={0}
                                                max={3}
                                                value={card.buttons?.length ?? 0}
                                                onChange={(e) =>
                                                    updateSlide(idx, {
                                                        buttons: resizeButtons(
                                                            card.buttons,
                                                            Number.parseInt(e.target.value || '0', 10),
                                                        ),
                                                    })
                                                }
                                                style={{ ...S.input, width: 54, flex: '0 0 54px', padding: '4px 6px' }}
                                            />
                                        </label>
                                        <button
                                            type="button"
                                            disabled={(card.buttons ?? []).length >= 3}
                                            onClick={() =>
                                                updateSlide(idx, {
                                                    buttons: [...(card.buttons ?? []), { label: '버튼' }],
                                                })
                                            }
                                            style={{
                                                fontSize: 10,
                                                color: '#0046A4',
                                                background: 'none',
                                                border: 'none',
                                                cursor: 'pointer',
                                                opacity: (card.buttons ?? []).length >= 3 ? 0.35 : 1,
                                            }}
                                        >
                                            + 버튼 추가 ({(card.buttons ?? []).length}/3)
                                        </button>
                                    </div>
                                    {(card.buttons ?? []).map((btn, bi) => (
                                        <div key={bi} style={{ ...S.row, paddingLeft: 46 }}>
                                            <input
                                                type="text"
                                                value={btn.label}
                                                onChange={(e) => {
                                                    const newBtns = [...(card.buttons ?? [])];
                                                    newBtns[bi] = { ...newBtns[bi], label: e.target.value };
                                                    updateSlide(idx, { buttons: newBtns });
                                                }}
                                                style={{ ...S.input, flex: '0 0 100px' }}
                                                placeholder="라벨"
                                            />
                                            <input
                                                type="text"
                                                value={btn.href ?? ''}
                                                onChange={(e) => {
                                                    const newBtns = [...(card.buttons ?? [])];
                                                    newBtns[bi] = { ...newBtns[bi], href: e.target.value || undefined };
                                                    updateSlide(idx, { buttons: newBtns });
                                                }}
                                                style={S.input}
                                                placeholder="https://..."
                                            />
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    updateSlide(idx, {
                                                        buttons: (card.buttons ?? []).filter((_, i) => i !== bi),
                                                    })
                                                }
                                                style={{ ...S.deleteBtn, width: 20, height: 20 }}
                                            >
                                                <svg
                                                    viewBox="0 0 24 24"
                                                    width="9"
                                                    height="9"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    strokeWidth="2.5"
                                                    strokeLinecap="round"
                                                >
                                                    <path d="M18 6 6 18M6 6l12 12" />
                                                </svg>
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    ))}

                    {/* 카드 추가 */}
                    <button type="button" onClick={handleAdd} style={S.addBtn}>
                        <svg
                            viewBox="0 0 24 24"
                            width="14"
                            height="14"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.5"
                            strokeLinecap="round"
                        >
                            <path d="M12 5v14M5 12h14" />
                        </svg>
                        카드 추가
                    </button>
                </div>

                {/* 푸터 */}
                <div style={S.footer}>
                    <span style={{ fontSize: 11, color: '#9ca3af' }}>총 {slides.length}장</span>
                    <div style={{ display: 'flex', gap: 6 }}>
                        <button onClick={onClose} style={S.cancelBtn}>
                            취소
                        </button>
                        <button data-testid="apply-info-card-slide-editor" onClick={handleApply} style={S.applyBtn}>
                            적용
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}
