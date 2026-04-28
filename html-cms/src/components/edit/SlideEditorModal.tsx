// src/components/edit/SlideEditorModal.tsx
// 캔버스에 올라간 promo-banner / product-gallery 블록의 슬라이드/카드를
// 폼 UI로 추가·삭제·편집하고 DOM을 직접 수정하는 모달.
// API 호출 없음 — Save 버튼이 builder.html()로 캔버스 전체를 읽어 저장.
'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';

import { openCmsFilesPicker } from '@/lib/cms-file-picker';
import { rgbToHex } from '@/lib/html-utils';

// ── 타입 ──────────────────────────────────────────────────────────────────

interface PromoBannerSlide {
    itemId: string;
    bgColor: string;
    bgImage?: string;
    badge: string;
    title: string;
    desc: string;
    ctaText: string;
    ctaHref: string;
}

interface ProductGalleryCard {
    type: 'savings' | 'deposit' | 'loan';
    badge: string;
    productName: string;
    rateValue: string;
    rateLabel: string;
    detail: string;
    ctaHref: string;
    accentColor?: string; // 커스텀 강조 색상 (미설정 시 type 기본값 사용)
    bgImage?: string; // 카드 배경 이미지 URL
}

// ── DOM 파싱 ──────────────────────────────────────────────────────────────

function parsePromoBannerSlides(root: HTMLElement): PromoBannerSlide[] {
    return Array.from(root.querySelectorAll('[data-pb-slide]')).map((wrapper, i) => {
        const inner = wrapper.querySelector<HTMLElement>('.pb-slide');
        // 실제 저장된 HTML은 배경색/배경이미지를 .pb-slide-bg 자식 요소에 둠.
        // .pb-slide-bg가 없는 구버전 HTML은 .pb-slide로 폴백한다.
        const bgEl = inner?.querySelector<HTMLElement>('.pb-slide-bg') ?? inner;
        const ctaEl = inner?.querySelector('.pb-slide-cta') as HTMLAnchorElement | null;
        const bgImageMatch = (bgEl?.style.backgroundImage ?? '').match(/url\(['"]?([^'"]+)['"]?\)/);
        const rawBgImage = bgImageMatch?.[1];
        const bgImage = rawBgImage ? rawBgImage.replace(/\\/g, '/').replace(/^(?!\/)/, '/') : undefined;
        // backgroundColor가 있으면 hex로 변환, 없으면 background shorthand에서 url() 제거 후 사용
        const rawBgColor = bgEl?.style.backgroundColor
            ? rgbToHex(bgEl.style.backgroundColor)
            : (bgEl?.style.background ?? '').replace(/url\(.*?\)\s*/g, '').trim();
        return {
            itemId: inner?.getAttribute('data-item-id') ?? `pb-${i + 1}`,
            bgColor: rawBgColor || '#0046A4', // 파싱 실패 시 기본값
            bgImage,
            badge: inner?.querySelector('.pb-badge')?.textContent ?? '',
            title: inner?.querySelector('.pb-slide-title')?.textContent ?? '',
            desc: inner?.querySelector('.pb-slide-desc')?.textContent ?? '',
            ctaText: (ctaEl?.textContent ?? '').replace(/\s*→\s*$/, '').trim(),
            ctaHref: ctaEl?.getAttribute('href') ?? '#',
        };
    });
}

function parseCard(inner: HTMLElement | null): ProductGalleryCard {
    const ctaEl = inner?.querySelector('[data-pg-field="cta"]') as HTMLAnchorElement | null;
    const rateEl = inner?.querySelector<HTMLElement>('[data-pg-field="rateValue"]');
    const rawColor = rateEl?.style.color ?? '';
    const accentColor = rawColor ? rgbToHex(rawColor) : undefined;
    const bgImageMatch = (inner?.style.backgroundImage ?? '').match(/url\(['"]?([^'"]+)['"]?\)/);
    const rawBgImage = bgImageMatch?.[1];
    // 백슬래시 → 슬래시, 절대 경로 보정 (Windows 환경 대응)
    const bgImage = rawBgImage
        ? (() => {
              const s = rawBgImage.replace(/\\/g, '/');
              return s.startsWith('/') ? s : '/' + s;
          })()
        : undefined;
    return {
        type: (inner?.getAttribute('data-type') ?? 'savings') as ProductGalleryCard['type'],
        badge: inner?.querySelector('[data-pg-field="badge"]')?.textContent ?? '',
        productName: inner?.querySelector('[data-pg-field="productName"]')?.textContent ?? '',
        rateValue: rateEl?.textContent ?? '',
        rateLabel: inner?.querySelector('[data-pg-field="rateLabel"]')?.textContent ?? '',
        detail: inner?.querySelector('[data-pg-field="detail"]')?.textContent ?? '',
        ctaHref: ctaEl?.getAttribute('href') ?? '#',
        accentColor,
        bgImage,
    };
}

function parseProductGalleryCards(root: HTMLElement, componentId: string): ProductGalleryCard[] {
    if (componentId === 'product-gallery-web') {
        // 웹: data-pg-slide 없음 — data-pg-grid 그리드 컨테이너 내부 [data-type] 카드 직접 파싱
        const gridDiv = root.querySelector<HTMLElement>('[data-pg-grid]');
        return Array.from(gridDiv?.querySelectorAll('[data-type]') ?? []).map((el) => parseCard(el as HTMLElement));
    }
    // 모바일/반응형: data-pg-slide 기준
    return Array.from(root.querySelectorAll('[data-pg-slide]')).map((wrapper) =>
        parseCard(wrapper.firstElementChild as HTMLElement | null),
    );
}

// ── HTML 재생성 ───────────────────────────────────────────────────────────

function buildSlideHtml(slide: PromoBannerSlide): string {
    // 배경색/배경이미지는 .pb-slide-bg에 분리하여 overflow:hidden 클리핑이 올바르게 동작하도록 함
    const bgStyle =
        `position:absolute;top:0;right:0;bottom:0;left:0;background:${slide.bgColor};` +
        (slide.bgImage
            ? `background-image:url("${slide.bgImage}");background-size:cover;background-position:center;`
            : '');
    return (
        `<div class="pb-slide" data-item-id="${slide.itemId}" style="position:relative;height:200px;overflow:hidden;border-radius:16px;">` +
        `<div class="pb-slide-bg" style="${bgStyle}"></div>` +
        `<div class="pb-slide-content" style="position:relative;z-index:1;padding:24px 20px;display:flex;flex-direction:column;gap:6px;height:100%;box-sizing:border-box;justify-content:center;">` +
        `<span class="pb-badge" style="display:inline-block;background:rgba(255,255,255,0.25);color:#fff;font-size:11px;font-weight:700;padding:3px 10px;border-radius:20px;letter-spacing:0.5px;width:fit-content;border:1px solid rgba(255,255,255,0.4);">${slide.badge}</span>` +
        `<h3 class="pb-slide-title" style="font-size:22px;font-weight:800;color:#fff;margin:0;line-height:1.2;letter-spacing:-0.5px;">${slide.title}</h3>` +
        `<p class="pb-slide-desc" style="font-size:13px;color:rgba(255,255,255,0.85);margin:0;line-height:1.4;">${slide.desc}</p>` +
        `<a class="pb-slide-cta" href="${slide.ctaHref}" style="display:inline-flex;align-items:center;background:rgba(255,255,255,0.2);color:#fff;text-decoration:none;font-size:13px;font-weight:600;padding:8px 16px;border-radius:20px;border:1px solid rgba(255,255,255,0.5);width:fit-content;margin-top:4px;min-height:36px;-webkit-tap-highlight-color:transparent;">${slide.ctaText} →</a>` +
        `</div>` +
        `</div>`
    );
}

const CARD_COLORS: Record<string, { accent: string; accentLight: string }> = {
    savings: { accent: '#0046A4', accentLight: '#E8F0FC' },
    deposit: { accent: '#0046A4', accentLight: '#E8F0FC' },
    loan: { accent: '#FF6600', accentLight: '#FFF3EC' },
};

function buildCardHtml(card: ProductGalleryCard, itemId: string): string {
    const base = CARD_COLORS[card.type];
    const accent = card.accentColor ?? base.accent;
    const accentLight = card.accentColor ? card.accentColor + '1A' : base.accentLight;
    const bgImageStyle = card.bgImage
        ? `background-image:url('${card.bgImage}');background-size:cover;background-position:center;`
        : '';
    return (
        `<div data-type="${card.type}" data-item-id="${itemId}" style="background:#fff;${bgImageStyle}border-radius:16px;padding:24px 20px;display:flex;flex-direction:column;gap:6px;box-shadow:0 4px 20px rgba(0,70,164,0.08);position:relative;overflow:hidden;">` +
        `<div style="position:absolute;top:0;right:0;width:120px;height:120px;background:linear-gradient(135deg,${accentLight} 0%,transparent 70%);border-radius:0 16px 0 100%;pointer-events:none;"></div>` +
        (card.bgImage
            ? `<div style="position:absolute;inset:0;background:rgba(255,255,255,0.82);border-radius:16px;pointer-events:none;"></div>`
            : '') +
        `<div data-pg-field="badge" style="display:inline-flex;align-items:center;background:${accentLight};color:${accent};font-size:11px;font-weight:700;padding:4px 10px;border-radius:20px;width:fit-content;letter-spacing:0.5px;position:relative;">${card.badge}</div>` +
        `<div data-pg-field="productName" style="font-size:20px;font-weight:700;color:#1A1A2E;line-height:1.3;margin-top:4px;position:relative;">${card.productName}</div>` +
        `<div data-pg-field="rateWrap" style="display:flex;align-items:baseline;gap:2px;margin-top:8px;position:relative;">` +
        `<span data-pg-field="rateValue" style="font-size:40px;font-weight:800;color:${accent};line-height:1;letter-spacing:-1px;">${card.rateValue}</span>` +
        `<span style="font-size:22px;font-weight:700;color:${accent};">%</span>` +
        `</div>` +
        `<div data-pg-field="rateLabel" style="font-size:12px;color:#6B7280;font-weight:500;position:relative;">${card.rateLabel}</div>` +
        `<div data-pg-field="detail" style="font-size:13px;color:#6B7280;padding:10px 0;border-top:1px solid #F3F4F6;margin-top:4px;position:relative;">${card.detail}</div>` +
        `<a data-pg-field="cta" href="${card.ctaHref}" style="display:flex;align-items:center;justify-content:center;background:${accent};color:#fff;text-decoration:none;font-size:15px;font-weight:600;padding:14px;border-radius:12px;margin-top:8px;min-height:48px;-webkit-tap-highlight-color:transparent;position:relative;">자세히 보기</a>` +
        `</div>`
    );
}

// ── DOM 반영 ──────────────────────────────────────────────────────────────

function applyPromoBannerSlides(root: HTMLElement, slides: PromoBannerSlide[]) {
    const track = root.querySelector('[data-pb-track]');
    if (!track) return;

    track.innerHTML = slides.map((s) => `<div data-pb-slide style="width:100%;">${buildSlideHtml(s)}</div>`).join('');

    // 하단 카운터 "/ N" 텍스트 노드 업데이트
    const curEl = root.querySelector('[data-pb-cur]');
    if (curEl?.nextSibling?.nodeType === Node.TEXT_NODE) {
        curEl.nextSibling.textContent = ` / ${slides.length}`;
    }
}

function applyProductGalleryCards(root: HTMLElement, cards: ProductGalleryCard[], componentId: string) {
    if (componentId === 'product-gallery-web') {
        // 웹: data-pg-track 없음 — data-pg-grid 그리드 컨테이너
        const gridDiv = root.querySelector<HTMLElement>('[data-pg-grid]');
        if (gridDiv) {
            gridDiv.innerHTML = cards
                .map((c, i) => `<div style="flex:1;min-width:0;">${buildCardHtml(c, `pg-${i + 1}`)}</div>`)
                .join('');
        }
    } else {
        const track = root.querySelector('[data-pg-track]');
        if (track) {
            track.innerHTML = cards
                .map((c, i) => `<div data-pg-slide style="width:100%;">${buildCardHtml(c, `pg-${i + 1}`)}</div>`)
                .join('');
        }
    }
}

// ── 기본값 ────────────────────────────────────────────────────────────────

const DEFAULT_SLIDE: PromoBannerSlide = {
    itemId: '',
    bgColor: '#0046A4',
    badge: '이벤트',
    title: '새 배너 제목',
    desc: '배너 설명을 입력하세요',
    ctaText: '자세히 보기',
    ctaHref: '#',
};

const DEFAULT_CARD: ProductGalleryCard = {
    type: 'savings',
    badge: '적금',
    productName: '새 상품명',
    rateValue: '0.0',
    rateLabel: '최고 금리 (연)',
    detail: '기간 및 조건을 입력하세요',
    ctaHref: '#',
};

// ── 공통 스타일 ───────────────────────────────────────────────────────────

const INPUT: React.CSSProperties = {
    width: '100%',
    padding: '6px 10px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    fontSize: '13px',
    color: '#111827',
    background: '#fff',
    boxSizing: 'border-box',
    outline: 'none',
};

const LBL: React.CSSProperties = { fontSize: '11px', fontWeight: 600, color: '#6b7280' };

// ── 배경색 프리셋 ─────────────────────────────────────────────────────────

const BG_PRESETS = [
    { label: 'IBK 블루', value: '#0046A4' },
    { label: '하나 초록', value: '#008B5E' },
    { label: 'KB 노랑', value: '#F0B50A' },
    { label: '신한 파랑', value: '#005BAC' },
    { label: '우리 하늘', value: '#0070C0' },
    { label: '보라', value: '#6B21A8' },
    { label: '분홍', value: '#DB2777' },
    { label: '진회색', value: '#1F2937' },
    { label: '에메랄드', value: '#059669' },
];

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────

interface Props {
    /** 캔버스 DOM에서 가장 가까운 [data-component-id] 루트 요소 */
    blockEl: HTMLElement;
    onClose: () => void;
}

export default function SlideEditorModal({ blockEl, onClose }: Props) {
    const componentId = blockEl.getAttribute('data-component-id') ?? '';
    const isPromoBanner = componentId.startsWith('promo-banner-');

    const [promoSlides, setPromoSlides] = useState<PromoBannerSlide[]>(() => parsePromoBannerSlides(blockEl));
    const [productCards, setProductCards] = useState<ProductGalleryCard[]>(() =>
        parseProductGalleryCards(blockEl, componentId),
    );

    const [pos, setPos] = useState(() => ({
        x: Math.max(8, window.innerWidth / 2 - 280),
        y: Math.max(8, window.innerHeight / 2 - 300),
    }));
    const dragging = useRef(false);
    const dragStart = useRef({ mx: 0, my: 0, px: 0, py: 0 });

    const onHeaderMouseDown = useCallback(
        (e: React.MouseEvent) => {
            if ((e.target as HTMLElement).closest('button, input, select')) return;
            dragging.current = true;
            dragStart.current = { mx: e.clientX, my: e.clientY, px: pos.x, py: pos.y };
            e.preventDefault();
        },
        [pos],
    );

    useEffect(() => {
        const onMove = (e: MouseEvent) => {
            if (!dragging.current) return;
            setPos({
                x: dragStart.current.px + e.clientX - dragStart.current.mx,
                y: dragStart.current.py + e.clientY - dragStart.current.my,
            });
        };
        const onUp = () => {
            dragging.current = false;
        };
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
        return () => {
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
        };
    }, []);

    // 모달 열림 동안 열 툴바·RTE 툴바 숨김 (CB가 재표시해도 CSS로 강제 억제)
    useEffect(() => {
        document.body.classList.add('spw-slide-modal-open');
        return () => {
            document.body.classList.remove('spw-slide-modal-open');
        };
    }, []);

    // 확인: blockEl을 클론하여 변경 사항을 적용한 뒤 replaceWith로 교체.
    // ContentBuilder는 DOM 노드 레퍼런스로 HTML 스냅샷을 캐싱하므로,
    // 기존 노드에 innerHTML을 직접 수정하면 applyBehavior() 호출 시 캐시된 구버전으로 덮어쓰인다.
    // replaceWith로 새 노드를 삽입하면 ContentBuilder가 새 노드를 기준으로 재스냅샷하므로 변경이 유지된다.
    function handleConfirm() {
        const clone = blockEl.cloneNode(true) as HTMLElement;
        if (isPromoBanner) {
            applyPromoBannerSlides(clone, promoSlides);
        } else {
            applyProductGalleryCards(clone, productCards, componentId);
        }
        blockEl.replaceWith(clone);
        onClose();
    }

    // 취소: 편집 중 DOM을 건드리지 않으므로 그냥 닫기 (복원 불필요)
    function handleCancel() {
        onClose();
    }

    const title = isPromoBanner ? '홍보 배너 슬라이드 편집' : '금융 상품 카드 편집';

    return (
        <div
            style={{
                position: 'fixed',
                left: pos.x,
                top: pos.y,
                width: 560,
                maxHeight: '85vh',
                zIndex: 99999,
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: 12,
                boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
                display: 'flex',
                flexDirection: 'column',
                fontFamily: "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif",
                overflow: 'hidden',
            }}
        >
            {/* 헤더 (드래그 핸들) */}
            <div
                onMouseDown={onHeaderMouseDown}
                style={{
                    padding: '12px 14px',
                    borderBottom: '1px solid #f3f4f6',
                    borderRadius: '12px 12px 0 0',
                    background: '#fafafa',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexShrink: 0,
                    cursor: 'grab',
                    userSelect: 'none',
                }}
            >
                <span style={{ fontSize: '15px', fontWeight: 700, color: '#111827' }}>{title}</span>
                <button
                    onClick={handleCancel}
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
            <div
                style={{
                    padding: '16px 20px',
                    overflowY: 'auto',
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '12px',
                }}
            >
                {isPromoBanner ? (
                    <PromoSlidesEditor slides={promoSlides} onChange={setPromoSlides} />
                ) : (
                    <ProductCardsEditor cards={productCards} onChange={setProductCards} />
                )}
            </div>

            {/* 푸터 */}
            <div
                style={{
                    padding: '14px 20px',
                    borderTop: '1px solid #e5e7eb',
                    display: 'flex',
                    justifyContent: 'flex-end',
                    gap: '8px',
                    flexShrink: 0,
                }}
            >
                <button
                    onClick={handleCancel}
                    style={{
                        padding: '7px 16px',
                        border: '1px solid #d1d5db',
                        borderRadius: '8px',
                        background: '#fff',
                        color: '#374151',
                        fontSize: '13px',
                        fontWeight: 600,
                        cursor: 'pointer',
                    }}
                >
                    취소
                </button>
                <button
                    onClick={handleConfirm}
                    style={{
                        padding: '7px 16px',
                        border: 'none',
                        borderRadius: '8px',
                        background: '#0046A4',
                        color: '#fff',
                        fontSize: '13px',
                        fontWeight: 600,
                        cursor: 'pointer',
                    }}
                >
                    확인
                </button>
            </div>
        </div>
    );
}

// ── 홍보 배너 편집기 ──────────────────────────────────────────────────────

function PromoSlidesEditor({
    slides,
    onChange,
}: {
    slides: PromoBannerSlide[];
    onChange: React.Dispatch<React.SetStateAction<PromoBannerSlide[]>>;
}) {
    // functional update 패턴으로 작성해 openCmsFilesPicker 콜백이
    // 캡처 시점의 stale slides를 참조하는 문제를 방지한다.
    const update = (idx: number, patch: Partial<PromoBannerSlide>) =>
        onChange((prev) => prev.map((s, i) => (i === idx ? { ...s, ...patch } : s)));

    const add = () => onChange((prev) => [...prev, { ...DEFAULT_SLIDE, itemId: `pb-${Date.now()}` }]);

    const remove = (idx: number) => onChange((prev) => prev.filter((_, i) => i !== idx));

    return (
        <>
            {slides.map((slide, idx) => (
                <div
                    key={slide.itemId}
                    style={{
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px',
                        padding: '14px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '10px',
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ fontSize: '12px', fontWeight: 700, color: '#374151' }}>슬라이드 {idx + 1}</span>
                        <button
                            onClick={() => remove(idx)}
                            disabled={slides.length <= 1}
                            style={{
                                border: 'none',
                                background: 'transparent',
                                cursor: slides.length <= 1 ? 'not-allowed' : 'pointer',
                                color: slides.length <= 1 ? '#d1d5db' : '#ef4444',
                                fontSize: '12px',
                                padding: '2px 6px',
                            }}
                        >
                            삭제
                        </button>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>뱃지</span>
                            <input
                                value={slide.badge}
                                onChange={(e) => update(idx, { badge: e.target.value })}
                                style={INPUT}
                            />
                        </label>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>CTA 버튼 텍스트</span>
                            <input
                                value={slide.ctaText}
                                onChange={(e) => update(idx, { ctaText: e.target.value })}
                                style={INPUT}
                            />
                        </label>
                    </div>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>제목</span>
                        <input
                            value={slide.title}
                            onChange={(e) => update(idx, { title: e.target.value })}
                            style={INPUT}
                        />
                    </label>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>설명</span>
                        <input
                            value={slide.desc}
                            onChange={(e) => update(idx, { desc: e.target.value })}
                            style={INPUT}
                        />
                    </label>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>링크</span>
                        <input
                            value={slide.ctaHref}
                            onChange={(e) => update(idx, { ctaHref: e.target.value })}
                            style={INPUT}
                        />
                    </label>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        <span style={LBL}>배경색</span>
                        <div style={{ display: 'flex', gap: '5px', flexWrap: 'wrap', alignItems: 'center' }}>
                            {BG_PRESETS.map(({ label, value }) => (
                                <button
                                    key={value}
                                    title={label}
                                    onClick={() => update(idx, { bgColor: value })}
                                    style={{
                                        width: 28,
                                        height: 28,
                                        borderRadius: 6,
                                        background: value,
                                        border:
                                            slide.bgColor === value ? '2.5px solid #0046A4' : '2px solid transparent',
                                        cursor: 'pointer',
                                        padding: 0,
                                        outline: 'none',
                                        flexShrink: 0,
                                        boxShadow: '0 0 0 1px rgba(0,0,0,0.15)',
                                    }}
                                />
                            ))}
                            {/* 직접 단색 선택 */}
                            <label
                                title="직접 선택"
                                style={{
                                    width: 28,
                                    height: 28,
                                    borderRadius: 6,
                                    border: '2px solid #d1d5db',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    overflow: 'hidden',
                                    flexShrink: 0,
                                    background: 'conic-gradient(red, yellow, lime, cyan, blue, magenta, red)',
                                }}
                            >
                                <input
                                    type="color"
                                    value={slide.bgColor.startsWith('#') ? slide.bgColor : '#0046A4'}
                                    onChange={(e) => update(idx, { bgColor: e.target.value })}
                                    style={{ opacity: 0, width: 1, height: 1, padding: 0, border: 'none' }}
                                />
                            </label>
                        </div>
                    </div>
                    {/* 배경 이미지 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        <span style={LBL}>배경 이미지</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            {slide.bgImage ? (
                                <>
                                    <img
                                        src={slide.bgImage}
                                        style={{
                                            width: 64,
                                            height: 44,
                                            objectFit: 'cover',
                                            borderRadius: 6,
                                            border: '1px solid #e5e7eb',
                                        }}
                                        alt="배경 미리보기"
                                    />
                                    <button
                                        onClick={() => update(idx, { bgImage: undefined })}
                                        style={{
                                            fontSize: 12,
                                            color: '#ef4444',
                                            background: 'none',
                                            border: '1px solid #fca5a5',
                                            borderRadius: 6,
                                            padding: '4px 10px',
                                            cursor: 'pointer',
                                        }}
                                    >
                                        삭제
                                    </button>
                                </>
                            ) : (
                                <button
                                    type="button"
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: 6,
                                        padding: '6px 12px',
                                        border: '1px dashed #9ca3af',
                                        borderRadius: 6,
                                        cursor: 'pointer',
                                        fontSize: 12,
                                        color: '#374151',
                                        background: '#f9fafb',
                                    }}
                                    onClick={() => {
                                        try {
                                            openCmsFilesPicker((url) => {
                                                // AssetBrowser.handleConfirm 이 resolveAssetSrc 로 정규화된 URL 을
                                                // 전달하므로 추가 경로 조작 없이 그대로 사용한다.
                                                update(idx, { bgImage: url });
                                            });
                                        } catch {
                                            alert('cms/files 이미지 선택 창을 열 수 없습니다.');
                                        }
                                    }}
                                >
                                    + 이미지 선택
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            ))}
            <button
                onClick={add}
                style={{
                    padding: '8px',
                    border: '1px dashed #9ca3af',
                    borderRadius: '8px',
                    background: '#f9fafb',
                    color: '#374151',
                    fontSize: '13px',
                    fontWeight: 600,
                    cursor: 'pointer',
                }}
            >
                + 슬라이드 추가
            </button>
        </>
    );
}

// ── 금융 상품 카드 편집기 ─────────────────────────────────────────────────

const CARD_TYPE_OPTIONS: { value: ProductGalleryCard['type']; label: string }[] = [
    { value: 'savings', label: '적금' },
    { value: 'deposit', label: '예금' },
    { value: 'loan', label: '대출' },
];

const CARD_COLOR_PRESETS = [
    { label: 'IBK 블루', value: '#0046A4' },
    { label: '하나 초록', value: '#008B5E' },
    { label: 'KB 노랑', value: '#F0B50A' },
    { label: '신한 파랑', value: '#005BAC' },
    { label: '우리 하늘', value: '#0070C0' },
    { label: '주황', value: '#FF6600' },
    { label: '보라', value: '#6B21A8' },
    { label: '진회색', value: '#1F2937' },
];

function ProductCardsEditor({
    cards,
    onChange,
}: {
    cards: ProductGalleryCard[];
    onChange: React.Dispatch<React.SetStateAction<ProductGalleryCard[]>>;
}) {
    // functional update 패턴으로 작성해 openCmsFilesPicker 콜백이
    // 캡처 시점의 stale cards를 참조하는 문제를 방지한다.
    const update = (idx: number, patch: Partial<ProductGalleryCard>) =>
        onChange((prev) => prev.map((c, i) => (i === idx ? { ...c, ...patch } : c)));

    const add = () => onChange((prev) => [...prev, { ...DEFAULT_CARD }]);

    const remove = (idx: number) => onChange((prev) => prev.filter((_, i) => i !== idx));

    return (
        <>
            {cards.map((card, idx) => (
                <div
                    key={idx}
                    style={{
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px',
                        padding: '14px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '10px',
                    }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ fontSize: '12px', fontWeight: 700, color: '#374151' }}>상품 카드 {idx + 1}</span>
                        <button
                            onClick={() => remove(idx)}
                            disabled={cards.length <= 1}
                            style={{
                                border: 'none',
                                background: 'transparent',
                                cursor: cards.length <= 1 ? 'not-allowed' : 'pointer',
                                color: cards.length <= 1 ? '#d1d5db' : '#ef4444',
                                fontSize: '12px',
                                padding: '2px 6px',
                            }}
                        >
                            삭제
                        </button>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>상품 유형</span>
                            <select
                                value={card.type}
                                onChange={(e) => update(idx, { type: e.target.value as ProductGalleryCard['type'] })}
                                style={INPUT}
                            >
                                {CARD_TYPE_OPTIONS.map((o) => (
                                    <option key={o.value} value={o.value}>
                                        {o.label}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>뱃지 텍스트</span>
                            <input
                                value={card.badge}
                                onChange={(e) => update(idx, { badge: e.target.value })}
                                style={INPUT}
                            />
                        </label>
                    </div>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>상품명</span>
                        <input
                            value={card.productName}
                            onChange={(e) => update(idx, { productName: e.target.value })}
                            style={INPUT}
                        />
                    </label>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>금리 수치</span>
                            <input
                                value={card.rateValue}
                                onChange={(e) => update(idx, { rateValue: e.target.value })}
                                style={INPUT}
                                placeholder="예: 4.5"
                            />
                        </label>
                        <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={LBL}>금리 레이블</span>
                            <input
                                value={card.rateLabel}
                                onChange={(e) => update(idx, { rateLabel: e.target.value })}
                                style={INPUT}
                                placeholder="예: 최고 금리 (연)"
                            />
                        </label>
                    </div>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>상세 조건</span>
                        <input
                            value={card.detail}
                            onChange={(e) => update(idx, { detail: e.target.value })}
                            style={INPUT}
                        />
                    </label>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <span style={LBL}>링크</span>
                        <input
                            value={card.ctaHref}
                            onChange={(e) => update(idx, { ctaHref: e.target.value })}
                            style={INPUT}
                        />
                    </label>

                    {/* 강조 색상 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        <span style={LBL}>강조 색상</span>
                        <div style={{ display: 'flex', gap: '5px', flexWrap: 'wrap', alignItems: 'center' }}>
                            {CARD_COLOR_PRESETS.map(({ label, value }) => (
                                <button
                                    key={value}
                                    title={label}
                                    onClick={() => update(idx, { accentColor: value })}
                                    style={{
                                        width: 24,
                                        height: 24,
                                        borderRadius: 5,
                                        background: value,
                                        border:
                                            card.accentColor === value ? '2.5px solid #111' : '2px solid transparent',
                                        cursor: 'pointer',
                                        padding: 0,
                                        outline: 'none',
                                        flexShrink: 0,
                                        boxShadow: '0 0 0 1px rgba(0,0,0,0.15)',
                                    }}
                                />
                            ))}
                            <label
                                title="직접 선택"
                                style={{
                                    width: 24,
                                    height: 24,
                                    borderRadius: 5,
                                    border: '2px solid #d1d5db',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    overflow: 'hidden',
                                    flexShrink: 0,
                                    background: 'conic-gradient(red, yellow, lime, cyan, blue, magenta, red)',
                                }}
                            >
                                <input
                                    type="color"
                                    value={card.accentColor?.startsWith('#') ? card.accentColor : '#0046A4'}
                                    onChange={(e) => update(idx, { accentColor: e.target.value })}
                                    style={{ opacity: 0, width: 1, height: 1, padding: 0, border: 'none' }}
                                />
                            </label>
                            {card.accentColor && (
                                <button
                                    onClick={() => update(idx, { accentColor: undefined })}
                                    style={{
                                        fontSize: 11,
                                        color: '#6b7280',
                                        background: 'none',
                                        border: 'none',
                                        cursor: 'pointer',
                                        padding: '0 4px',
                                    }}
                                >
                                    초기화
                                </button>
                            )}
                        </div>
                    </div>

                    {/* 배경 이미지 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        <span style={LBL}>배경 이미지</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            {card.bgImage ? (
                                <>
                                    <img
                                        src={card.bgImage}
                                        style={{
                                            width: 64,
                                            height: 44,
                                            objectFit: 'cover',
                                            borderRadius: 6,
                                            border: '1px solid #e5e7eb',
                                        }}
                                        alt="배경 미리보기"
                                    />
                                    <button
                                        onClick={() => update(idx, { bgImage: undefined })}
                                        style={{
                                            fontSize: 12,
                                            color: '#ef4444',
                                            background: 'none',
                                            border: '1px solid #fca5a5',
                                            borderRadius: 6,
                                            padding: '4px 10px',
                                            cursor: 'pointer',
                                        }}
                                    >
                                        삭제
                                    </button>
                                </>
                            ) : (
                                <button
                                    type="button"
                                    style={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        gap: 6,
                                        padding: '6px 12px',
                                        border: '1px dashed #9ca3af',
                                        borderRadius: 6,
                                        cursor: 'pointer',
                                        fontSize: 12,
                                        color: '#374151',
                                        background: '#f9fafb',
                                    }}
                                    onClick={() => {
                                        try {
                                            openCmsFilesPicker((url) => {
                                                // AssetBrowser.handleConfirm 이 resolveAssetSrc 로 정규화된 URL 을
                                                // 전달하므로 추가 경로 조작 없이 그대로 사용한다.
                                                update(idx, { bgImage: url });
                                            });
                                        } catch {
                                            alert('cms/files 이미지 선택 창을 열 수 없습니다.');
                                        }
                                    }}
                                >
                                    + 이미지 선택
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            ))}
            <button
                onClick={add}
                style={{
                    padding: '8px',
                    border: '1px dashed #9ca3af',
                    borderRadius: '8px',
                    background: '#f9fafb',
                    color: '#374151',
                    fontSize: '13px',
                    fontWeight: 600,
                    cursor: 'pointer',
                }}
            >
                + 상품 카드 추가
            </button>
        </>
    );
}
