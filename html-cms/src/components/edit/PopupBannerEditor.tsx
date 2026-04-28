// src/components/edit/PopupBannerEditor.tsx
// popup-banner 이미지 팝업 배너 편집 패널 (Issue #281)
// 이미지 URL·링크·보지않기 기간 편집, data-images / data-hide-days 속성 업데이트

'use client';

import { useState } from 'react';

import { openCmsFilesPicker } from '@/lib/cms-file-picker';

// ── 데이터 모델 ──────────────────────────────────────────────────────────

interface BannerImage {
    url: string;
    link: string;
    alt: string;
}

interface Props {
    blockEl: HTMLElement;
    /** ContentBuilder openContentEditor 세 번째 인자 — 호출 시 ContentBuilder가 현재 DOM을 스냅샷하여 내부 HTML 갱신 */
    cbOnChange?: (() => void) | null;
    onClose: () => void;
}

// ── href 보안 처리 ───────────────────────────────────────────────────────

function sanitizeHref(url: string): string {
    const trimmed = url.trim();
    if (/^(https?:\/\/|\/|#)/.test(trimmed)) return trimmed;
    return '#';
}

// ── data 속성 파싱 ───────────────────────────────────────────────────────

function parseImages(el: HTMLElement): BannerImage[] {
    try {
        const raw = el.getAttribute('data-images');
        if (!raw) return [];
        return JSON.parse(raw) as BannerImage[];
    } catch {
        return [];
    }
}

function parseHideDays(el: HTMLElement): number {
    const raw = el.getAttribute('data-hide-days');
    const n = parseInt(raw ?? '3', 10);
    return isNaN(n) ? 3 : n;
}

// ── 패널 컴포넌트 ─────────────────────────────────────────────────────────

const FONT = "-apple-system,BlinkMacSystemFont,'Malgun Gothic','Apple SD Gothic Neo',sans-serif";

export default function PopupBannerEditor({ blockEl, cbOnChange, onClose }: Props) {
    const [images, setImages] = useState<BannerImage[]>(() => parseImages(blockEl));
    const [hideDays, setHideDays] = useState<number>(() => parseHideDays(blockEl));

    // ── 이미지 필드 변경 ─────────────────────────────────────────────────

    function updateImage(index: number, field: keyof BannerImage, value: string) {
        setImages((prev) => prev.map((img, i) => (i === index ? { ...img, [field]: value } : img)));
    }

    function addImage() {
        setImages((prev) => [...prev, { url: '', link: '#', alt: '' }]);
    }

    function removeImage(index: number) {
        setImages((prev) => prev.filter((_, i) => i !== index));
    }

    function moveImage(index: number, dir: -1 | 1) {
        const next = index + dir;
        if (next < 0 || next >= images.length) return;
        setImages((prev) => {
            const arr = [...prev];
            [arr[index], arr[next]] = [arr[next], arr[index]];
            return arr;
        });
    }

    // /cms/files 팝업에서 승인 이미지 선택 → 해당 슬롯의 url 교체
    function pickImage(index: number) {
        try {
            openCmsFilesPicker((url) => updateImage(index, 'url', url));
        } catch (err: unknown) {
            console.error('cms/files 이미지 선택 실패:', err);
        }
    }

    // ── 적용 ─────────────────────────────────────────────────────────────

    function handleApply() {
        const validated = images.map((img) => ({
            url: img.url.trim(),
            link: sanitizeHref(img.link),
            alt: img.alt.trim(),
        }));

        // popup-banner 요소는 data-cb-type만 있고 data-component-id가 없으므로
        // replaceWith 방식은 ContentBuilder 내부 참조를 갱신하지 못한다.
        // 올바른 방식:
        //   1) 속성 갱신 (DOM에 즉시 반영)
        //   2) cbOnChange() — ContentBuilder에 변경을 알려 내부 HTML 스냅샷 갱신
        //      → debouncedReinit의 applyBehavior() 가 구 스냅샷으로 DOM을 복원하는 것을 방지
        //   3) reinitialize() — Runtime이 즉시 unmount → mount 실행 (300ms debounce 없음)
        blockEl.setAttribute('data-images', JSON.stringify(validated));
        blockEl.setAttribute('data-hide-days', String(hideDays));
        cbOnChange?.();
        void window.builderRuntime?.reinitialize();
        onClose();
    }

    // ── 스타일 상수 ──────────────────────────────────────────────────────

    const inputStyle: React.CSSProperties = {
        width: '100%',
        height: '34px',
        padding: '0 10px',
        border: '1px solid #E5E7EB',
        borderRadius: '8px',
        fontSize: '13px',
        fontFamily: FONT,
        color: '#1A1A2E',
        background: '#fff',
        boxSizing: 'border-box',
        outline: 'none',
    };

    const labelStyle: React.CSSProperties = {
        fontSize: '11px',
        fontWeight: 600,
        color: '#6B7280',
        fontFamily: FONT,
        marginBottom: '4px',
        display: 'block',
    };

    const iconBtnStyle: React.CSSProperties = {
        width: '28px',
        height: '28px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        border: '1px solid #E5E7EB',
        borderRadius: '6px',
        background: '#fff',
        cursor: 'pointer',
        flexShrink: 0,
        padding: 0,
    };

    return (
        <>
            {/* 오버레이 */}
            <div
                onClick={onClose}
                style={{
                    position: 'fixed',
                    inset: 0,
                    zIndex: 99998,
                    background: 'rgba(0,0,0,0.35)',
                }}
            />

            {/* 패널 */}
            <div
                style={{
                    position: 'fixed',
                    left: '50%',
                    top: '50%',
                    transform: 'translate(-50%,-50%)',
                    width: 'min(380px, calc(100vw - 24px))',
                    maxHeight: '80vh',
                    zIndex: 99999,
                    background: '#fff',
                    border: '1px solid #E5E7EB',
                    borderRadius: '12px',
                    boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
                    display: 'flex',
                    flexDirection: 'column',
                    fontFamily: FONT,
                    overflow: 'hidden',
                }}
            >
                {/* 헤더 */}
                <div
                    style={{
                        padding: '12px 14px',
                        borderBottom: '1px solid #F3F4F6',
                        background: '#fafafa',
                        borderRadius: '12px 12px 0 0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                    }}
                >
                    <span style={{ fontSize: '14px', fontWeight: 700, color: '#1A1A2E' }}>이미지 팝업 배너 편집</span>
                    <button
                        onClick={onClose}
                        style={{ ...iconBtnStyle, border: 'none', background: 'transparent' }}
                        aria-label="닫기"
                    >
                        <svg
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="#374151"
                            strokeWidth="2"
                            strokeLinecap="round"
                        >
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                    </button>
                </div>

                {/* 본문 */}
                <div style={{ padding: '14px', overflowY: 'auto', flex: 1 }}>
                    {/* 이미지 목록 */}
                    <div style={{ marginBottom: '14px' }}>
                        <div
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                marginBottom: '8px',
                            }}
                        >
                            <span style={{ fontSize: '13px', fontWeight: 700, color: '#1A1A2E' }}>이미지 목록</span>
                            <button
                                onClick={addImage}
                                style={{
                                    padding: '4px 10px',
                                    border: '1px solid #0046A4',
                                    borderRadius: '6px',
                                    background: '#fff',
                                    color: '#0046A4',
                                    fontSize: '12px',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                    fontFamily: FONT,
                                }}
                            >
                                + 추가
                            </button>
                        </div>

                        {images.map((img, idx) => (
                            <div
                                key={idx}
                                style={{
                                    border: '1px solid #E5E7EB',
                                    borderRadius: '10px',
                                    padding: '10px 12px',
                                    marginBottom: '8px',
                                    background: '#F9FAFB',
                                }}
                            >
                                {/* 슬롯 헤더 */}
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        marginBottom: '8px',
                                    }}
                                >
                                    <span style={{ fontSize: '12px', fontWeight: 600, color: '#374151' }}>
                                        이미지 {idx + 1}
                                    </span>
                                    <div style={{ display: 'flex', gap: '4px' }}>
                                        <button
                                            onClick={() => moveImage(idx, -1)}
                                            disabled={idx === 0}
                                            style={{ ...iconBtnStyle, opacity: idx === 0 ? 0.3 : 1 }}
                                            title="위로"
                                        >
                                            <svg
                                                width="12"
                                                height="12"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="#374151"
                                                strokeWidth="2"
                                                strokeLinecap="round"
                                            >
                                                <polyline points="18 15 12 9 6 15" />
                                            </svg>
                                        </button>
                                        <button
                                            onClick={() => moveImage(idx, 1)}
                                            disabled={idx === images.length - 1}
                                            style={{ ...iconBtnStyle, opacity: idx === images.length - 1 ? 0.3 : 1 }}
                                            title="아래로"
                                        >
                                            <svg
                                                width="12"
                                                height="12"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="#374151"
                                                strokeWidth="2"
                                                strokeLinecap="round"
                                            >
                                                <polyline points="6 9 12 15 18 9" />
                                            </svg>
                                        </button>
                                        <button
                                            onClick={() => removeImage(idx)}
                                            disabled={images.length <= 1}
                                            style={{ ...iconBtnStyle, opacity: images.length <= 1 ? 0.3 : 1 }}
                                            title="삭제"
                                        >
                                            <svg
                                                width="12"
                                                height="12"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="#DC2626"
                                                strokeWidth="2"
                                                strokeLinecap="round"
                                            >
                                                <polyline points="3 6 5 6 21 6" />
                                                <path d="M19 6l-1 14H6L5 6" />
                                                <path d="M10 11v6M14 11v6" />
                                                <path d="M9 6V4h6v2" />
                                            </svg>
                                        </button>
                                    </div>
                                </div>

                                {/* 이미지 — cms/files에서 선택 */}
                                <div style={{ marginBottom: '6px' }}>
                                    <label style={labelStyle}>이미지</label>
                                    <div style={{ display: 'flex', gap: 6 }}>
                                        <input
                                            type="text"
                                            value={img.url}
                                            onChange={(e) => updateImage(idx, 'url', e.target.value)}
                                            placeholder="cms/files에서 이미지를 선택하세요"
                                            readOnly
                                            style={{ ...inputStyle, flex: 1 }}
                                        />
                                        <button
                                            type="button"
                                            onClick={() => pickImage(idx)}
                                            style={{
                                                padding: '0 10px',
                                                height: '34px',
                                                border: '1px solid #C7D8F4',
                                                borderRadius: 6,
                                                background: '#F0F4FF',
                                                color: '#0046A4',
                                                fontSize: 12,
                                                fontWeight: 600,
                                                cursor: 'pointer',
                                                whiteSpace: 'nowrap',
                                                fontFamily: FONT,
                                            }}
                                        >
                                            이미지 선택
                                        </button>
                                    </div>
                                    {img.url ? (
                                        <div style={{ marginTop: 6 }}>
                                            {/* eslint-disable-next-line @next/next/no-img-element */}
                                            <img
                                                src={img.url}
                                                alt=""
                                                style={{
                                                    width: '100%',
                                                    maxHeight: 120,
                                                    objectFit: 'contain',
                                                    border: '1px solid #E5E7EB',
                                                    borderRadius: 6,
                                                    background: '#fff',
                                                }}
                                            />
                                        </div>
                                    ) : null}
                                </div>

                                {/* 링크 URL */}
                                <div style={{ marginBottom: '6px' }}>
                                    <label style={labelStyle}>클릭 링크</label>
                                    <input
                                        type="text"
                                        value={img.link}
                                        onChange={(e) => updateImage(idx, 'link', e.target.value)}
                                        placeholder="https://... 또는 #"
                                        style={inputStyle}
                                    />
                                </div>

                                {/* 대체 텍스트 */}
                                <div>
                                    <label style={labelStyle}>대체 텍스트 (alt)</label>
                                    <input
                                        type="text"
                                        value={img.alt}
                                        onChange={(e) => updateImage(idx, 'alt', e.target.value)}
                                        placeholder="이미지 설명 (접근성)"
                                        style={inputStyle}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* 보지않기 기간 */}
                    <div>
                        <label
                            style={{
                                ...labelStyle,
                                fontSize: '13px',
                                fontWeight: 700,
                                color: '#1A1A2E',
                                marginBottom: '8px',
                            }}
                        >
                            보지 않기 기간
                        </label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <input
                                type="number"
                                min={1}
                                max={90}
                                value={hideDays}
                                onChange={(e) => setHideDays(Math.max(1, parseInt(e.target.value, 10) || 1))}
                                style={{ ...inputStyle, width: '80px' }}
                            />
                            <span style={{ fontSize: '13px', color: '#6B7280', fontFamily: FONT }}>일간 보지 않기</span>
                        </div>
                    </div>
                </div>

                {/* 버튼 영역 */}
                <div
                    style={{
                        padding: '10px 14px 14px',
                        borderTop: '1px solid #F3F4F6',
                        display: 'flex',
                        gap: '8px',
                        justifyContent: 'flex-end',
                    }}
                >
                    <button
                        onClick={onClose}
                        style={{
                            padding: '0 16px',
                            height: '36px',
                            border: '1px solid #E5E7EB',
                            borderRadius: '8px',
                            background: '#fff',
                            color: '#374151',
                            fontSize: '13px',
                            fontWeight: 500,
                            cursor: 'pointer',
                            fontFamily: FONT,
                        }}
                    >
                        취소
                    </button>
                    <button
                        onClick={handleApply}
                        style={{
                            padding: '0 20px',
                            height: '36px',
                            border: 'none',
                            borderRadius: '8px',
                            background: '#0046A4',
                            color: '#fff',
                            fontSize: '13px',
                            fontWeight: 600,
                            cursor: 'pointer',
                            fontFamily: FONT,
                            boxShadow: '0 2px 12px rgba(0,70,164,0.3)',
                        }}
                    >
                        적용
                    </button>
                </div>
            </div>
        </>
    );
}
