// src/components/edit/EventBannerEditor.tsx
// 이벤트 배너 컴포넌트 편집 패널 (Issue #293)
// migrate-event-banner-to-html.ts 와 빌더 함수 동기화 필수
'use client';

import { useState, useCallback, useEffect } from 'react';

import { openCmsFilesPicker } from '@/lib/cms-file-picker';

// ── 데이터 모델 ──────────────────────────────────────────────────────────

interface BannerSlide {
    imageUrl: string;
    linkHref?: string;
    altText?: string;
    overlayTitle?: string;
    overlayDesc?: string;
}

export interface Props {
    blockEl: HTMLElement;
    onClose: () => void;
}

// ── 보안 처리 (마이그레이션 스크립트와 동기화) ──────────────────────────

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

// ── 슬라이드 HTML 빌더 (마이그레이션 스크립트와 동기화) ─────────────────

function buildSlideHtml(slide: BannerSlide, idx: number): string {
    const imgHtml = slide.imageUrl
        ? `<img src="${escapeHtml(slide.imageUrl)}" alt="${escapeHtml(slide.altText ?? '')}" style="width:100%;aspect-ratio:16/9;object-fit:contain;display:block;" />`
        : `<div style="width:100%;aspect-ratio:16/9;background:#E5E7EB;display:flex;align-items:center;justify-content:center;"><span style="color:#9CA3AF;font-size:14px;">이미지를 추가하세요</span></div>`;

    const overlayHtml =
        slide.overlayTitle || slide.overlayDesc
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

// ── 인라인 스크립트 (마이그레이션 스크립트와 동기화) ─────────────────────

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
    `styleEl.textContent='[data-eb-id=\"'+styleId+'\"]::-webkit-scrollbar{display:none}';` +
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

// ── 전체 HTML 조립 (마이그레이션 스크립트와 동기화) ─────────────────────

const FONT_FAMILY = "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif";

const EXTRA_STYLES: Record<string, string> = {
    mobile: '',
    web: 'width:100%;box-sizing:border-box;',
    responsive: 'width:100%;box-sizing:border-box;',
};

function buildEventBannerHtml(
    slides: BannerSlide[],
    componentId: string,
    interval: number,
    extraStyle: string,
): string {
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

// ── blockEl 파싱 ──────────────────────────────────────────────────────────

function parseBannerData(blockEl: HTMLElement): { slides: BannerSlide[]; interval: number } {
    let slides: BannerSlide[] = [];

    // 1순위: data-banner-slides 속성 JSON 파싱
    const raw = blockEl.getAttribute('data-banner-slides');
    if (raw) {
        try {
            slides = JSON.parse(raw) as BannerSlide[];
        } catch {
            // 파싱 실패 시 DOM fallback으로 진행
        }
    }

    // 2순위: DOM에서 직접 추출
    if (!slides.length) {
        blockEl.querySelectorAll<HTMLElement>('[data-banner-item]').forEach((item) => {
            const a = item.querySelector<HTMLAnchorElement>('a');
            const img = item.querySelector<HTMLImageElement>('img');
            const titleEl = item.querySelector<HTMLElement>('[data-banner-overlay-title]');
            const descEl = item.querySelector<HTMLElement>('[data-banner-overlay-desc]');
            slides.push({
                imageUrl: img?.getAttribute('src') ?? '',
                linkHref: a?.getAttribute('href') ?? '#',
                altText: img?.getAttribute('alt') ?? '',
                overlayTitle: titleEl?.textContent?.trim() || undefined,
                overlayDesc: descEl?.textContent?.trim() || undefined,
            });
        });
    }

    // 폴백: 기본 슬라이드 1장
    if (!slides.length) {
        slides = [{ imageUrl: '', linkHref: '#' }];
    }

    const interval = parseInt(blockEl.getAttribute('data-banner-interval') ?? '3000', 10);
    return { slides, interval };
}

// ── DOM 업데이트 ──────────────────────────────────────────────────────────

function applyToBlock(blockEl: HTMLElement, slides: BannerSlide[], interval: number): void {
    const componentId = blockEl.getAttribute('data-component-id') ?? 'event-banner-mobile';
    const viewMode = componentId.endsWith('-web')
        ? 'web'
        : componentId.endsWith('-responsive')
          ? 'responsive'
          : 'mobile';

    const newHtml = buildEventBannerHtml(slides, componentId, interval, EXTRA_STYLES[viewMode] ?? '');
    const tmp = document.createElement('div');
    tmp.innerHTML = newHtml;
    const newEl = tmp.firstElementChild as HTMLElement | null;
    if (newEl) blockEl.replaceWith(newEl);
}

// ── 패널 스타일 상수 ──────────────────────────────────────────────────────

const S = {
    overlay: {
        position: 'fixed' as const,
        inset: 0,
        zIndex: 99998,
        background: 'rgba(0,0,0,0.35)',
    },
    panel: {
        position: 'fixed' as const,
        left: '50%',
        top: '50%',
        transform: 'translate(-50%,-50%)',
        width: 460,
        maxWidth: 'calc(100vw - 32px)',
        zIndex: 99999,
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: 12,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
        display: 'flex',
        flexDirection: 'column' as const,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 14px',
        borderBottom: '1px solid #f3f4f6',
        background: '#fafafa',
        borderRadius: '12px 12px 0 0',
        flexShrink: 0,
    },
    body: {
        overflowY: 'auto' as const,
        maxHeight: 420,
        display: 'flex',
        flexDirection: 'column' as const,
        gap: 10,
        padding: '14px 14px 10px',
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '10px 14px 14px',
        borderTop: '1px solid #f3f4f6',
        flexShrink: 0,
    },
    label: {
        display: 'block',
        fontSize: 12,
        color: '#6B7280',
        marginBottom: 3,
    },
    input: {
        width: '100%',
        border: '1px solid #e5e7eb',
        borderRadius: 6,
        padding: '5px 8px',
        fontSize: 13,
        outline: 'none',
        boxSizing: 'border-box' as const,
        fontFamily: 'inherit',
    },
    slideCard: {
        background: '#f9fafb',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: '10px 12px',
        display: 'flex',
        flexDirection: 'column' as const,
        gap: 6,
    },
    slideHeader: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 4,
    },
    iconBtn: {
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        padding: '2px 5px',
        fontSize: 13,
        color: '#6B7280',
        borderRadius: 4,
    },
    deleteBtn: {
        background: 'none',
        border: '1px solid #fca5a5',
        cursor: 'pointer',
        padding: '2px 7px',
        fontSize: 12,
        color: '#ef4444',
        borderRadius: 4,
    },
    addBtn: {
        width: '100%',
        border: '1.5px dashed #93c5fd',
        borderRadius: 8,
        padding: '8px 0',
        fontSize: 13,
        color: '#2563eb',
        background: '#eff6ff',
        cursor: 'pointer',
    },
    cancelBtn: {
        padding: '6px 16px',
        border: '1px solid #e5e7eb',
        borderRadius: 7,
        background: '#fff',
        fontSize: 13,
        cursor: 'pointer',
        color: '#374151',
    },
    applyBtn: {
        padding: '6px 18px',
        border: 'none',
        borderRadius: 7,
        background: '#0046A4',
        fontSize: 13,
        cursor: 'pointer',
        color: '#fff',
        fontWeight: 600,
    },
};

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────

export default function EventBannerEditor({ blockEl, onClose }: Props) {
    // 1. state
    const [slides, setSlides] = useState<BannerSlide[]>([]);
    const [autoInterval, setAutoInterval] = useState(3000);
    // 입력 도중 빈 문자열·중간값 허용을 위해 string으로 별도 관리
    const [intervalStr, setIntervalStr] = useState('3000');

    // 2. effect — 초기값 로드
    useEffect(() => {
        const { slides: s, interval } = parseBannerData(blockEl);
        setSlides(s);
        setAutoInterval(interval);
        setIntervalStr(String(interval));
    }, [blockEl]);

    // 슬라이드 부분 업데이트
    const updateSlide = useCallback((idx: number, patch: Partial<BannerSlide>) => {
        setSlides((prev) => prev.map((s, i) => (i === idx ? { ...s, ...patch } : s)));
    }, []);

    const handleAdd = useCallback(() => {
        setSlides((prev) => [...prev, { imageUrl: '', linkHref: '#' }]);
    }, []);

    const handleDelete = useCallback((idx: number) => {
        setSlides((prev) => (prev.length <= 1 ? prev : prev.filter((_, i) => i !== idx)));
    }, []);

    const handleMoveUp = useCallback((idx: number) => {
        if (idx === 0) return;
        setSlides((prev) => {
            const a = [...prev];
            [a[idx - 1], a[idx]] = [a[idx], a[idx - 1]];
            return a;
        });
    }, []);

    const handleMoveDown = useCallback((idx: number) => {
        setSlides((prev) => {
            if (idx >= prev.length - 1) return prev;
            const a = [...prev];
            [a[idx], a[idx + 1]] = [a[idx + 1], a[idx]];
            return a;
        });
    }, []);

    const handleApply = useCallback(() => {
        applyToBlock(blockEl, slides, autoInterval);
        onClose();
    }, [blockEl, slides, autoInterval, onClose]);

    // /cms/files 팝업에서 승인된 이미지 선택 → 해당 슬라이드 imageUrl 교체
    const handlePickImage = useCallback(
        (idx: number) => {
            try {
                openCmsFilesPicker((url) => {
                    updateSlide(idx, { imageUrl: url });
                });
            } catch (err: unknown) {
                console.error('cms/files 이미지 선택 실패:', err);
            }
        },
        [updateSlide],
    );

    return (
        <>
            {/* 배경 오버레이 */}
            <div style={S.overlay} onClick={onClose} />

            {/* 편집 패널 */}
            <div style={S.panel}>
                {/* 헤더 */}
                <div style={S.header}>
                    <span style={{ fontSize: 14, fontWeight: 600, color: '#111827' }}>이벤트 배너 편집</span>
                    <button style={{ ...S.iconBtn, fontSize: 16, color: '#9CA3AF' }} onClick={onClose}>
                        ×
                    </button>
                </div>

                {/* 본문 */}
                <div style={S.body}>
                    {/* 자동재생 간격 */}
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 8,
                            padding: '6px 10px',
                            background: '#f9fafb',
                            border: '1px solid #e5e7eb',
                            borderRadius: 8,
                        }}
                    >
                        <label style={{ ...S.label, marginBottom: 0, whiteSpace: 'nowrap' }}>자동재생 간격 (ms)</label>
                        <input
                            type="number"
                            min={500}
                            step={100}
                            value={intervalStr}
                            onChange={(e) => {
                                setIntervalStr(e.target.value);
                                const n = parseInt(e.target.value);
                                if (!isNaN(n) && n >= 100) setAutoInterval(n);
                            }}
                            onBlur={() => {
                                const clamped = Math.max(500, parseInt(intervalStr) || 3000);
                                setAutoInterval(clamped);
                                setIntervalStr(String(clamped));
                            }}
                            style={{ ...S.input, width: 90 }}
                        />
                    </div>

                    {/* 슬라이드 목록 */}
                    {slides.map((slide, idx) => (
                        <div key={idx} style={S.slideCard}>
                            {/* 슬라이드 헤더 */}
                            <div style={S.slideHeader}>
                                <span style={{ fontSize: 13, fontWeight: 600, color: '#374151' }}>
                                    슬라이드 {idx + 1}
                                </span>
                                <div style={{ display: 'flex', gap: 4 }}>
                                    <button style={S.iconBtn} onClick={() => handleMoveUp(idx)} title="위로">
                                        ▲
                                    </button>
                                    <button style={S.iconBtn} onClick={() => handleMoveDown(idx)} title="아래로">
                                        ▼
                                    </button>
                                    <button style={S.deleteBtn} onClick={() => handleDelete(idx)}>
                                        삭제
                                    </button>
                                </div>
                            </div>

                            {/* 이미지 — cms/files에서 선택 */}
                            <div>
                                <label style={S.label}>이미지</label>
                                <div style={{ display: 'flex', gap: 6 }}>
                                    <input
                                        value={slide.imageUrl}
                                        onChange={(e) => updateSlide(idx, { imageUrl: e.target.value })}
                                        placeholder="cms/files에서 이미지를 선택하세요"
                                        style={{ ...S.input, flex: 1 }}
                                        readOnly
                                    />
                                    <button
                                        type="button"
                                        onClick={() => handlePickImage(idx)}
                                        style={{
                                            padding: '5px 10px',
                                            border: '1px solid #C7D8F4',
                                            borderRadius: 6,
                                            background: '#F0F4FF',
                                            color: '#0046A4',
                                            fontSize: 12,
                                            fontWeight: 600,
                                            cursor: 'pointer',
                                            whiteSpace: 'nowrap',
                                        }}
                                    >
                                        이미지 선택
                                    </button>
                                </div>
                                {slide.imageUrl ? (
                                    <div style={{ marginTop: 6 }}>
                                        {/* eslint-disable-next-line @next/next/no-img-element */}
                                        <img
                                            src={slide.imageUrl}
                                            alt=""
                                            style={{
                                                width: '100%',
                                                aspectRatio: '16/9',
                                                objectFit: 'cover',
                                                borderRadius: 6,
                                                border: '1px solid #e5e7eb',
                                                background: '#f9fafb',
                                            }}
                                        />
                                    </div>
                                ) : null}
                            </div>

                            {/* 링크 */}
                            <div>
                                <label style={S.label}>링크</label>
                                <input
                                    value={slide.linkHref ?? '#'}
                                    onChange={(e) => updateSlide(idx, { linkHref: e.target.value })}
                                    placeholder="#"
                                    style={S.input}
                                />
                            </div>

                            {/* 제목 (오버레이) */}
                            <div>
                                <label style={S.label}>제목 (오버레이, 선택)</label>
                                <input
                                    value={slide.overlayTitle ?? ''}
                                    onChange={(e) => updateSlide(idx, { overlayTitle: e.target.value || undefined })}
                                    placeholder="배너 제목"
                                    style={S.input}
                                />
                            </div>

                            {/* 설명 (오버레이) */}
                            <div>
                                <label style={S.label}>설명 (오버레이, 선택)</label>
                                <input
                                    value={slide.overlayDesc ?? ''}
                                    onChange={(e) => updateSlide(idx, { overlayDesc: e.target.value || undefined })}
                                    placeholder="배너 설명"
                                    style={S.input}
                                />
                            </div>
                        </div>
                    ))}

                    {/* 슬라이드 추가 버튼 */}
                    <button style={S.addBtn} onClick={handleAdd}>
                        + 슬라이드 추가
                    </button>
                </div>

                {/* 푸터 */}
                <div style={S.footer}>
                    <span style={{ fontSize: 13, color: '#9CA3AF' }}>총 {slides.length}장</span>
                    <div style={{ display: 'flex', gap: 8 }}>
                        <button style={S.cancelBtn} onClick={onClose}>
                            취소
                        </button>
                        <button style={S.applyBtn} onClick={handleApply}>
                            적용
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}
