// src/components/edit/ProductMenuIconEditor.tsx
// product-menu 블록의 아이콘을 교체하는 드래그 가능 모달

'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { openCmsFilesPicker } from '@/lib/cms-file-picker';

// 아이콘 SVG — 피커 표시용 (크기는 컨테이너로 제어, stroke는 color 파라미터로 치환)
const ICONS: Record<string, string> = {
    deposit: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="15" rx="2"/><path d="M2 10h20"/><path d="M6 15h4"/><path d="M14 15h.01"/><path d="M18 15h.01"/></svg>`,
    loan: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 12V8H4v12h8"/><path d="M4 8V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2"/><circle cx="18" cy="18" r="4"/><path d="M18 16v2l1 1"/></svg>`,
    fund: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>`,
    trust: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>`,
    forex: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 12h8"/><path d="M15 8s2 1 2 4-2 4-2 4"/><path d="M9 8s-2 1-2 4 2 4 2 4"/></svg>`,
    insurance: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M23 12a11.05 11.05 0 0 0-22 0zm-5 7a3 3 0 0 1-6 0v-7"/></svg>`,
    card: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>`,
    isa: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/><polyline points="6 9 9 12 13 8 17 11"/></svg>`,
    pension: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
    transfer: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>`,
    stock: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
    new: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>`,
};

const ICON_LABELS: Record<string, string> = {
    deposit: '예금',
    loan: '대출',
    fund: '펀드',
    trust: '신탁',
    forex: '외환',
    insurance: '보험',
    card: '카드',
    isa: 'ISA',
    pension: '연금',
    transfer: '이체',
    stock: '증권',
    new: '기타',
};

// 색상 프리셋
const COLOR_PRESETS = ['#374151', '#0046A4', '#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#8b5cf6'];

// 페이지 저장 시 사용할 아이콘 HTML (width/height + stroke 색상 명시)
// aria-hidden="true" — 장식용 SVG는 .pm-label이 접근 텍스트를 담당하므로 스크린 리더 중복 낭독 방지
function buildIconHtml(key: string, color: string): string {
    return ICONS[key]
        .replace('<svg ', `<svg width="30" height="30" aria-hidden="true" `)
        .replace(/stroke="currentColor"/g, `stroke="${color}"`);
}

// SVG에서 현재 stroke 색상 추출
function readStrokeColor(itemEl: HTMLElement): string {
    const svg = itemEl.querySelector<SVGElement>('.pm-icon-wrap svg');
    return svg?.getAttribute('stroke') ?? '#374151';
}

interface Props {
    blockEl: HTMLElement;
    onClose: () => void;
}

export default function ProductMenuIconEditor({ blockEl, onClose }: Props) {
    // 모달 위치
    const [pos, setPos] = useState(() => ({
        x: Math.max(8, window.innerWidth / 2 - 120),
        y: 80,
    }));
    // 현재 피커가 열린 항목 인덱스
    const [pickerIdx, setPickerIdx] = useState<number | null>(null);
    // 피커가 열린 항목의 현재 아이콘 색상
    const [iconColor, setIconColor] = useState('#374151');
    // DOM 변경 후 리렌더 트리거
    const [tick, setTick] = useState(0);

    // 드래그
    const dragging = useRef(false);
    const dragStart = useRef({ mx: 0, my: 0, px: 0, py: 0 });

    const onHeaderMouseDown = useCallback(
        (e: React.MouseEvent) => {
            if ((e.target as HTMLElement).closest('button')) return;
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

    // 피커 열기 — 현재 아이콘 색상 읽어서 초기화
    const openPicker = useCallback(
        (idx: number, itemEl: HTMLElement) => {
            if (pickerIdx === idx) {
                setPickerIdx(null);
            } else {
                setIconColor(readStrokeColor(itemEl));
                setPickerIdx(idx);
            }
        },
        [pickerIdx],
    );

    // 아이콘 교체 — DOM 직접 수정
    const applyIcon = useCallback((itemEl: HTMLElement, key: string, color: string) => {
        const wrap = itemEl.querySelector('.pm-icon-wrap');
        if (wrap) wrap.innerHTML = buildIconHtml(key, color);
        setPickerIdx(null);
        setTick((n) => n + 1);
    }, []);

    // 색상 변경 — 현재 SVG의 stroke만 교체
    const applyColor = useCallback((itemEl: HTMLElement, color: string) => {
        const svg = itemEl.querySelector<SVGElement>('.pm-icon-wrap svg');
        if (svg) {
            svg.setAttribute('stroke', color);
            setTick((n) => n + 1);
        }
        setIconColor(color);
    }, []);

    // 전체 색상 일괄 변경
    const applyColorAll = useCallback(
        (color: string) => {
            blockEl.querySelectorAll<SVGElement>('.pm-icon-wrap svg').forEach((svg) => {
                svg.setAttribute('stroke', color);
            });
            setIconColor(color);
            setTick((n) => n + 1);
        },
        [blockEl],
    );

    // CMS 이미지 브라우저를 열어 아이콘 이미지 교체
    // openCmsFilesPicker — /cms/files 승인 이미지 선택기를 iframe 모달로 표시하고,
    // 선택 완료 시 콜백으로 URL 1건을 전달받는다.
    const openImagePicker = useCallback((itemEl: HTMLElement) => {
        try {
            openCmsFilesPicker((url) => {
                const wrap = itemEl.querySelector('.pm-icon-wrap');
                if (wrap) {
                    // alt="" — 장식용 이미지. .pm-label이 접근 텍스트를 담당하므로 빈 alt 허용
                    wrap.innerHTML = `<img src="${url}" alt="" style="width:30px;height:30px;object-fit:contain;" />`;
                }
                setPickerIdx(null);
                setTick((n) => n + 1);
            });
        } catch (err: unknown) {
            console.error('CMS 이미지 선택 실패:', err);
        }
    }, []);

    // tick 변경 시마다 최신 DOM에서 항목 재조회
    const items = Array.from(blockEl.querySelectorAll<HTMLElement>('.pm-item'));
    void tick; // tick을 의존성으로 사용해 리렌더 유도

    return (
        <div
            data-pm-icon-editor
            style={{
                position: 'fixed',
                left: pos.x,
                top: pos.y,
                width: 240,
                zIndex: 99999,
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: 12,
                boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
                fontFamily: "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif",
                fontSize: 13,
            }}
        >
            {/* ── 드래그 헤더 ── */}
            <div
                onMouseDown={onHeaderMouseDown}
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    borderBottom: '1px solid #f3f4f6',
                    borderRadius: '12px 12px 0 0',
                    background: '#fafafa',
                    cursor: 'grab',
                    userSelect: 'none',
                }}
            >
                <span style={{ fontWeight: 700, color: '#111827' }}>아이콘 편집</span>
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

            {/* ── 전체 색상 일괄 변경 ── */}
            <div style={{ padding: '10px 12px', borderBottom: '1px solid #f3f4f6' }}>
                <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 6, fontWeight: 600 }}>
                    전체 색상 일괄 변경
                </div>
                <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                    {COLOR_PRESETS.map((color) => (
                        <button
                            key={color}
                            title={color}
                            onClick={() => applyColorAll(color)}
                            style={{
                                width: 20,
                                height: 20,
                                borderRadius: 4,
                                background: color,
                                border: '2px solid transparent',
                                cursor: 'pointer',
                                padding: 0,
                                outline: 'none',
                                flexShrink: 0,
                            }}
                            onMouseEnter={(e) => {
                                (e.currentTarget as HTMLElement).style.borderColor = '#0046A4';
                            }}
                            onMouseLeave={(e) => {
                                (e.currentTarget as HTMLElement).style.borderColor = 'transparent';
                            }}
                        />
                    ))}
                    <label
                        title="직접 선택"
                        style={{
                            width: 20,
                            height: 20,
                            borderRadius: 4,
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
                            onChange={(e) => applyColorAll(e.target.value)}
                            style={{ opacity: 0, width: 1, height: 1, padding: 0, border: 'none' }}
                        />
                    </label>
                </div>
            </div>

            {/* ── 항목 목록 ── */}
            <div style={{ padding: 12, maxHeight: 380, overflowY: 'auto' }}>
                {items.length === 0 && (
                    <p style={{ color: '#9ca3af', textAlign: 'center', margin: '16px 0' }}>항목 없음</p>
                )}
                {items.map((item, idx) => {
                    const iconHtml = item.querySelector('.pm-icon-wrap')?.innerHTML ?? '';
                    const isOpen = pickerIdx === idx;
                    const hasSvg = iconHtml.includes('<svg');

                    return (
                        <div key={idx} style={{ marginBottom: 6 }}>
                            {/* 항목 행 */}
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 8,
                                    padding: '8px 10px',
                                    background: '#f9fafb',
                                    border: `1px solid ${isOpen ? '#0046A4' : '#e5e7eb'}`,
                                    borderRadius: 8,
                                }}
                            >
                                {/* 현재 아이콘 미리보기 */}
                                <div
                                    style={{
                                        width: 28,
                                        height: 28,
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        flexShrink: 0,
                                    }}
                                    dangerouslySetInnerHTML={{ __html: iconHtml }}
                                />
                                <button
                                    onClick={() => openPicker(idx, item)}
                                    style={{
                                        marginLeft: 'auto',
                                        padding: '4px 10px',
                                        border: '1px solid #d1d5db',
                                        borderRadius: 6,
                                        background: isOpen ? '#0046A4' : '#fff',
                                        color: isOpen ? '#fff' : '#374151',
                                        cursor: 'pointer',
                                        fontSize: 12,
                                        fontWeight: 600,
                                        flexShrink: 0,
                                    }}
                                >
                                    변경
                                </button>
                            </div>

                            {/* 아이콘 피커 */}
                            {isOpen && (
                                <div
                                    style={{
                                        marginTop: 4,
                                        padding: 10,
                                        background: '#fff',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: 8,
                                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                                    }}
                                >
                                    {/* SVG 아이콘 그리드 */}
                                    <div
                                        style={{
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(4, 1fr)',
                                            gap: 4,
                                            marginBottom: 8,
                                        }}
                                    >
                                        {Object.entries(ICONS).map(([key, svg]) => (
                                            <button
                                                key={key}
                                                title={ICON_LABELS[key]}
                                                onClick={() => applyIcon(item, key, iconColor)}
                                                style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    padding: '6px',
                                                    border: '1.5px solid transparent',
                                                    borderRadius: 8,
                                                    background: '#f9fafb',
                                                    cursor: 'pointer',
                                                    color: iconColor,
                                                }}
                                                onMouseEnter={(e) => {
                                                    (e.currentTarget as HTMLElement).style.borderColor = '#0046A4';
                                                    (e.currentTarget as HTMLElement).style.background = '#ebf4ff';
                                                }}
                                                onMouseLeave={(e) => {
                                                    (e.currentTarget as HTMLElement).style.borderColor = 'transparent';
                                                    (e.currentTarget as HTMLElement).style.background = '#f9fafb';
                                                }}
                                            >
                                                <span
                                                    style={{ display: 'flex', width: 22, height: 22 }}
                                                    dangerouslySetInnerHTML={{ __html: svg }}
                                                />
                                            </button>
                                        ))}
                                    </div>

                                    {/* 색상 선택 */}
                                    <div style={{ marginBottom: 8 }}>
                                        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginBottom: 4 }}>
                                            {COLOR_PRESETS.map((color) => (
                                                <button
                                                    key={color}
                                                    title={color}
                                                    onClick={() => applyColor(item, color)}
                                                    style={{
                                                        width: 20,
                                                        height: 20,
                                                        borderRadius: 4,
                                                        background: color,
                                                        border:
                                                            iconColor === color
                                                                ? '2px solid #0046A4'
                                                                : '2px solid transparent',
                                                        cursor: 'pointer',
                                                        padding: 0,
                                                        outline: 'none',
                                                        flexShrink: 0,
                                                        opacity: hasSvg ? 1 : 0.35,
                                                    }}
                                                />
                                            ))}
                                            {/* 커스텀 색상 */}
                                            <label
                                                title="직접 선택"
                                                style={{
                                                    width: 20,
                                                    height: 20,
                                                    borderRadius: 4,
                                                    border: '2px solid #d1d5db',
                                                    cursor: 'pointer',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    overflow: 'hidden',
                                                    flexShrink: 0,
                                                    opacity: hasSvg ? 1 : 0.35,
                                                    background:
                                                        'conic-gradient(red, yellow, lime, cyan, blue, magenta, red)',
                                                }}
                                            >
                                                <input
                                                    type="color"
                                                    value={iconColor}
                                                    disabled={!hasSvg}
                                                    onChange={(e) => applyColor(item, e.target.value)}
                                                    style={{
                                                        opacity: 0,
                                                        width: 1,
                                                        height: 1,
                                                        padding: 0,
                                                        border: 'none',
                                                    }}
                                                />
                                            </label>
                                        </div>
                                        {!hasSvg && (
                                            <p style={{ fontSize: 10, color: '#9ca3af', margin: 0 }}>
                                                이미지는 색상 변경 불가
                                            </p>
                                        )}
                                    </div>

                                    {/* CMS 이미지 선택 버튼 — /cms/files 브라우저를 열어 승인된 이미지를 아이콘으로 설정 */}
                                    <button
                                        onClick={() => openImagePicker(item)}
                                        style={{
                                            width: '100%',
                                            padding: 7,
                                            border: '1.5px dashed #d1d5db',
                                            borderRadius: 8,
                                            background: '#fff',
                                            color: '#374151',
                                            cursor: 'pointer',
                                            fontSize: 12,
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            gap: 6,
                                        }}
                                        onMouseEnter={(e) => {
                                            (e.currentTarget as HTMLElement).style.borderColor = '#0046A4';
                                            (e.currentTarget as HTMLElement).style.color = '#0046A4';
                                        }}
                                        onMouseLeave={(e) => {
                                            (e.currentTarget as HTMLElement).style.borderColor = '#d1d5db';
                                            (e.currentTarget as HTMLElement).style.color = '#374151';
                                        }}
                                    >
                                        {/* 이미지 프레임 아이콘 — CMS 이미지 브라우저 열기 의미 */}
                                        <svg
                                            width="13"
                                            height="13"
                                            viewBox="0 0 24 24"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            aria-hidden="true"
                                        >
                                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                                            <circle cx="8.5" cy="8.5" r="1.5" />
                                            <polyline points="21 15 16 10 5 21" />
                                        </svg>
                                        이미지 선택
                                    </button>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
