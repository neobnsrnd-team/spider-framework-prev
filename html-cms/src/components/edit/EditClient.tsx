// src/components/edit/EditClient.tsx
'use client';

import React, { useEffect, useRef, useState, useCallback, useMemo } from 'react';

// Runtime library for rendering ContentBuilder-generated content
import ContentBuilderRuntime from '@innovastudio/contentbuilder-runtime';
import '@innovastudio/contentbuilder-runtime/dist/contentbuilder-runtime.css';

// ContentBuilder library for editing
import ContentBuilder from '@innovastudio/contentbuilder';
import '@innovastudio/contentbuilder/public/contentbuilder/contentbuilder.css';

import ComponentPanel from '@/components/edit/ComponentPanel';
import AppHeaderBorderEditor from '@/components/edit/AppHeaderBorderEditor';
import AuthCenterIconEditor from '@/components/edit/AuthCenterIconEditor';
import BranchLocatorEditor from '@/components/edit/BranchLocatorEditor';
import InfoAccordionEditor from '@/components/edit/InfoAccordionEditor';
import MenuTabGridEditor from '@/components/edit/MenuTabGridEditor';
import BenefitCardEditor from '@/components/edit/BenefitCardEditor';
import MediaVideoEditor from '@/components/edit/MediaVideoEditor';
import ProductMenuIconEditor from '@/components/edit/ProductMenuIconEditor';
import SlideEditorModal from '@/components/edit/SlideEditorModal';
import SiteFooterSelectEditor from '@/components/edit/SiteFooterSelectEditor';
import FlexListEditor from '@/components/edit/FlexListEditor';
import InfoCardSlideEditor from '@/components/edit/InfoCardSlideEditor';
import PopupBannerEditor from '@/components/edit/PopupBannerEditor';
import StatusCardEditor from '@/components/edit/StatusCardEditor';
import MyDataAssetEditor from '@/components/edit/MyDataAssetEditor';
import FinanceCalendarEditor from '@/components/edit/FinanceCalendarEditor';
import EventBannerEditor from '@/components/edit/EventBannerEditor';
import CreatePageModal from '@/components/ui/CreatePageModal';
import Toast from '@/components/ui/Toast';
import type { FinanceComponent } from '@/data/finance-component-data';
import { type BrandTheme } from '@/data/brand-themes';
import ko from '@/data/ko';
import useToast from '@/hooks/useToast';
import { nextApi } from '@/lib/api-url';
import { attachResizeHandles, centerInitialBox } from '@/lib/resizable-box';
import { normalizeCmsViewMode } from '@/lib/view-mode';

// 기본 블록 타입 — DB SPW_CMS_COMPONENT에서 로드
export interface BasicBlock {
    id: string; // COMPONENT_ID (예: 'basic-web-001')
    thumbnail: string; // 썸네일 경로 (예: 'preview/basic-01b.png')
    html: string;
    viewMode: 'mobile' | 'web' | 'responsive';
    label?: string; // DB label (한국어 블록명)
}

// 캔버스에 올라간 블록 하나를 나타내는 타입
export interface ParsedBlock {
    id: string;
    cbType: string; // data-cb-type 값 (금융 컴포넌트 아닌 경우 빈 문자열)
    label: string; // 블록 이름 (금융 컴포넌트면 한글명, 아니면 타입명)
    preview: string; // 썸네일 경로 (금융 컴포넌트만 존재)
    outerHtml: string; // ContentBuilder가 감싼 전체 HTML
}

/**
 * ContentBuilder의 현재 HTML을 파싱하여 모든 블록 목록을 반환합니다.
 *
 * ContentBuilder는 각 블록을 <div class="row"><div class="column">...</div></div>
 * 구조로 감쌉니다. 이 함수는 최상위 row 요소 하나를 블록 하나로 취급합니다.
 * - row 안에 [data-cb-type] 포함 → 금융 컴포넌트 블록
 * - 그 외 → 기본 블록 (텍스트 미리보기 사용)
 *
 * outerHtml에는 row 전체를 저장하므로 reorder 시
 * loadHtml(rows.join(''))으로 ContentBuilder 구조가 그대로 복원됩니다.
 */
function parseBuilderBlocks(html: string, componentsMap?: Record<string, FinanceComponent>): ParsedBlock[] {
    if (!html?.trim()) return [];
    const doc = new DOMParser().parseFromString(html, 'text/html');

    // ContentBuilder의 최상위 row 요소들 (직계 자식)
    const rows = Array.from(doc.body.children);

    return rows.map((row, i) => {
        const rowEl = row as HTMLElement;

        // 플러그인 기반 금융 컴포넌트 (data-cb-type 존재)
        const pluginEl = rowEl.querySelector('[data-cb-type]');
        if (pluginEl) {
            const cbType = pluginEl.getAttribute('data-cb-type') ?? '';
            const comp = componentsMap?.[cbType];
            return {
                id: `block-${i}-${cbType}`,
                cbType,
                label: comp?.label ?? (cbType || `컴포넌트 ${i + 1}`),
                preview: comp?.preview ?? '',
                outerHtml: rowEl.outerHTML,
            };
        }

        // 순수 HTML 변환 금융 컴포넌트 (data-component-id 존재)
        const htmlCompEl = rowEl.querySelector('[data-component-id]');
        if (htmlCompEl) {
            const compId = htmlCompEl.getAttribute('data-component-id') ?? '';
            const comp = componentsMap?.[compId];
            return {
                id: `block-${i}-${compId}`,
                cbType: compId,
                label: comp?.label ?? (compId || `컴포넌트 ${i + 1}`),
                preview: comp?.preview ?? '',
                outerHtml: rowEl.outerHTML,
            };
        }

        // 기본 블록 — 텍스트 내용 앞 25자를 레이블로 사용
        const text = rowEl.textContent?.trim().replace(/\s+/g, ' ') ?? '';
        const label = text.slice(0, 25) || `기본 블록 ${i + 1}`;
        return {
            id: `block-${i}-basic`,
            cbType: '',
            label,
            preview: '',
            outerHtml: rowEl.outerHTML,
        };
    });
}

const btnStyle: React.CSSProperties = {
    padding: '6px 14px',
    borderRadius: '6px',
    border: '1px solid #e5e7eb',
    background: '#ffffff',
    color: '#374151',
    fontSize: '13px',
    fontWeight: 500,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
};

// CMS 전체에서 공유하는 색상 팔레트 — 플러그인 에디터에서 window.__cmsColors로 접근
const CMS_COLORS = [
    // ── 은행 대표 색상 ──
    '#004B9C',
    '#0064C8',
    '#5B9BD5',
    '#BDD7EE',
    '#008C6A',
    '#00A887',
    '#5EC4A8',
    '#B7E3D8',
    '#FFBC00',
    '#FFD966',
    '#594A2E',
    '#C9B07A',
    '#003DA5',
    '#0046FF',
    '#5B78D5',
    '#B4C2F0',
    // ── 기본 팔레트 ──
    '#000000',
    '#404040',
    '#808080',
    '#BFBFBF',
    '#FFFFFF',
    '#FF0000',
    '#FF6600',
    '#FFFF00',
    '#00FF00',
    '#00FFFF',
    '#0000FF',
    '#8000FF',
    '#FF00FF',
    '#FF0080',
];
if (typeof window !== 'undefined') window.__cmsColors = CMS_COLORS;

interface TabData {
    id: string;
    label: string;
    viewMode: ViewMode;
}

// 패널 너비 (접힌 상태: 40px, 펼친 상태: 264px) — CSS transition과 동기화
const PANEL_WIDTH_OPEN = 264;

// ── 뷰 모드 ──────────────────────────────────────────────────────────────
type ViewMode = 'mobile' | 'web' | 'responsive';

const VIEW_MODE_CONFIG: Record<ViewMode, { label: string; maxWidth: string; icon: string }> = {
    mobile: { label: '모바일', maxWidth: '390px', icon: '📱' },
    web: { label: '웹', maxWidth: '1280px', icon: '🖥️' },
    responsive: { label: '반응형', maxWidth: '100%', icon: '🔄' },
};

function roundMs(value: number) {
    return Math.round(value * 10) / 10;
}

function logEditPerf(label: string, metrics: Record<string, unknown>) {
    console.warn(`[cms/edit perf] ${label}`, metrics);
}

function normalizeViewMode(value: unknown): ViewMode {
    return normalizeCmsViewMode(value);
}

/** hex 색상(#RRGGBB)을 "R,G,B" 문자열로 변환 — rgba() 치환용 */
function hexToRgbValues(hex: string): string {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `${r},${g},${b}`;
}

/** IBK 기본 색상을 brandTheme 팔레트로 치환 */
function applyBrandTheme(html: string, theme: BrandTheme): string {
    const p = hexToRgbValues(theme.primary);
    const s = hexToRgbValues(theme.secondary);
    const pl = hexToRgbValues(theme.primaryLight);
    const sl = hexToRgbValues(theme.secondaryLight);

    return (
        html
            .replace(/#0046A4/gi, theme.primary)
            .replace(/#FF6600/gi, theme.secondary)
            .replace(/#E8F0FC/gi, theme.primaryLight)
            .replace(/#FFF3EC/gi, theme.secondaryLight)
            // rgb() 치환 — 컴포넌트 내부에 rgb 형태로 정의된 경우 대응 (공백 허용)
            .replace(/rgb\(\s*0\s*,\s*70\s*,\s*164\s*\)/gi, `rgb(${p})`)
            .replace(/rgb\(\s*255\s*,\s*102\s*,\s*0\s*\)/gi, `rgb(${s})`)
            .replace(/rgb\(\s*232\s*,\s*240\s*,\s*252\s*\)/gi, `rgb(${pl})`)
            .replace(/rgb\(\s*255\s*,\s*243\s*,\s*236\s*\)/gi, `rgb(${sl})`)
            // rgba() 치환 — 투명도 포함 색상 대응 (공백 허용)
            .replace(/rgba\(\s*0\s*,\s*70\s*,\s*164\s*,/gi, `rgba(${p},`)
            .replace(/rgba\(\s*255\s*,\s*102\s*,\s*0\s*,/gi, `rgba(${s},`)
    );
}

export default function EditClient({
    bank = 'ibk',
    userId,
    brandTheme,
    canWrite = true,
}: {
    bank?: string;
    userId: string;
    brandTheme?: BrandTheme | null;
    canWrite?: boolean;
}) {
    const builderRef = useRef<ContentBuilder | null>(null); // ContentBuilder 인스턴스
    const runtimeRef = useRef<ContentBuilderRuntime | null>(null); // Runtime 인스턴스
    const brandThemeRef = useRef(brandTheme ?? null); // onAdd 콜백에서 최신 테마 참조용
    useEffect(() => {
        brandThemeRef.current = brandTheme ?? null;
    }, [brandTheme]);
    const [containerOpacity, setContainerOpacity] = useState(0);
    const { toastMessage: infoToast, showToast: showInfoToast } = useToast();

    // 컴포넌트 패널 드래그 상태
    // — ref: 동기 접근용 (dragover 이벤트 핸들러 내 즉시 참조)
    // — state: 리렌더링 트리거용 (오버레이 표시/숨김)
    const isDraggingRef = useRef(false);
    const [isDraggingComponent, setIsDraggingComponent] = useState(false);
    const [isDropOver, setIsDropOver] = useState(false);

    // 드롭 삽입 위치 인덱스 (−1 = 끝에 추가)
    const dropInsertIdxRef = useRef<number>(-1);
    // 시각적 삽입 선 위치 (viewport 기준 Y, null이면 비표시)
    const [dropLineY, setDropLineY] = useState<number | null>(null);

    // 현재 캔버스에 올라간 블록 목록 (순서 변경 패널에서 사용)
    const [canvasBlocks, setCanvasBlocks] = useState<ParsedBlock[]>([]);
    // ref: 이벤트 핸들러 클로저에서 최신 blocks를 동기적으로 참조
    const canvasBlocksRef = useRef<ParsedBlock[]>([]);
    useEffect(() => {
        canvasBlocksRef.current = canvasBlocks;
    }, [canvasBlocks]);

    // content-plugins.js 기본 블록 (우측 패널 "기본 블록" 탭에서 사용)
    const [basicBlocks, setBasicBlocks] = useState<BasicBlock[]>([]);
    const [basicBlocksLoading, setBasicBlocksLoading] = useState(true);
    const [basicBlocksError, setBasicBlocksError] = useState<string | null>(null);

    // 금융 컴포넌트 (DB 또는 파일에서 로드)
    const [financeComponents, setFinanceComponents] = useState<FinanceComponent[]>([]);
    const [financeComponentsLoading, setFinanceComponentsLoading] = useState(true);
    const [financeComponentsError, setFinanceComponentsError] = useState<string | null>(null);
    const financeComponentsMap = useMemo(
        () =>
            financeComponents.reduce(
                (map, comp) => {
                    map[comp.id] = comp;
                    return map;
                },
                {} as Record<string, FinanceComponent>,
            ),
        [financeComponents],
    );
    const financeComponentsMapRef = useRef<Record<string, FinanceComponent>>({});
    useEffect(() => {
        financeComponentsMapRef.current = financeComponentsMap;
    }, [financeComponentsMap]);

    // 세션 스토리지 탭 목록 키 — 사용자별 분리
    const SESSION_TABS_KEY = `cms_editor_tabs_${userId}`;

    // 세션 기반 탭 목록 (현재 세션에서 열어본 페이지만 표시)
    const [tabs, setTabs] = useState<TabData[]>([]);
    const [tabsLoading, setTabsLoading] = useState(true);
    // 탭 추가 인라인 입력 표시 여부
    const [showAddTab, setShowAddTab] = useState(false);

    const getRemainingTabsWithoutCurrent = useCallback((): TabData[] => {
        const removeCurrent = (source: TabData[]) => source.filter((tab) => tab.id !== bank);

        if (tabs.length > 0) {
            return removeCurrent(tabs);
        }

        try {
            const stored = sessionStorage.getItem(SESSION_TABS_KEY);
            if (!stored) return [];
            const parsed = JSON.parse(stored) as TabData[];
            return removeCurrent(parsed).map((tab) => ({
                ...tab,
                viewMode: normalizeViewMode(tab.viewMode),
            }));
        } catch (err: unknown) {
            console.warn('탭 목록 세션 복원 실패:', err);
            return [];
        }
    }, [SESSION_TABS_KEY, bank, tabs]);

    const removeCurrentTabAndRedirect = useCallback(
        (message?: string) => {
            const remaining = getRemainingTabsWithoutCurrent();
            setTabs(remaining);

            if (message) {
                alert(message);
            }

            window.location.href =
                remaining.length > 0 ? nextApi(`/edit?bank=${remaining[0].id}`) : nextApi('/dashboard');
        },
        [getRemainingTabsWithoutCurrent],
    );

    // product-menu 아이콘 편집 모달
    const [productMenuBlock, setProductMenuBlock] = useState<HTMLElement | null>(null);
    // media-video 영상 URL 편집 모달
    const [mediaVideoBlock, setMediaVideoBlock] = useState<HTMLElement | null>(null);
    // site-footer 드롭다운 편집 패널
    const [siteFooterBlock, setSiteFooterBlock] = useState<HTMLElement | null>(null);
    // auth-center 아이콘 편집 패널
    const [authCenterBlock, setAuthCenterBlock] = useState<HTMLElement | null>(null);
    // app-header 구분선 편집 패널
    const [appHeaderBlock, setAppHeaderBlock] = useState<HTMLElement | null>(null);
    // branch-locator 지점 편집 패널
    const [branchLocatorBlock, setBranchLocatorBlock] = useState<HTMLElement | null>(null);
    // info-accordion 항목 편집 모달
    const [infoAccordionBlock, setInfoAccordionBlock] = useState<HTMLElement | null>(null);
    // menu-tab-grid 탭 항목 편집 모달
    const [menuTabGridBlock, setMenuTabGridBlock] = useState<HTMLElement | null>(null);
    // benefit-card 혜택 카드 편집 모달
    const [benefitCardBlock, setBenefitCardBlock] = useState<HTMLElement | null>(null);
    // flex-list 가변 리스트 편집 모달
    const [flexListBlock, setFlexListBlock] = useState<HTMLElement | null>(null);
    // info-card-slide 정보 카드 슬라이드 편집 모달
    const [infoCardBlock, setInfoCardBlock] = useState<HTMLElement | null>(null);
    // popup-banner 이미지 팝업 배너 편집 패널
    const [popupBannerBlock, setPopupBannerBlock] = useState<HTMLElement | null>(null);
    // popup-banner onChange: ContentBuilder 스냅샷 갱신 콜백 (openContentEditor 세 번째 인자)
    const [popupBannerOnChange, setPopupBannerOnChange] = useState<(() => void) | null>(null);
    // status-card 현황 카드 편집 모달
    const [statusCardBlock, setStatusCardBlock] = useState<HTMLElement | null>(null);
    const [myDataAssetBlock, setMyDataAssetBlock] = useState<HTMLElement | null>(null);
    const [financeCalendarBlock, setFinanceCalendarBlock] = useState<HTMLElement | null>(null);
    // event-banner 이벤트 배너 편집 모달
    const [eventBannerBlock, setEventBannerBlock] = useState<HTMLElement | null>(null);

    // 슬라이드 편집 모달 (promo-banner / product-gallery)
    const [slideEditorBlock, setSlideEditorBlock] = useState<HTMLElement | null>(null);

    // 이미지 교체 picker 모달 — #fileEmbedImage 인터셉트 시 /cms/files를 iframe으로 띄움
    // 별도 브라우저 창(window.open) 대신 에디터 DOM 내부 모달로 표시해
    // 주소창 노출·팝업 차단·별개 창 수명 문제를 제거한다.
    const [imagePickerOpen, setImagePickerOpen] = useState(false);
    // 모달 박스 ref — 초기 중앙 배치와 8방향 드래그 리사이즈 핸들 부착 대상
    const imagePickerBoxRef = useRef<HTMLDivElement>(null);

    // ── 현재 탭의 뷰 모드 (생성 시 결정, 이후 변경 불가) ─────────────────
    const currentTab = tabs.find((t) => t.id === bank);
    const viewMode: ViewMode = normalizeViewMode(currentTab?.viewMode);

    // ── 퀵 메뉴 아이콘 보호 패치 ────────────────────────────────────────
    // .pm-icon-wrap에 contenteditable="false"가 없으면 동적 주입
    // 탐색 범위를 .container로 제한하여 성능·안전성 확보
    // (마이그레이션 이전에 저장된 페이지도 에디터 로드·삽입·이동·삭제 후 보호)
    const patchPmIconWrap = useCallback(() => {
        const container = document.querySelector('.container');
        if (!container) return;
        container.querySelectorAll<HTMLElement>('.pm-icon-wrap:not([contenteditable])').forEach((el) => {
            el.contentEditable = 'false';
        });
    }, []);

    // patchMaxChars — 하위 호환용 no-op (실제 제한은 main useEffect의 document 핸들러가 담당)
    const patchMaxChars = useCallback(() => {}, []);

    useEffect(() => {
        const editorBootStart = performance.now();
        // 플러그인 재초기화 — 연속 호출 방지를 위해 300ms 디바운스
        // reinitialize(): Runtime이 data-cb-type 플러그인 DOM을 재마운트
        // applyBehavior(): ContentBuilder가 모든 row에 편집 이벤트 핸들러 재연결
        //   → reinitialize()가 플러그인 DOM을 교체하면 ContentBuilder가 앞쪽 row 참조를
        //     잃어 move/delete 버튼이 마지막 row에만 동작하는 문제를 방지합니다.

        let reinitTimer: ReturnType<typeof setTimeout> | null = null;
        const debouncedReinit = () => {
            if (reinitTimer) clearTimeout(reinitTimer);
            reinitTimer = setTimeout(async () => {
                // reinitialize()가 비동기(플러그인 JS/CSS lazy-load)이므로 완료 후 applyBehavior() 호출
                await runtimeRef.current?.reinitialize();
                builderRef.current?.applyBehavior();
                patchPmIconWrap();
                patchMaxChars();
                // ContentBuilder 자체 삭제/이동 후 순서 패널 동기화
                const html = builderRef.current?.html() ?? '';
                setCanvasBlocks(parseBuilderBlocks(html, financeComponentsMapRef.current));
            }, 300);
        };
        // 툴바의 이동/복제/삭제 후 ContentBuilder 재연결을 위해 전역 노출
        window.builderReinit = debouncedReinit;

        const upload = async (_file: File) => {
            throw new Error('이미지 업로드는 cms/files에서 승인된 이미지를 선택해 주세요.');
        };

        // Create ContentBuilder instance
        const builderInitStart = performance.now();
        builderRef.current = new ContentBuilder({
            container: '.container',
            previewURL: 'preview-with-plugins.html',
            lang: ko,
            sidePanel: 'left', // 설정 패널을 왼쪽에서 슬라이드 (오른쪽 컴포넌트 패널과 겹치지 않도록)
            // 삽입 HTML 앞뒤 공백 제거 — ContentBuilder 'row' 모드에서
            // 선행 개행이 childNodes[0]을 텍스트 노드로 만들어
            // element.tagName.toLowerCase() 크래시가 발생하는 버그 방지
            // ContentBuilder onAdd 타입이 (html: string) => string을 허용하지 않아 불가피하게 any 사용
            /* eslint-disable @typescript-eslint/no-explicit-any */
            onAdd: ((html: string) => {
                const trimmed = html.trim();
                const theme = brandThemeRef.current;
                // brandTheme 미설정 시 원본 색상 그대로 사용
                if (!theme) return trimmed;
                return applyBrandTheme(trimmed, theme);
            }) as any,
            /* eslint-enable @typescript-eslint/no-explicit-any */
            upload,

            // Enable Code Chat (supports OpenAI or OpenRouter)

            // clearChatSettings: true, // clear chat settings on load

            // OpenRouter:
            sendCommandUrl: nextApi('/api/openrouter'),
            sendCommandStreamUrl: nextApi('/api/openrouter/stream'),
            systemModel: 'openai/gpt-4o-mini', // Configure model for analyzing request
            codeModels: [
                // Configure available models for code generation
                { id: 'anthropic/claude-opus-4.5', label: 'Claude Opus 4.5' },
                { id: 'google/gemini-3-pro-preview', label: 'Google Gemini 3 Pro Preview' },
                { id: 'google/gemini-2.5-flash', label: 'Google Gemini 2.5 Flash' },
                { id: 'qwen/qwen3-coder', label: 'Qwen 3 Coder' },
                { id: 'openai/gpt-5-mini', label: 'GPT-5 Mini' },
                { id: 'openai/gpt-5.1-codex-mini', label: 'GPT-5.1-Codex-Mini' },
                { id: 'openai/gpt-5.1-codex', label: 'GPT-5.1-Codex' },
                { id: 'anthropic/claude-sonnet-4.5', label: 'Claude Sonnet 4.5' },
                { id: 'x-ai/grok-code-fast-1', label: 'Grok Code Fast 1' },
                { id: 'mistralai/codestral-2508', label: 'Mistral Codestral 2508' },
                { id: 'mistralai/devstral-small', label: 'Mistral Devstral Small' },
                { id: 'openai/gpt-oss-120b', label: 'GPT OSS 120B' },
                { id: 'google/gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash Lite' },
                { id: 'google/gemini-2.5-pro', label: 'Gemini 2.5 Pro' },
                { id: 'z-ai/glm-4.6', label: 'GLM 4.6' },
                { id: 'x-ai/grok-4-fast', label: 'Grok 4 Fast' },
                { id: 'mistralai/mistral-large-2407', label: 'Mistral Large 2407' },
                { id: 'mistralai/mistral-nemo', label: 'Mistral Nemo' },
                { id: 'moonshotai/kimi-k2-0905', label: 'Kimi K2' },
                { id: 'qwen/qwen3-vl-235b-a22b-instruct', label: 'Qwen 3 VL 235B' },
                { id: 'deepseek/deepseek-v3.1-terminus', label: 'DeepSeek V3.1 Terminus' },
                { id: 'deepseek/deepseek-chat-v3-0324', label: 'DeepSeek Chat V3' },
                { id: 'minimax/minimax-m2', label: 'MiniMax M2' },
            ],
            chatModels: [
                // Configure available models for chat
                { id: 'openai/gpt-4o-mini', label: 'GPT-4o Mini' },
                { id: 'openai/gpt-4o', label: 'GPT-4o' },
                { id: 'openai/gpt-5.1', label: 'GPT-5.1' },
                { id: 'openai/gpt-5.1-chat', label: 'GPT-5.1 Chat' },
                { id: 'openai/gpt-5-mini', label: 'GPT-5 Mini' },
                { id: 'openai/gpt-5-nano', label: 'GPT-5 Nano' },
                { id: 'anthropic/claude-sonnet-4.5', label: 'Claude Sonnet 4.5' },
                { id: 'google/gemini-2.5-flash', label: 'Google Gemini 2.5 Flash' },
                { id: 'google/gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash Lite' },
                { id: 'google/gemini-2.5-pro', label: 'Gemini 2.5 Pro' },
                { id: 'z-ai/glm-4.6', label: 'GLM 4.6' },
                { id: 'x-ai/grok-4-fast', label: 'Grok 4 Fast' },
                { id: 'mistralai/mistral-large-2407', label: 'Mistral Large 2407' },
                { id: 'mistralai/mistral-nemo', label: 'Mistral Nemo' },
                { id: 'moonshotai/kimi-k2-0905', label: 'Kimi K2' },
                { id: 'qwen/qwen3-vl-235b-a22b-instruct', label: 'Qwen 3 VL 235B' },
                { id: 'deepseek/deepseek-v3.1-terminus', label: 'DeepSeek V3.1 Terminus' },
                { id: 'deepseek/deepseek-chat-v3-0324', label: 'DeepSeek Chat V3' },
                { id: 'minimax/minimax-m2', label: 'MiniMax M2' },
            ],
            defaultChatSettings: {
                // Configure default code chat settings
                codeModel: 'google/gemini-3-pro-preview',
                chatModel: 'openai/gpt-5-mini',
                imageModel: 'fal-ai/nano-banana',
                imageSize: '',
            },

            defaultImageGenerationProvider: 'fal',
            generateMediaUrl_Fal: nextApi('/api/fal/request'),
            checkRequestStatusUrl_Fal: nextApi('/api/fal/status'),
            getResultUrl_Fal: nextApi('/api/fal/result'),
            // 이미지 삽입 시 승인된 이미지만 선택하도록 전용 브라우저(/files) 오픈
            // - filePicker: 스니펫 내부 커스텀 이미지 필드 (예: 퀵뱅킹 링크 선택)
            // - imageSelect: 에디터 기본 "이미지 변경" 툴바 버튼 (이미지 블록·헤더 이미지 등)
            filePicker: nextApi('/files'),
            imageSelect: nextApi('/files'),
            filePickerSize: 'medium',

            // 컬러 피커 색상 팔레트
            colors: CMS_COLORS,

            // 블록 추가/변경 시 플러그인 CSS·JS 재적용 (디바운스)
            onChange: debouncedReinit,
            onSnippetAdd: debouncedReinit,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any -- ContentBuilder 생성자 옵션 타입이 불완전하여 불가피하게 사용
        } as any);
        const builderInitMs = roundMs(performance.now() - builderInitStart);

        // ContentBuilder 기본 피커는 사용하지 않습니다.
        // 기본 블록은 아래 별도 useEffect에서 로드하여 우측 패널에 표시합니다.

        // Get basePath
        const basePath = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '');

        // Initialize runtime BEFORE loading content
        const runtimeInitStart = performance.now();
        try {
            runtimeRef.current = new ContentBuilderRuntime({
                // Registers available plugins (not yet loaded).
                // Scripts and styles are fetched only when the plugin is actually used in content.
                plugins: {
                    'logo-loop': {
                        url: basePath + '/assets/plugins/logo-loop/index.js',
                        css: basePath + '/assets/plugins/logo-loop/style.css',
                    },
                    'click-counter': {
                        url: basePath + '/assets/plugins/click-counter/index.js',
                        css: basePath + '/assets/plugins/click-counter/style.css',
                    },
                    'card-list': {
                        url: basePath + '/assets/plugins/card-list/index.js',
                        css: basePath + '/assets/plugins/card-list/style.css',
                    },
                    accordion: {
                        url: basePath + '/assets/plugins/accordion/index.js',
                        css: basePath + '/assets/plugins/accordion/style.css',
                    },
                    'hero-animation': {
                        url: basePath + '/assets/plugins/hero-animation/index.js',
                        css: basePath + '/assets/plugins/hero-animation/style.css',
                    },
                    'animated-stats': {
                        url: basePath + '/assets/plugins/animated-stats/index.js',
                        css: basePath + '/assets/plugins/animated-stats/style.css',
                    },
                    timeline: {
                        url: basePath + '/assets/plugins/timeline/index.js',
                        css: basePath + '/assets/plugins/timeline/style.css',
                    },
                    'before-after-slider': {
                        url: basePath + '/assets/plugins/before-after-slider/index.js',
                        css: basePath + '/assets/plugins/before-after-slider/style.css',
                    },
                    'more-info': {
                        url: basePath + '/assets/plugins/more-info/index.js',
                        css: basePath + '/assets/plugins/more-info/style.css',
                    },
                    'social-share': {
                        url: basePath + '/assets/plugins/social-share/index.js',
                        css: basePath + '/assets/plugins/social-share/style.css',
                    },
                    pendulum: {
                        url: basePath + '/assets/plugins/pendulum/index.js',
                        css: basePath + '/assets/plugins/pendulum/style.css',
                    },
                    'browser-mockup': {
                        url: basePath + '/assets/plugins/browser-mockup/index.js',
                        css: basePath + '/assets/plugins/browser-mockup/style.css',
                    },
                    'hero-background': {
                        url: basePath + '/assets/plugins/hero-background/index.js',
                        css: basePath + '/assets/plugins/hero-background/style.css',
                    },
                    'cta-buttons': {
                        url: basePath + '/assets/plugins/cta-buttons/index.js',
                        css: basePath + '/assets/plugins/cta-buttons/style.css',
                    },

                    'media-slider': {
                        url: basePath + '/assets/plugins/media-slider/index.js',
                        css: basePath + '/assets/plugins/media-slider/style.css',
                    },
                    'media-grid': {
                        url: basePath + '/assets/plugins/media-grid/index.js',
                        css: basePath + '/assets/plugins/media-grid/style.css',
                    },
                    'particle-constellation': {
                        url: basePath + '/assets/plugins/particle-constellation/index.js',
                        css: basePath + '/assets/plugins/particle-constellation/style.css',
                    },
                    'vector-force': {
                        url: basePath + '/assets/plugins/vector-force/index.js',
                        css: basePath + '/assets/plugins/vector-force/style.css',
                    },
                    'aurora-glow': {
                        url: basePath + '/assets/plugins/aurora-glow/index.js',
                        css: basePath + '/assets/plugins/aurora-glow/style.css',
                    },
                    'simple-stats': {
                        url: basePath + '/assets/plugins/simple-stats/index.js',
                        css: basePath + '/assets/plugins/simple-stats/style.css',
                    },
                    faq: {
                        url: basePath + '/assets/plugins/faq/index.js',
                        css: basePath + '/assets/plugins/faq/style.css',
                    },
                    'callout-box': {
                        url: basePath + '/assets/plugins/callout-box/index.js',
                        css: basePath + '/assets/plugins/callout-box/style.css',
                    },
                    code: {
                        url: basePath + '/assets/plugins/code/index.js',
                        css: basePath + '/assets/plugins/code/style.css',
                    },
                    'video-embed': {
                        // Experimental
                        url: basePath + '/assets/plugins/video-embed/index.js',
                        css: basePath + '/assets/plugins/video-embed/style.css',
                    },
                    'swiper-slider': {
                        url: basePath + '/assets/plugins/swiper-slider/index.js',
                        css: basePath + '/assets/plugins/swiper-slider/style.css',
                    },

                    // ── 금융 모바일 컴포넌트 (플러그인 유지 대상) ──────────────
                    // 순수 HTML 변환 완료분은 등록 제거 — 런타임 재개입 방지
                    // (app-header, product-menu, auth-center, media-video, site-footer, product-gallery, promo-banner, branch-locator)
                    'exchange-board': {
                        url: basePath + '/assets/plugins/exchange-board/index.js',
                        css: basePath + '/assets/plugins/exchange-board/style.css',
                    },
                    'loan-calculator': {
                        url: basePath + '/assets/plugins/loan-calculator/index.js',
                        css: basePath + '/assets/plugins/loan-calculator/style.css',
                    },
                    'popup-banner': {
                        url: basePath + '/assets/plugins/popup-banner/index.js',
                        css: basePath + '/assets/plugins/popup-banner/style.css',
                    },
                    'sticky-floating-bar': {
                        url: basePath + '/assets/plugins/sticky-floating-bar/index.js',
                        css: basePath + '/assets/plugins/sticky-floating-bar/style.css',
                    },
                },
            });
            // Make runtime available globally for ContentBuilder editor
            window.builderRuntime = runtimeRef.current;
            // 플러그인에서 에디터/뷰어 환경 구분에 사용
            window.__spwEditor = true;
        } catch (err: unknown) {
            console.error('런타임 초기화 오류:', err);
        }
        const runtimeInitMs = roundMs(performance.now() - runtimeInitStart);
        logEditPerf('editor-init', {
            builderInitMs,
            runtimeInitMs,
            initTotalMs: roundMs(performance.now() - editorBootStart),
        });

        // ── RTE 툴바 위치 보정 ────────────────────────────────────────────
        // ContentBuilder JS가 positionToolbar()에서 style.top을 반복 갱신하므로
        // MutationObserver로 툴바 요소의 style 변경을 감지해 top을 강제 오버라이드합니다.
        // 툴바를 네비바 아래, 캔버스 영역(뷰포트 - 우측 패널) 수평 중앙에 배치
        const fixRtePos = (el: HTMLElement) => {
            if (el.style.getPropertyValue('top') === '52px' && el.style.getPropertyPriority('top') === 'important')
                return;
            el.style.setProperty('top', '52px', 'important');
            el.style.setProperty('left', `calc((100vw - ${PANEL_WIDTH_OPEN}px) / 2)`, 'important');
            el.style.setProperty('transform', 'translateX(-50%)', 'important');
        };
        // 기존 코드와의 호환을 위한 별칭
        const fixRteTop = fixRtePos;

        const rteObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                // 새 요소가 DOM에 추가될 때
                mutation.addedNodes.forEach((node) => {
                    if (!(node instanceof HTMLElement)) return;
                    if (node.classList.contains('is-rte-tool') || node.classList.contains('is-elementrte-tool')) {
                        fixRteTop(node);
                    }
                });
                // 기존 툴바 요소의 style 속성이 변경될 때
                if (mutation.type === 'attributes' && mutation.target instanceof HTMLElement) {
                    const t = mutation.target;
                    if (t.classList.contains('is-rte-tool') || t.classList.contains('is-elementrte-tool')) {
                        fixRteTop(t);
                    }
                }
            });
        });
        rteObserver.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['style'],
        });

        // ContentBuilder 초기화 시 이미 생성된 툴바 요소에 즉시 적용
        document.querySelectorAll<HTMLElement>('.is-rte-tool, .is-elementrte-tool').forEach(fixRteTop);
        // 초기화 직후 비동기로도 한 번 더 적용 (ContentBuilder가 rAF 내에서 위치를 재설정할 경우 대비)
        setTimeout(() => {
            document.querySelectorAll<HTMLElement>('.is-rte-tool, .is-elementrte-tool').forEach(fixRteTop);
        }, 300);

        // ── 기본블록 설정 모달 드래그 이동 ────────────────────────────────────
        // ContentBuilder의 .is-modal은 라이브러리 내부에서 생성되므로 직접 수정 불가.
        // MutationObserver로 .is-modal DOM 추가를 감지하여 드래그 이벤트를 동적으로 주입.
        const makeModalDraggable = (modal: HTMLElement) => {
            // 이미 드래그 핸들러가 등록된 경우 중복 방지
            if (modal.dataset.draggable === 'true') return;
            modal.dataset.draggable = 'true';

            // 모달 헤더(첫 번째 자식 div 또는 모달 상단 영역)를 드래그 핸들로 사용
            const handle =
                modal.querySelector<HTMLElement>('.is-modal-header, .modal-header, div:first-child') ?? modal;

            handle.style.cursor = 'move';
            handle.style.userSelect = 'none';

            let startX = 0,
                startY = 0,
                startLeft = 0,
                startTop = 0;

            const onMouseMove = (e: MouseEvent) => {
                const dx = e.clientX - startX;
                const dy = e.clientY - startY;
                modal.style.left = `${startLeft + dx}px`;
                modal.style.top = `${startTop + dy}px`;
                modal.style.transform = 'none';
            };

            const onMouseUp = () => {
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);
            };

            handle.addEventListener('mousedown', (e: MouseEvent) => {
                // 입력 요소 클릭 시 드래그 방지
                if ((e.target as HTMLElement).closest('input, textarea, select, button')) return;

                const rect = modal.getBoundingClientRect();
                startX = e.clientX;
                startY = e.clientY;
                startLeft = rect.left;
                startTop = rect.top;

                // 드래그 시작 시 position을 fixed로 고정
                modal.style.position = 'fixed';
                modal.style.left = `${rect.left}px`;
                modal.style.top = `${rect.top}px`;
                modal.style.transform = 'none';
                modal.style.margin = '0';

                document.addEventListener('mousemove', onMouseMove);
                document.addEventListener('mouseup', onMouseUp);
                e.preventDefault();
            });
        };

        // MutationObserver로 .is-modal 생성 감지
        const modalObserver = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (!(node instanceof HTMLElement)) return;
                    // 직접 추가된 모달
                    if (node.classList.contains('is-modal')) {
                        makeModalDraggable(node);
                    }
                    // 하위에 포함된 모달
                    node.querySelectorAll<HTMLElement>('.is-modal').forEach(makeModalDraggable);
                });
            });
        });
        modalObserver.observe(document.body, { childList: true, subtree: true });

        // 이미 존재하는 모달에 즉시 적용
        document.querySelectorAll<HTMLElement>('.is-modal').forEach(makeModalDraggable);

        // ── #divLinkTool 커스텀 버튼 주입 ────────────────────────────────────
        // ContentBuilder는 <a> 태그 클릭 시 #divLinkTool 툴바를 보여준다.
        // 이 툴바에 커스텀 버튼을 한 번만 주입하고, 활성 요소 위치에 따라 가시성을 제어한다.
        //
        // [적용 컴포넌트]
        //   - product-menu  : .pm-item(<a>) 클릭 → 아이콘 편집 버튼 표시
        //   - media-video   : 제목 <a> 클릭   → 영상 URL 변경 버튼 표시
        //
        // [요구사항] 버튼을 추가하려는 컴포넌트 블록 안에 <a> 태그가 반드시 있어야 한다.
        const SPW_PM_BTN_CLASS = 'spw-pm-icon-edit-btn';
        const SPW_MV_BTN_CLASS = 'spw-mv-url-edit-btn';
        const SPW_AC_BTN_CLASS = 'spw-ac-icon-edit-btn';
        const SPW_AH_BTN_CLASS = 'spw-ah-border-edit-btn';
        const SPW_BL_ROW_BTN_CLASS = 'spw-bl-row-edit-btn';
        const SPW_PB_BTN_CLASS = 'spw-pb-edit-btn';

        // #divLinkTool에 커스텀 버튼 일괄 주입 (중복 주입 방지)
        const injectCustomButtonsToLinkTool = (linkTool: HTMLElement) => {
            // ① product-menu 아이콘 편집 버튼
            if (!linkTool.querySelector(`.${SPW_PM_BTN_CLASS}`)) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = SPW_PM_BTN_CLASS;
                btn.title = '아이콘 편집';
                // #divLinkTool 버튼 스타일 통일: width:37px height:37px transparent, fill:#111
                btn.style.cssText =
                    'display:none;width:37px;height:37px;flex-shrink:0;justify-content:center;align-items:center;background:transparent;cursor:pointer;border:none;padding:0;';
                btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="#111"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>`;
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    // pm-item 클릭: .icon-active → 상위 product-menu 탐색
                    // 외부 블록 클릭: .elm-active → 상위 product-menu 탐색
                    const anchor =
                        document
                            .querySelector<HTMLElement>('.icon-active')
                            ?.closest<HTMLElement>('[data-component-id^="product-menu"]') ??
                        document
                            .querySelector<HTMLElement>('.elm-active')
                            ?.closest<HTMLElement>('[data-component-id^="product-menu"]');
                    if (anchor) setProductMenuBlock(anchor);
                });
                linkTool.appendChild(btn);
            }

            // ② app-header 구분선 편집 버튼
            if (!linkTool.querySelector(`.${SPW_AH_BTN_CLASS}`)) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = SPW_AH_BTN_CLASS;
                btn.title = '구분선 편집';
                btn.style.cssText =
                    'display:none;width:37px;height:37px;flex-shrink:0;justify-content:center;align-items:center;background:transparent;cursor:pointer;border:none;padding:0;';
                btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#111" stroke-width="2" stroke-linecap="round"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6" stroke-width="3"/><line x1="3" y1="18" x2="21" y2="18"/></svg>`;
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    const block =
                        document
                            .querySelector<HTMLElement>('.icon-active')
                            ?.closest<HTMLElement>('[data-component-id^="app-header"]') ??
                        document
                            .querySelector<HTMLElement>('.elm-active')
                            ?.closest<HTMLElement>('[data-component-id^="app-header"]');
                    if (block) setAppHeaderBlock(block);
                });
                linkTool.appendChild(btn);
            }

            // ③ auth-center 아이콘 편집 버튼
            if (!linkTool.querySelector(`.${SPW_AC_BTN_CLASS}`)) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = SPW_AC_BTN_CLASS;
                btn.title = '아이콘 편집';
                btn.style.cssText =
                    'display:none;width:37px;height:37px;flex-shrink:0;justify-content:center;align-items:center;background:transparent;cursor:pointer;border:none;padding:0;';
                btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="#111"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>`;
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    const block =
                        document
                            .querySelector<HTMLElement>('.icon-active')
                            ?.closest<HTMLElement>('[data-component-id^="auth-center"]') ??
                        document
                            .querySelector<HTMLElement>('.elm-active')
                            ?.closest<HTMLElement>('[data-component-id^="auth-center"]');
                    if (block) setAuthCenterBlock(block);
                });
                linkTool.appendChild(btn);
            }

            // ③ media-video 영상 URL 변경 버튼
            if (!linkTool.querySelector(`.${SPW_MV_BTN_CLASS}`)) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = SPW_MV_BTN_CLASS;
                btn.title = '영상 URL 변경';
                btn.style.cssText =
                    'display:none;width:37px;height:37px;flex-shrink:0;justify-content:center;align-items:center;background:transparent;cursor:pointer;border:none;padding:0;';
                btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="#111"><path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z"/></svg>`;
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    const block =
                        document
                            .querySelector<HTMLElement>('.icon-active')
                            ?.closest<HTMLElement>('[data-component-id^="media-video"]') ??
                        document
                            .querySelector<HTMLElement>('.elm-active')
                            ?.closest<HTMLElement>('[data-component-id^="media-video"]');
                    if (block) setMediaVideoBlock(block);
                });
                linkTool.appendChild(btn);
            }

            // ⑥ popup-banner 이미지 팝업 편집 버튼
            if (!linkTool.querySelector(`.${SPW_PB_BTN_CLASS}`)) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = SPW_PB_BTN_CLASS;
                btn.title = '팝업 배너 편집';
                btn.style.cssText =
                    'display:none;width:37px;height:37px;flex-shrink:0;justify-content:center;align-items:center;background:transparent;cursor:pointer;border:none;padding:0;';
                btn.innerHTML = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#111" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>`;
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    e.preventDefault();
                    // popup-banner는 data-component-id 없이 data-cb-type만 사용
                    const block =
                        document
                            .querySelector<HTMLElement>('.icon-active')
                            ?.closest<HTMLElement>('[data-cb-type="popup-banner"]') ??
                        document
                            .querySelector<HTMLElement>('.elm-active')
                            ?.closest<HTMLElement>('[data-cb-type="popup-banner"]');
                    if (block) setPopupBannerBlock(block);
                });
                linkTool.appendChild(btn);
            }
        };

        // 활성 요소 위치에 따라 각 버튼 가시성 갱신
        const updateLinkToolBtnVisibility = () => {
            const pmBtn = document.querySelector<HTMLElement>(`#divLinkTool .${SPW_PM_BTN_CLASS}`);
            const acBtn = document.querySelector<HTMLElement>(`#divLinkTool .${SPW_AC_BTN_CLASS}`);
            const ahBtn = document.querySelector<HTMLElement>(`#divLinkTool .${SPW_AH_BTN_CLASS}`);
            const mvBtn = document.querySelector<HTMLElement>(`#divLinkTool .${SPW_MV_BTN_CLASS}`);
            const iconActive = document.querySelector('.icon-active');
            const elmActive = document.querySelector('.elm-active');

            if (pmBtn) {
                const isInPm =
                    !!iconActive?.closest('[data-component-id^="product-menu"]') ||
                    !!elmActive?.closest('[data-component-id^="product-menu"]');
                pmBtn.style.display = isInPm ? 'flex' : 'none';
            }
            if (acBtn) {
                const isInAc =
                    !!iconActive?.closest('[data-component-id^="auth-center"]') ||
                    !!elmActive?.closest('[data-component-id^="auth-center"]');
                acBtn.style.display = isInAc ? 'flex' : 'none';
            }
            if (ahBtn) {
                const isInAh =
                    !!iconActive?.closest('[data-component-id^="app-header"]') ||
                    !!elmActive?.closest('[data-component-id^="app-header"]');
                ahBtn.style.display = isInAh ? 'flex' : 'none';
            }
            if (mvBtn) {
                const isInMv =
                    !!iconActive?.closest('[data-component-id^="media-video"]') ||
                    !!elmActive?.closest('[data-component-id^="media-video"]');
                mvBtn.style.display = isInMv ? 'flex' : 'none';
            }
            const pbBtn = document.querySelector<HTMLElement>(`#divLinkTool .${SPW_PB_BTN_CLASS}`);
            if (pbBtn) {
                // popup-banner는 data-component-id 없이 data-cb-type만 사용
                const isInPb =
                    !!iconActive?.closest('[data-cb-type="popup-banner"]') ||
                    !!elmActive?.closest('[data-cb-type="popup-banner"]');
                pbBtn.style.display = isInPb ? 'flex' : 'none';
            }
        };

        const colToolObserver = new MutationObserver((mutations) => {
            let needsVisibilityUpdate = false;
            mutations.forEach((mutation) => {
                // #divLinkTool 추가 감지 → 버튼 주입
                mutation.addedNodes.forEach((node) => {
                    if (!(node instanceof HTMLElement)) return;
                    if (node.id === 'divLinkTool') injectCustomButtonsToLinkTool(node);
                    node.querySelectorAll<HTMLElement>('#divLinkTool').forEach(injectCustomButtonsToLinkTool);
                });
                // .icon-active 또는 .elm-active 클래스 변화 → 가시성 갱신
                if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                    const cl = (mutation.target as HTMLElement).classList;
                    if (cl.contains('icon-active') || cl.contains('elm-active')) {
                        needsVisibilityUpdate = true;
                        const activeEl = mutation.target as HTMLElement;
                        void activeEl; // 가시성 갱신 외 자동 오픈 없음
                    }
                }
            });
            if (needsVisibilityUpdate) updateLinkToolBtnVisibility();
        });
        colToolObserver.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class'],
        });

        // 이미 DOM에 있는 #divLinkTool에 즉시 적용
        const existingLinkTool = document.querySelector<HTMLElement>('#divLinkTool');
        if (existingLinkTool) injectCustomButtonsToLinkTool(existingLinkTool);

        // ── 슬라이드 편집 버튼 — .is-row-tool 주입 ───────────────────────────
        // promo-banner / product-gallery 블록 선택 시 행 툴바에 "슬라이드 편집" 버튼 추가
        const SPW_SLIDE_BTN_CLASS = 'spw-slide-edit-btn';

        const injectSlideEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_SLIDE_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_SLIDE_BTN_CLASS;
            btn.title = '슬라이드 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            // 슬라이드 스택 아이콘
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="#fff"><path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block =
                    activeEl?.closest<HTMLElement>('[data-component-id^="promo-banner"]') ??
                    activeEl?.closest<HTMLElement>('[data-component-id^="product-gallery"]');
                if (block) setSlideEditorBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateSlideToolBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_SLIDE_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isSlide =
                    !!activeEl?.closest('[data-component-id^="promo-banner"]') ||
                    !!activeEl?.closest('[data-component-id^="product-gallery"]');
                btn.style.display = isSlide ? 'flex' : 'none';
            });
        };

        // ── menu-tab-grid 탭 항목 편집 버튼 — .is-row-tool 주입 ────────────────
        const SPW_MTG_ROW_BTN_CLASS = 'spw-mtg-row-edit-btn';

        const injectMtgEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_MTG_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_MTG_ROW_BTN_CLASS;
            btn.title = '탭 메뉴 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="menu-tab-grid"]');
                if (block) setMenuTabGridBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateMtgRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_MTG_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isMtg = !!activeEl?.closest('[data-component-id^="menu-tab-grid"]');
                btn.style.display = isMtg ? 'flex' : 'none';
            });
        };

        // ── benefit-card 혜택 카드 편집 버튼 — .is-row-tool 주입 ────────────────
        const SPW_BC_ROW_BTN_CLASS = 'spw-bc-row-edit-btn';

        const injectBcEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_BC_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_BC_ROW_BTN_CLASS;
            btn.title = '혜택 카드 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 3H8L2 7h20l-6-4z"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="benefit-card"]');
                if (block) setBenefitCardBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateBcRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_BC_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isBc = !!activeEl?.closest('[data-component-id^="benefit-card"]');
                btn.style.display = isBc ? 'flex' : 'none';
            });
        };

        // ── info-accordion 항목 편집 버튼 — .is-row-tool 주입 ────────────────
        const SPW_IA_ROW_BTN_CLASS = 'spw-ia-row-edit-btn';

        const injectIaEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_IA_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_IA_ROW_BTN_CLASS;
            btn.title = '항목 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="info-accordion"]');
                if (block) setInfoAccordionBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateIaRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_IA_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isIa = !!activeEl?.closest('[data-component-id^="info-accordion"]');
                btn.style.display = isIa ? 'flex' : 'none';
            });
        };

        // ── flex-list 가변 리스트 편집 버튼 — .is-row-tool 주입 ────────────────
        const SPW_FL_ROW_BTN_CLASS = 'spw-fl-row-edit-btn';

        const injectFlEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_FL_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_FL_ROW_BTN_CLASS;
            btn.title = '가변 리스트 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6h13"/><path d="M8 12h13"/><path d="M8 18h13"/><path d="M3 6h.01"/><path d="M3 12h.01"/><path d="M3 18h.01"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="flex-list"]');
                if (block) setFlexListBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateFlRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_FL_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isFl = !!activeEl?.closest('[data-component-id^="flex-list"]');
                btn.style.display = isFl ? 'flex' : 'none';
            });
        };

        // ── info-card-slide 정보 카드 편집 버튼 — .is-row-tool 주입 ──────────
        const SPW_ICS_ROW_BTN_CLASS = 'spw-ics-row-edit-btn';

        const injectIcsEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_ICS_ROW_BTN_CLASS}`)) return;
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_ICS_ROW_BTN_CLASS;
            btn.title = '정보 카드 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="info-card-slide"]');
                if (block) setInfoCardBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateIcsRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_ICS_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isIcs = !!activeEl?.closest('[data-component-id^="info-card-slide"]');
                btn.style.display = isIcs ? 'flex' : 'none';
            });
        };

        // ── status-card 현황 카드 편집 버튼 — .is-row-tool 주입 ────────────────
        const SPW_SC_ROW_BTN_CLASS = 'spw-sc-row-edit-btn';

        const injectScEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_SC_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_SC_ROW_BTN_CLASS;
            btn.title = '현황 카드 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="status-card"]');
                if (block) setStatusCardBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateScRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_SC_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isSc = !!activeEl?.closest('[data-component-id^="status-card"]');
                btn.style.display = isSc ? 'flex' : 'none';
            });
        };

        const SPW_MA_ROW_BTN_CLASS = 'spw-ma-row-edit-btn';

        const injectMaEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_MA_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_MA_ROW_BTN_CLASS;
            btn.title = '자산 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 1 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="mydata-asset"]');
                if (block) setMyDataAssetBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateMaRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_MA_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isMa = !!activeEl?.closest('[data-component-id^="mydata-asset"]');
                btn.style.display = isMa ? 'flex' : 'none';
            });
        };

        const SPW_FC_ROW_BTN_CLASS = 'spw-fc-row-edit-btn';

        const injectFcEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_FC_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_FC_ROW_BTN_CLASS;
            btn.title = '금융 일정 캘린더 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="finance-calendar"]');
                if (block) setFinanceCalendarBlock(block);
            });
            rowTool.appendChild(btn);
        };

        const updateFcRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_FC_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isFc = !!activeEl?.closest('[data-component-id^="finance-calendar"]');
                btn.style.display = isFc ? 'flex' : 'none';
            });
        };

        // ── event-banner 이벤트 배너 ──
        const SPW_EB_ROW_BTN_CLASS = 'spw-eb-row-edit-btn';
        const injectEbEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_EB_ROW_BTN_CLASS}`)) return;
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_EB_ROW_BTN_CLASS;
            btn.title = '이벤트 배너 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5" fill="#fff" stroke="none"/><polyline points="21 15 16 10 5 21"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="event-banner"]');
                if (block) setEventBannerBlock(block);
            });
            rowTool.appendChild(btn);
        };
        const updateEbRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_EB_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isEb = !!activeEl?.closest('[data-component-id^="event-banner"]');
                btn.style.display = isEb ? 'flex' : 'none';
            });
        };

        // ── branch-locator 지점 찾기 편집 버튼 — .is-row-tool 주입 ────────────────
        const injectBlEditToRowTool = (rowTool: HTMLElement) => {
            if (rowTool.querySelector(`.${SPW_BL_ROW_BTN_CLASS}`)) return;

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = SPW_BL_ROW_BTN_CLASS;
            btn.title = '지점 찾기 편집';
            btn.style.cssText =
                'display:none;width:28px;height:28px;flex-shrink:0;justify-content:center;align-items:center;background:rgba(0,70,164,0.9);cursor:pointer;border:none;padding:0;';
            btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`;
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                e.preventDefault();
                const activeEl = document.querySelector<HTMLElement>('.elm-active');
                const block = activeEl?.closest<HTMLElement>('[data-component-id^="branch-locator"]');
                if (block) setBranchLocatorBlock(block);
            });
            rowTool.appendChild(btn);
        };
        const updateBlRowBtnVisibility = () => {
            document.querySelectorAll<HTMLElement>(`.${SPW_BL_ROW_BTN_CLASS}`).forEach((btn) => {
                const activeEl = document.querySelector('.elm-active');
                const isBl = !!activeEl?.closest('[data-component-id^="branch-locator"]');
                btn.style.display = isBl ? 'flex' : 'none';
            });
        };

        // 슬라이드·아코디언 컴포넌트 행 툴바 감지 — colToolObserver와 별도 옵저버 사용
        const slideToolObserver = new MutationObserver((mutations) => {
            let needsRowToolVisibility = false;
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (!(node instanceof HTMLElement)) return;
                    if (node.classList.contains('is-row-tool')) {
                        injectSlideEditToRowTool(node);
                        injectIaEditToRowTool(node);
                        injectMtgEditToRowTool(node);
                        injectBcEditToRowTool(node);
                        injectFlEditToRowTool(node);
                        injectIcsEditToRowTool(node);
                        injectScEditToRowTool(node);
                        injectMaEditToRowTool(node);
                        injectFcEditToRowTool(node);
                        injectEbEditToRowTool(node);
                        injectBlEditToRowTool(node);
                    }
                    node.querySelectorAll<HTMLElement>('.is-row-tool').forEach((t) => {
                        injectSlideEditToRowTool(t);
                        injectIaEditToRowTool(t);
                        injectMtgEditToRowTool(t);
                        injectBcEditToRowTool(t);
                        injectFlEditToRowTool(t);
                        injectIcsEditToRowTool(t);
                        injectScEditToRowTool(t);
                        injectMaEditToRowTool(t);
                        injectFcEditToRowTool(t);
                        injectEbEditToRowTool(t);
                        injectBlEditToRowTool(t);
                    });
                });
                if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                    if ((mutation.target as HTMLElement).classList.contains('elm-active')) {
                        needsRowToolVisibility = true;
                    }
                }
            });
            if (needsRowToolVisibility) {
                updateSlideToolBtnVisibility();
                updateIaRowBtnVisibility();
                updateMtgRowBtnVisibility();
                updateBcRowBtnVisibility();
                updateFlRowBtnVisibility();
                updateIcsRowBtnVisibility();
                updateScRowBtnVisibility();
                updateMaRowBtnVisibility();
                updateFcRowBtnVisibility();
                updateEbRowBtnVisibility();
                updateBlRowBtnVisibility();
            }
        });
        slideToolObserver.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class'],
        });

        // 이미 DOM에 있는 .is-row-tool에 즉시 적용
        document.querySelectorAll<HTMLElement>('.is-row-tool').forEach((t) => {
            injectSlideEditToRowTool(t);
            injectIaEditToRowTool(t);
            injectMtgEditToRowTool(t);
            injectBcEditToRowTool(t);
            injectFlEditToRowTool(t);
            injectIcsEditToRowTool(t);
            injectScEditToRowTool(t);
            injectMaEditToRowTool(t);
            injectFcEditToRowTool(t);
            injectEbEditToRowTool(t);
            injectBlEditToRowTool(t);
        });

        // ── quickadd 팝업 드래그 이동 ─────────────────────────────────────────
        // .is-pop.quickadd는 DOM에 항상 존재하며 ContentBuilder가 display만 토글함.
        // style 변경을 감지하여 표시될 때 position:fixed로 전환 + 드래그 핸들 등록.
        const makeQuickaddDraggable = (popup: HTMLElement) => {
            if (popup.dataset.draggableQa === 'true') return;
            popup.dataset.draggableQa = 'true';
            popup.style.cursor = 'move';
            popup.style.userSelect = 'none';

            let startX = 0,
                startY = 0,
                startLeft = 0,
                startTop = 0;
            let isDragging = false;

            const onMouseMove = (e: MouseEvent) => {
                if (!isDragging) return;
                popup.style.left = `${startLeft + e.clientX - startX}px`;
                popup.style.top = `${startTop + e.clientY - startY}px`;
            };
            const onMouseUp = () => {
                isDragging = false;
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);
            };

            popup.addEventListener('mousedown', (e: MouseEvent) => {
                // 버튼 클릭은 드래그 시작 안 함 (실제 항목 선택 동작 보존)
                if ((e.target as HTMLElement).closest('button')) return;

                // 처음 드래그 시작 시 absolute → fixed 전환하여 뷰포트 기준으로 고정
                if (popup.style.position !== 'fixed') {
                    const rect = popup.getBoundingClientRect();
                    popup.style.position = 'fixed';
                    popup.style.left = `${rect.left}px`;
                    popup.style.top = `${rect.top}px`;
                    popup.style.margin = '0';
                }

                isDragging = true;
                const rect = popup.getBoundingClientRect();
                startX = e.clientX;
                startY = e.clientY;
                startLeft = rect.left;
                startTop = rect.top;

                document.addEventListener('mousemove', onMouseMove);
                document.addEventListener('mouseup', onMouseUp);
                e.preventDefault();
            });
        };

        // style 변경으로 표시 여부가 바뀌는 것을 감지 → 드래그 핸들 1회 등록
        const quickaddObserver = new MutationObserver(() => {
            const popup = document.querySelector<HTMLElement>('#_cbhtml .is-pop.quickadd');
            if (!popup) return;
            const visible = popup.style.display !== '' && popup.style.display !== 'none';
            if (visible) makeQuickaddDraggable(popup);
        });
        quickaddObserver.observe(document.body, {
            subtree: true,
            attributes: true,
            attributeFilter: ['style'],
        });

        // 에디터 캔버스 내 링크 클릭 시 페이지 이동 차단
        // (컴포넌트 내부 <a href> 가 실제 네비게이션을 일으키는 문제 방지)
        const blockCanvasLinkNavigation = (e: MouseEvent) => {
            const anchor = (e.target as HTMLElement).closest('a[href]');
            if (!anchor) return;
            // 캔버스(.container) 내부 링크만 차단 — 네비바 탭 등 외부 링크는 정상 동작
            const container = document.querySelector('.container');
            if (!container?.contains(anchor)) return;
            const href = anchor.getAttribute('href');
            // '#'이나 'javascript:'는 이미 이동 없으므로 제외
            if (href && href !== '#' && !href.startsWith('javascript')) {
                e.preventDefault();
            }
        };
        // document 레벨 캡처 단계에서 가로채기 — ContentBuilder 이벤트는 그대로 전달
        document.addEventListener('click', blockCanvasLinkNavigation, true);

        // ── media-video 영상 래퍼(검정 박스) 클릭 → 영상 URL 편집 모달 오픈 ─
        // pointer-events:none(globals.css)으로 iframe 클릭을 래퍼 div에 전달받음
        const handleMediaVideoAreaClick = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const container = document.querySelector('.container');
            if (!container?.contains(target)) return;
            // <a> 클릭은 제목 편집용 — 모달 오픈 제외
            if (target.closest('a')) return;
            const mediaBlock = target.closest<HTMLElement>('[data-component-id^="media-video"]');
            if (!mediaBlock) return;
            // iframe 또는 iframe의 직접 부모(래퍼 div)인 경우에만 오픈
            if (target.tagName === 'IFRAME' || target.querySelector(':scope > iframe')) {
                setMediaVideoBlock(mediaBlock);
            }
        };
        document.addEventListener('click', handleMediaVideoAreaClick, true);

        // ── 행 활성화 보조 핸들러 (mousedown 캡처 단계) ─────────────────────
        // ContentBuilder의 handleCellClick이 quick-banking 등 플러그인 내부 클릭에서
        // 동작하지 않을 때를 대비한 보완 핸들러입니다.
        //
        // 왜 mousedown + capture인가?
        //  • capture: true → 요소 레벨 핸들러보다 먼저 실행
        //  • mousedown → click 이벤트 체인보다 먼저 실행
        //  • composedPath() → SVG <path>/<use> 같은 내부 요소에서도
        //    .column 조상을 안정적으로 탐색 (closest()는 SVG 경계에서 실패 가능)
        const activateRowOnMouseDown = (e: MouseEvent) => {
            const target = e.target as HTMLElement;

            // composedPath()로 이벤트 경로 전체에서 요소 탐색
            // (SVG <path>/<use> 같은 내부 요소에서도 안정적으로 동작)
            const path = e.composedPath() as Element[];

            // ── .cell-add 버튼 클릭 처리 ─────────────────────────────────
            // .cell-add는 .is-col-tool(.is-tool) 안에 있어서 아래 is-tool early-return에
            // 걸려 ContentBuilder의 cellSelected()가 null을 반환하는 문제를 방지합니다.
            // mousedown(캡처) 단계에서 .cell-active를 먼저 설정해두면
            // 이후 click 단계의 ContentBuilder 핸들러가 정상 동작합니다.
            const isCellAdd = path.some((el) => el instanceof HTMLElement && el.classList.contains('cell-add'));
            if (isCellAdd) {
                const row = path.find((el) => el instanceof HTMLElement && el.classList.contains('row')) as
                    | HTMLElement
                    | undefined;
                const container = document.querySelector('.container');
                if (row && container?.contains(row)) {
                    const col = row.querySelector('.column') as HTMLElement | null;
                    if (col) {
                        document
                            .querySelectorAll('.row-active')
                            .forEach((r) => r.classList.remove('row-active', 'row-outline'));
                        document.querySelectorAll('.cell-active').forEach((c) => c.classList.remove('cell-active'));
                        document
                            .querySelectorAll('.builder-active')
                            .forEach((b) => b.classList.remove('builder-active'));
                        row.classList.add('row-active');
                        col.classList.add('cell-active');
                        row.parentElement?.classList.add('builder-active');
                        document.body.classList.add('content-edit');
                    }
                }
                return; // ContentBuilder의 .cell-add click 핸들러로 위임
            }

            // 그 외 도구 버튼 클릭(이동/삭제/더보기)은 ContentBuilder에 위임
            if (target.closest?.('.is-tool')) return;
            const col = path.find((el) => el instanceof HTMLElement && el.classList.contains('column')) as
                | HTMLElement
                | undefined;
            if (!col) return;

            // .container(.is-builder) 내부 클릭만 처리
            const container = document.querySelector('.container');
            if (!container?.contains(col)) return;

            const row = col.parentElement;
            if (!row?.classList.contains('row')) return;

            // handleCellClick이 이미 row-active를 설정했으면 패스
            if (row.classList.contains('row-active')) return;

            // 기존 활성 상태 제거 (ContentBuilder의 clearActiveCell()에 해당)
            document.querySelectorAll('.row-active').forEach((r) => r.classList.remove('row-active', 'row-outline'));
            document.querySelectorAll('.cell-active').forEach((c) => c.classList.remove('cell-active'));
            document.querySelectorAll('.builder-active').forEach((b) => b.classList.remove('builder-active'));

            // 행/열 활성화
            row.classList.add('row-active');
            col.classList.add('cell-active');
            row.parentElement?.classList.add('builder-active');
            document.body.classList.add('content-edit');

            // 다중 열 행인 경우 row-outline 추가 (is-row-tool 등 비열 자식 제외)
            const colCount = Array.from(row.children).filter((c) =>
                (c as HTMLElement).classList.contains('column'),
            ).length;
            if (colCount > 1) row.classList.add('row-outline');
        };
        document.addEventListener('mousedown', activateRowOnMouseDown, true);

        // ── .cell-add 클릭 → 새 행 추가로 리디렉션 ──────────────────────────
        // .cell-add(is-col-tool 내부)의 기본 동작은 같은 열에 옆으로 추가합니다.
        // 금융 컴포넌트 에디터에서는 항상 현재 행 아래에 새 행을 추가해야 하므로
        // 클릭을 캡처 단계에서 가로채 같은 행의 is-rowadd-tool 버튼을 대신 클릭합니다.
        const redirectCellAddToRowAdd = (e: MouseEvent) => {
            const path = e.composedPath() as Element[];
            const isCellAdd = path.some((el) => el instanceof HTMLElement && el.classList.contains('cell-add'));
            if (!isCellAdd) return;

            e.stopImmediatePropagation();
            e.preventDefault();

            const row = path.find((el) => el instanceof HTMLElement && el.classList.contains('row')) as
                | HTMLElement
                | undefined;
            if (!row) return;

            const rowAddBtn = row.querySelector('.is-rowadd-tool button') as HTMLElement | null;
            rowAddBtn?.click();
        };
        document.addEventListener('click', redirectCellAddToRowAdd, true);

        // ── quickadd 블록 예시 텍스트 한글화 ────────────────────────────────
        // quickadd 팝업에서 블록을 선택하면 ContentBuilder가 영문 예시 텍스트를 삽입합니다.
        // 캡처 단계에서 클릭을 감지해 플래그를 세운 뒤,
        // MutationObserver로 새 row 삽입을 감지해 한글로 대체합니다.
        const KO_TEXT: [string, string][] = [
            ['Headline Goes Here', '제목을 입력하세요'],
            [
                "It's easy to use, customizable, and user-friendly. A truly amazing features.",
                '사용하기 쉽고 커스터마이징이 가능합니다. 여기에 인용구를 입력하세요.',
            ],
            ['Heading 1 here', '제목 1을 입력하세요'],
            ['Heading 2 here', '제목 2를 입력하세요'],
            ['Heading 3 here', '제목 3을 입력하세요'],
            ['Heading 4 here', '제목 4를 입력하세요'],
            ['Lorem Ipsum is simply dummy text', '예시 텍스트'],
            ['Read More', '더 보기'],
            ['Get Started', '시작하기'],
            // ── HTML/JS 블록 (applyBehaviorOn 실행 후 스크립트가 교체한 텍스트) ──
            ['Hello World..!', '안녕하세요!'],
            [
                'This is a code block. You can edit this block using the source dialog.',
                'HTML/JS 블록입니다. 소스 편집기로 수정할 수 있습니다.',
            ],
            // ── 폼 블록 (FormViewer 기본 샘플 데이터) ──────────────────────
            ["Let's Build Something Cool!", '나만의 폼을 만들어보세요!'],
            ['Fuel your creativity with ease.', '쉽게 폼을 구성할 수 있습니다.'],
            ["Let's Go!", '제출하기'],
            ['Your Name:', '이름:'],
            ['Your Best Email:', '이메일:'],
            ['Enter your name', '이름을 입력하세요'],
            ['Enter your email', '이메일을 입력하세요'],
        ];
        const KO_LONG_LOREM = '여기에 내용을 입력하세요. 이 텍스트를 클릭하여 편집할 수 있습니다.';

        const replaceEnglishPlaceholders = (node: HTMLElement) => {
            // ① 일반 텍스트 노드 대체
            const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT, null);
            const textNodes: Text[] = [];
            let curr: Node | null;
            while ((curr = walker.nextNode())) textNodes.push(curr as Text);

            for (const t of textNodes) {
                const text = t.textContent ?? '';
                if (text.includes('Lorem Ipsum is simply dummy text of the printing')) {
                    t.textContent = KO_LONG_LOREM;
                    continue;
                }
                const match = KO_TEXT.find(([en]) => text.trim() === en);
                if (match) t.textContent = match[1];
            }

            // ② data-html 속성 대체 (HTML/JS 블록, 폼 블록)
            // ContentBuilder는 code/form 블록 콘텐츠를 URL인코딩해 data-html에 저장합니다.
            node.querySelectorAll<HTMLElement>('[data-html]').forEach((el) => {
                const encoded = el.getAttribute('data-html') ?? '';
                let decoded = decodeURIComponent(encoded);
                let changed = false;

                // HTML/JS 블록 예시 텍스트
                if (decoded.includes('Lorem ipsum')) {
                    decoded = decoded.replace(/<h1([^>]*)>Lorem ipsum<\/h1>/, '<h1$1>안녕하세요</h1>');
                    decoded = decoded.replace(
                        'This is a code block. You can edit this block using the source dialog.',
                        'HTML/JS 블록입니다. 소스 편집기로 수정할 수 있습니다.',
                    );
                    decoded = decoded.replace('Hello World..!', '안녕하세요!');
                    changed = true;
                }

                // 폼 블록: JSON 파싱 후 기본 텍스트 한글화
                // FormViewer가 json.title / json.submitText를 직접 innerText로 설정하므로
                // data-html 속성 단계에서 JSON 필드를 교체해야 합니다.
                if (decoded.trim().startsWith('{') && decoded.includes('"title"')) {
                    try {
                        const formJson = JSON.parse(decoded) as Record<string, unknown>;
                        let formChanged = false;
                        const FORM_KO: [string, string][] = [
                            ["Let's Build Something Cool!", '나만의 폼을 만들어보세요!'],
                            ['Fuel your creativity with ease.', '쉽게 폼을 구성할 수 있습니다.'],
                            ["Let's Go!", '제출하기'],
                            ['Your Form Title Here', '폼 제목을 입력하세요'],
                            ['Your Description Here', '폼 설명을 입력하세요'],
                        ];
                        for (const [en, ko] of FORM_KO) {
                            if (formJson['title'] === en) {
                                formJson['title'] = ko;
                                formChanged = true;
                            }
                            if (formJson['description'] === en) {
                                formJson['description'] = ko;
                                formChanged = true;
                            }
                            if (formJson['submitText'] === en) {
                                formJson['submitText'] = ko;
                                formChanged = true;
                            }
                        }
                        if (formChanged) {
                            decoded = JSON.stringify(formJson);
                            changed = true;
                        }
                    } catch {
                        /* JSON 파싱 실패 시 무시 */
                    }
                }

                if (changed) el.setAttribute('data-html', encodeURIComponent(decoded));
            });
        };

        let pendingKorean = false;
        const markKorean = (e: MouseEvent) => {
            const path = e.composedPath() as Element[];
            const inQuickadd = path.some((el) => el instanceof HTMLElement && el.classList.contains('quickadd'));
            if (!inQuickadd) return;
            const isAddBtn = path.some((el) => el instanceof HTMLElement && /\badd-\w/.test(el.className));
            if (isAddBtn) pendingKorean = true;
        };
        document.addEventListener('click', markKorean, true);

        const koObserver = new MutationObserver((mutations) => {
            if (!pendingKorean) return;
            for (const m of mutations) {
                m.addedNodes.forEach((n) => {
                    if (n instanceof HTMLElement && n.classList.contains('row')) {
                        replaceEnglishPlaceholders(n);
                        // 폼/코드 블록은 FormViewer·스크립트가 비동기로 렌더링하므로
                        // 100ms·500ms 후 재시도해 텍스트 노드를 교체합니다.
                        const rowRef = n;
                        setTimeout(() => replaceEnglishPlaceholders(rowRef), 100);
                        setTimeout(() => replaceEnglishPlaceholders(rowRef), 500);
                        pendingKorean = false;
                    }
                });
            }
        });
        const containerEl = document.querySelector('.container');
        if (containerEl) koObserver.observe(containerEl, { childList: true });

        // ── site-footer select 클릭 감지 → 드롭다운 편집 패널 오픈 ──────────────
        // <select>는 ContentBuilder가 #divLinkTool을 열지 않으므로,
        // 캡처 단계에서 직접 클릭을 감지하여 SiteFooterSelectEditor를 띄웁니다.
        const handleSiteFooterSelectClick = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const select = target.closest<HTMLSelectElement>('[data-component-id^="site-footer"] select');
            if (!select) return;
            e.preventDefault();
            e.stopPropagation();
            const block = select.closest<HTMLElement>('[data-component-id^="site-footer"]');
            if (block) setSiteFooterBlock(block);
        };
        document.addEventListener('click', handleSiteFooterSelectClick, true);

        // ── data-max-chars 글자 수 제한 (document 레벨 위임) ─────────────────
        // ContentBuilder는 span 자체가 아닌 상위 요소를 contenteditable로 만드므로
        // 선택 영역(Selection)의 공통 조상을 기준으로 data-max-chars 요소를 탐색
        const getMaxCharsEl = (): HTMLElement | null => {
            const sel = window.getSelection();
            if (!sel || sel.rangeCount === 0) return null;
            let node: Node | null = sel.getRangeAt(0).commonAncestorContainer;
            if (node.nodeType === Node.TEXT_NODE) node = node.parentNode;
            return (node as Element)?.closest?.('[data-max-chars]') as HTMLElement | null;
        };

        const handleMaxCharsKeydown = (e: KeyboardEvent) => {
            // 제어 키·단축키 허용
            if (e.ctrlKey || e.metaKey || e.altKey) return;
            if (e.key.length !== 1) return; // 특수 키(Backspace, Arrow 등) 제외
            const maxEl = getMaxCharsEl();
            if (!maxEl) return;
            const max = parseInt(maxEl.dataset.maxChars ?? '20', 10);
            const sel = window.getSelection();
            const selectedLen = sel?.toString().length ?? 0;
            if ((maxEl.textContent?.length ?? 0) - selectedLen >= max) {
                e.preventDefault();
            }
        };

        const handleMaxCharsPaste = (e: ClipboardEvent) => {
            const maxEl = getMaxCharsEl();
            if (!maxEl) return;
            e.preventDefault();
            const max = parseInt(maxEl.dataset.maxChars ?? '20', 10);
            const sel = window.getSelection();
            const selectedLen = sel?.toString().length ?? 0;
            const remaining = max - ((maxEl.textContent?.length ?? 0) - selectedLen);
            if (remaining <= 0) return;
            const text = (e.clipboardData?.getData('text') ?? '').slice(0, remaining);
            document.execCommand('insertText', false, text);
        };

        document.addEventListener('keydown', handleMaxCharsKeydown, true);
        document.addEventListener('paste', handleMaxCharsPaste, true);

        // Load content from the server (AbortController로 Strict Mode 중복 fetch 방지)
        const loadController = new AbortController();
        const pageLoadStart = performance.now();
        fetch(nextApi('/api/builder/load'), {
            method: 'POST',
            body: JSON.stringify({ bank }),
            headers: { 'Content-Type': 'application/json' },
            signal: loadController.signal,
        })
            .then(async (response) => {
                const jsonStart = performance.now();
                const json = await response.json();
                return {
                    json,
                    networkMs: roundMs(jsonStart - pageLoadStart),
                    jsonMs: roundMs(performance.now() - jsonStart),
                };
            })
            .then(({ json: response, networkMs, jsonMs }) => {
                if (loadController.signal.aborted) return;
                if (response.html && builderRef.current) {
                    const loadHtmlStart = performance.now();
                    builderRef.current.loadHtml(response.html);
                    logEditPerf('page-loadHtml', {
                        loadHtmlMs: roundMs(performance.now() - loadHtmlStart),
                        htmlBytes: response._timing?.htmlBytes,
                    });
                }
                // 로드 응답에서 탭 정보 등록 — 최근 접근 순(왼쪽), 최대 10개
                if (response.pageMissing) {
                    removeCurrentTabAndRedirect('삭제되었거나 존재하지 않는 페이지입니다.');
                    return;
                }
                if (response.pageName) {
                    setTabs((prev) => {
                        const filtered = prev.filter((t) => t.id !== bank);
                        const updated = [
                            {
                                id: bank,
                                label: response.pageName,
                                viewMode: normalizeViewMode(response.viewMode),
                            },
                            ...filtered,
                        ];
                        return updated.slice(0, 10);
                    });
                }
                // 플러그인 CSS·JS 로드 및 mount() 실행 + ContentBuilder 핸들러 재연결
                setTimeout(async () => {
                    const reinitStart = performance.now();
                    await runtimeRef.current?.reinitialize();
                    builderRef.current?.applyBehavior();
                    patchPmIconWrap();
                    patchMaxChars();
                    setContainerOpacity(1);
                    // 초기 블록 목록 파싱
                    const html = builderRef.current?.html() ?? '';
                    const blocks = parseBuilderBlocks(html, financeComponentsMapRef.current);
                    setCanvasBlocks(blocks);
                    logEditPerf('page-ready', {
                        totalMs: roundMs(performance.now() - pageLoadStart),
                        networkMs,
                        jsonMs,
                        reinitializeMs: roundMs(performance.now() - reinitStart),
                        server: response._timing,
                        blockCount: blocks.length,
                    });
                }, 300);
            })
            .catch((error) => {
                if (loadController.signal.aborted) return;
                console.error('로드 오류:', error);
                setContainerOpacity(1);
            });

        // Cleanup
        return () => {
            loadController.abort();
            rteObserver.disconnect();
            modalObserver.disconnect();
            colToolObserver.disconnect();
            slideToolObserver.disconnect();
            document.removeEventListener('click', blockCanvasLinkNavigation, true);
            document.removeEventListener('click', handleMediaVideoAreaClick, true);
            document.removeEventListener('click', redirectCellAddToRowAdd, true);
            document.removeEventListener('click', markKorean, true);
            document.removeEventListener('click', handleSiteFooterSelectClick, true);
            koObserver.disconnect();
            document.removeEventListener('mousedown', activateRowOnMouseDown, true);
            document.removeEventListener('keydown', handleMaxCharsKeydown, true);
            document.removeEventListener('paste', handleMaxCharsPaste, true);
            if (reinitTimer) clearTimeout(reinitTimer);
            builderRef.current?.destroy();
            builderRef.current = null;
            runtimeRef.current?.destroy();
            runtimeRef.current = null;
            window.builderRuntime = undefined;
            window.builderReinit = undefined;
            window.__spwEditor = undefined;
        };
    }, []);

    // ── ContentBuilder 기본 "이미지 변경" 버튼 인터셉트 ─────────────
    // ContentBuilder의 `#fileEmbedImage` (이미지 블록·헤더 이미지 변경) 는
    // filePicker/imageSelect 옵션을 경유하지 않고 OS 파일 선택창을 직접 띄움.
    // → 클릭을 캡처 단계에서 가로채서 승인 이미지 브라우저(/files) 모달로 우회
    const imageReplaceTargetRef = useRef<HTMLImageElement | null>(null);

    // 이미지 picker 모달이 열릴 때 초기 크기·중앙 배치 + 8방향 리사이즈 핸들 부착
    useEffect(() => {
        if (!imagePickerOpen) return;
        const box = imagePickerBoxRef.current;
        if (!box) return;

        centerInitialBox(box, { width: 1280, height: 900 });
        const detach = attachResizeHandles(box, { minWidth: 480, minHeight: 360 });
        return detach;
    }, [imagePickerOpen]);

    // picker iframe 참조 — message 리스너에서 event.source 비교로 "이 모달에서 온 메시지"인지 식별
    // (DOM 존재 여부 판별은 리스너 실행 순서·DOM 조작 타이밍 레이스에 취약 → event.source 비교가 안전)
    const imagePickerIframeRef = useRef<HTMLIFrameElement>(null);

    useEffect(() => {
        const handleFileInputClick = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            if (!(target instanceof HTMLInputElement)) return;
            if (target.type !== 'file') return;
            if (target.id !== 'fileEmbedImage') return;

            e.preventDefault();
            e.stopImmediatePropagation();

            // 현재 편집 중인 이미지 참조 저장 — postMessage 응답에서 src 교체에 사용
            // eslint-disable-next-line @typescript-eslint/no-explicit-any -- ContentBuilder 내부 속성
            const activeImg = (builderRef.current as any)?.activeImage as HTMLImageElement | null;
            imageReplaceTargetRef.current = activeImg ?? null;

            // iframe 모달로 /files 오픈 (postMessage 타겟은 window.parent 경로 사용)
            setImagePickerOpen(true);
        };

        document.addEventListener('click', handleFileInputClick, true);
        return () => document.removeEventListener('click', handleFileInputClick, true);
    }, []);

    // 파일 피커(/files)에서 이미지 선택 시 에디터에 삽입 또는 교체
    // - ASSET_SELECTED: 단건 삽입 (하위 호환)
    // - ASSETS_SELECTED: 다건 — 완료 버튼 경유
    //   · 교체 모드 (imageReplaceTargetRef 가 있을 때): 첫 URL 로 activeImage src 교체
    //   · 삽입 모드: selectAsset 루프 호출 (ContentBuilder 내장 filePicker/imageSelect 경로)
    // - PICKER_CLOSE: iframe 모달 내부 닫기 버튼에서 부모에게 모달 종료 요청
    //
    // 메시지 출처 식별은 event.source 비교로 처리한다. 이전에는 cms-file-picker 유틸 모달의
    // DOM 존재 여부(getElementById)로 스킵 판단을 했는데, 이는 리스너 실행 순서·DOM 조작
    // 타이밍 레이스에 취약해 안전하지 않다. 현재 구현은:
    //   1) event.source 가 cms-file-picker 유틸 iframe 이면 즉시 스킵 (유틸 자체 리스너가 처리)
    //   2) isOwnPicker 플래그(이 EditClient 의 React 모달 iframe 에서 온 메시지인지)로
    //      모달 상태 변경(setImagePickerOpen)·PICKER_CLOSE 처리를 범위 한정
    //   3) ASSETS_SELECTED 의 selectAsset 삽입 로직은 ContentBuilder 내장 filePicker 에서
    //      온 경우에도 동작해야 하므로 소스를 더 좁히지 않는다
    useEffect(() => {
        const handleMessage = (event: MessageEvent) => {
            // cms-file-picker 유틸 모달의 iframe 에서 온 메시지는 유틸이 단독 처리 — 여기서는 무시
            const utilModalEl = document.getElementById('spw-cms-file-picker-modal');
            const utilIframe = utilModalEl?.querySelector('iframe') as HTMLIFrameElement | null;
            if (utilIframe && event.source === utilIframe.contentWindow) return;

            // 이 모달(EditClient) 의 iframe 에서 온 메시지인지 — setImagePickerOpen·PICKER_CLOSE 범위 한정용
            const isOwnPicker = event.source === imagePickerIframeRef.current?.contentWindow;

            switch (event.data.type) {
                case 'ASSET_SELECTED':
                    builderRef.current?.selectAsset(event.data.url);
                    if (isOwnPicker) setImagePickerOpen(false);
                    window.focus();
                    break;
                case 'ASSETS_SELECTED': {
                    const urls: string[] = Array.isArray(event.data.urls) ? event.data.urls : [];
                    const replaceTarget = imageReplaceTargetRef.current;

                    if (replaceTarget && urls[0]) {
                        // 이미지 교체 모드 — #fileEmbedImage 인터셉트로 연 EditClient 모달 경로
                        replaceTarget.setAttribute('src', urls[0]);
                        imageReplaceTargetRef.current = null;
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any -- ContentBuilder 내부 onChange
                        const onChange = (builderRef.current as any)?.opts?.onChange;
                        if (typeof onChange === 'function') onChange();
                    } else {
                        // 일반 삽입 모드 — ContentBuilder 내장 filePicker/imageSelect 경유 또는 activeImg 부재 케이스
                        urls.forEach((url) => builderRef.current?.selectAsset(url));
                    }
                    if (isOwnPicker) setImagePickerOpen(false);
                    window.focus();
                    break;
                }
                case 'PICKER_CLOSE':
                    // iframe 내부 AssetBrowser 의 X 버튼 → 이 모달에 한해서만 종료 처리
                    if (!isOwnPicker) break;
                    imageReplaceTargetRef.current = null;
                    setImagePickerOpen(false);
                    break;
                default:
                    break;
            }
        };
        window.addEventListener('message', handleMessage);
        return () => {
            window.removeEventListener('message', handleMessage);
        };
    }, []);

    // popup-banner 편집 버튼 클릭 이벤트 수신 (index.js → CustomEvent → 패널 오픈)
    useEffect(() => {
        const handleEditEvent = (e: Event) => {
            const detail = (e as CustomEvent<{ element: HTMLElement; onChange?: () => void }>).detail;
            if (detail?.element) {
                setPopupBannerBlock(detail.element);
                // useState setter에 함수를 직접 넘기면 React가 state updater로 해석하므로
                // () => fn 형태로 래핑하여 함수 자체를 state 값으로 저장
                setPopupBannerOnChange(() => detail.onChange ?? null);
            }
        };
        document.addEventListener('spw:popup-banner:edit', handleEditEvent);
        return () => document.removeEventListener('spw:popup-banner:edit', handleEditEvent);
    }, []);

    // ── 세션에서 탭 목록 복구 ──────────────────────────────────────────
    useEffect(() => {
        try {
            const stored = sessionStorage.getItem(SESSION_TABS_KEY);
            if (stored) {
                const parsed: TabData[] = JSON.parse(stored);
                setTabs(
                    parsed.map((tab) => ({
                        ...tab,
                        viewMode: normalizeViewMode(tab.viewMode),
                    })),
                );
            }
        } catch (err: unknown) {
            console.warn('탭 목록 세션 로드 실패:', err);
        } finally {
            setTabsLoading(false);
        }
    }, [SESSION_TABS_KEY]);

    // ── 탭 변경 시 세션에 저장 ─────────────────────────────────────────
    useEffect(() => {
        if (tabsLoading) return; // 초기 로딩 중에는 저장하지 않음
        try {
            sessionStorage.setItem(SESSION_TABS_KEY, JSON.stringify(tabs));
        } catch (err: unknown) {
            console.warn('탭 목록 세션 저장 실패:', err);
        }
    }, [tabs, tabsLoading, SESSION_TABS_KEY]);

    // ── 기본 블록 DB 로드 (viewMode 변경 시 재조회) ─────────────────────
    useEffect(() => {
        let cancelled = false;
        const componentLoadStart = performance.now();
        setBasicBlocksLoading(true);
        setBasicBlocksError(null);
        fetch(nextApi(`/api/components?type=basic&viewMode=${viewMode}`))
            .then(async (res) => {
                const jsonStart = performance.now();
                const data = await res.json();
                return {
                    data,
                    networkMs: roundMs(jsonStart - componentLoadStart),
                    jsonMs: roundMs(performance.now() - jsonStart),
                };
            })
            .then(({ data, networkMs, jsonMs }) => {
                if (!cancelled && data.ok) {
                    const mapStart = performance.now();
                    const assetPrefix = nextApi('/assets');
                    const blocks: BasicBlock[] = data.components.map(
                        (c: { id: string; label?: string; preview?: string; html: string; viewMode: string }) => ({
                            id: c.id,
                            thumbnail: c.preview ?? '',
                            html: c.html
                                .replace(/src="assets\//g, `src="${assetPrefix}/`)
                                .replace(/url\(&quot;assets\//g, `url(&quot;${assetPrefix}/`)
                                .replace(/url\('assets\//g, `url('${assetPrefix}/`)
                                .replace(/url\(assets\//g, `url(${assetPrefix}/`),
                            viewMode: c.viewMode as BasicBlock['viewMode'],
                            label: c.label || undefined,
                        }),
                    );
                    setBasicBlocks(blocks);
                    logEditPerf('components-basic', {
                        totalMs: roundMs(performance.now() - componentLoadStart),
                        networkMs,
                        jsonMs,
                        mapMs: roundMs(performance.now() - mapStart),
                        count: blocks.length,
                        server: data._timing,
                    });
                    setBasicBlocksLoading(false);
                } else if (!cancelled) {
                    setBasicBlocksError(data.error ?? '기본 블록을 불러오지 못했습니다.');
                    setBasicBlocksLoading(false);
                }
            })
            .catch((err) => {
                console.error('기본 블록 로드 오류:', err);
                if (!cancelled) {
                    setBasicBlocksError('기본 블록을 불러오지 못했습니다.');
                    setBasicBlocksLoading(false);
                }
            });
        return () => {
            cancelled = true;
        };
    }, [viewMode]);

    // ── 금융 컴포넌트 API 로드 (viewMode 변경 시 재요청) ────────────────
    useEffect(() => {
        let cancelled = false;
        const componentLoadStart = performance.now();
        setFinanceComponentsLoading(true);
        setFinanceComponentsError(null);
        fetch(nextApi(`/api/components?type=finance&viewMode=${viewMode}`))
            .then(async (res) => {
                const jsonStart = performance.now();
                const data = await res.json();
                return {
                    data,
                    networkMs: roundMs(jsonStart - componentLoadStart),
                    jsonMs: roundMs(performance.now() - jsonStart),
                };
            })
            .then(({ data, networkMs, jsonMs }) => {
                if (!cancelled && data.ok) {
                    setFinanceComponents(data.components);
                    logEditPerf('components-finance', {
                        totalMs: roundMs(performance.now() - componentLoadStart),
                        networkMs,
                        jsonMs,
                        count: data.components.length,
                        server: data._timing,
                    });
                    setFinanceComponentsLoading(false);
                } else if (!cancelled) {
                    setFinanceComponentsError(data.error ?? '금융 컴포넌트를 불러오지 못했습니다.');
                    setFinanceComponentsLoading(false);
                }
            })
            .catch((err) => {
                console.error('금융 컴포넌트 로드 오류:', err);
                if (!cancelled) {
                    setFinanceComponentsError('금융 컴포넌트를 불러오지 못했습니다.');
                    setFinanceComponentsLoading(false);
                }
            });
        return () => {
            cancelled = true;
        };
    }, [viewMode]);

    // ── 컴포넌트 패널 → 캔버스 삽입 ──────────────────────────────────────
    /**
     * 패널에서 선택한 컴포넌트 HTML을 캔버스의 targetIdx 위치에 삽입합니다.
     * insertIdx가 없으면 끝에 추가합니다.
     * 모든 삽입은 loadHtml()로 통일합니다.
     * (addSnippet은 내부 스니펫 레지스트리를 참조해 의도치 않은 컴포넌트를 함께 삽입하는 부작용이 있음)
     */
    const handleInsertComponent = useCallback(
        (html: string, insertIdx?: number) => {
            const builder = builderRef.current;
            if (!builder) return;

            // 브랜드 테마 색상 치환 (금융 컴포넌트 삽입 시)
            const theme = brandThemeRef.current;
            // 금융 컴포넌트 inline script는 window.builderRuntime / .is-builder 가드를 포함하므로
            // 에디터에서 실행되어도 즉시 return — 스크립트를 보존해야 배포 HTML에서도 동작함
            const themedHtml = theme ? applyBrandTheme(html, theme) : html;

            // canvasBlocksRef.current 대신 builder.html()로 현재 DOM 상태를 직접 읽음
            // — ContentBuilder 자체 삭제/이동 후 React state가 동기화되지 않은 경우에도
            //   항상 실제 DOM 기준의 최신 블록 목록을 사용합니다.
            const liveBlocks = parseBuilderBlocks(builder.html() ?? '', financeComponentsMapRef.current);
            // 금융 컴포넌트(data-spw-block)는 컬럼 패딩 없이 캔버스 전체 너비를 채워야 함
            // 루트 요소의 속성만 확인 — 텍스트 내용에 문자열이 포함되어도 오작동하지 않도록 파싱
            const isSpwBlock = (() => {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = themedHtml.trim();
                const root = tempDiv.firstElementChild;
                return root?.hasAttribute('data-spw-block') ?? false;
            })();
            const shouldShowMyDataAssetToast = (() => {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = themedHtml.trim();
                const root = tempDiv.firstElementChild as HTMLElement | null;
                const componentId = root?.getAttribute('data-component-id') ?? '';
                return componentId.startsWith('mydata-asset');
            })();
            const colClass = isSpwBlock ? 'column spw-finance-col' : 'column';
            const wrappedHtml = `<div class="row"><div class="${colClass}">\n${themedHtml}\n</div></div>`;
            const blockHtmls = liveBlocks.map((b) => b.outerHtml);

            // insertIdx가 유효하면 해당 위치에, 아니면 끝에 추가
            // addSnippet 대신 loadHtml로 통일 — addSnippet은 내부 스니펫 레지스트리를
            // 참조해 의도치 않은 컴포넌트를 함께 삽입하는 부작용이 있음
            const targetIdx =
                insertIdx !== undefined && insertIdx >= 0 ? Math.min(insertIdx, blockHtmls.length) : blockHtmls.length;

            blockHtmls.splice(targetIdx, 0, wrappedHtml);
            builder.loadHtml(blockHtmls.join('\n'));
            if (shouldShowMyDataAssetToast) {
                showInfoToast(
                    '캔버스 내 숫자 직접 수정은 표시값만 바뀝니다. 비율과 차트까지 반영하려면 편집 모달을 사용해주세요.',
                );
            }

            // 플러그인 재초기화 + ContentBuilder 편집 핸들러 재연결 + 블록 목록 갱신
            setTimeout(async () => {
                await runtimeRef.current?.reinitialize();
                builderRef.current?.applyBehavior();
                patchPmIconWrap();
                patchMaxChars();
                const newHtml = builderRef.current?.html() ?? '';
                setCanvasBlocks(parseBuilderBlocks(newHtml, financeComponentsMapRef.current));
            }, 300);
        },
        [patchPmIconWrap, patchMaxChars, showInfoToast],
    );

    // ── 순서 탭 블록 클릭 → 캔버스 활성화 ──────────────────────────────
    /**
     * 순서 패널에서 블록을 클릭하면 캔버스의 해당 row를 활성화합니다.
     * ContentBuilder의 handleCellClick은 column 클릭 이벤트로 동작하므로
     * 해당 row의 .column 요소를 프로그래매틱하게 클릭합니다.
     */
    const handleActivate = useCallback((idx: number) => {
        const container = document.querySelector('.container');
        if (!container) return;

        const rows = Array.from(container.children).filter((c) =>
            (c as HTMLElement).classList.contains('row'),
        ) as HTMLElement[];

        const row = rows[idx];
        if (!row) return;

        const col = row.querySelector('.column') as HTMLElement | null;
        col?.click();
        row.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, []);

    // ── 블록 삭제 ────────────────────────────────────────────────────────
    /**
     * 순서 패널의 삭제 버튼으로 특정 인덱스의 블록을 제거합니다.
     */
    const handleDelete = useCallback(
        (idx: number) => {
            const builder = builderRef.current;
            if (!builder) return;

            const liveBlocks = parseBuilderBlocks(builder.html() ?? '', financeComponentsMapRef.current);
            const next = liveBlocks.filter((_, i) => i !== idx);
            const newHtml = next.map((b) => b.outerHtml).join('\n');
            builder.loadHtml(newHtml);

            setTimeout(async () => {
                await runtimeRef.current?.reinitialize();
                builderRef.current?.applyBehavior();
                patchPmIconWrap();
                patchMaxChars();
                const updatedHtml = builderRef.current?.html() ?? '';
                setCanvasBlocks(parseBuilderBlocks(updatedHtml, financeComponentsMapRef.current));
            }, 300);
        },
        [patchPmIconWrap, patchMaxChars],
    );

    // ── 전체 블록 삭제 ──────────────────────────────────────────────────
    const handleDeleteAllBlocks = useCallback(() => {
        const builder = builderRef.current;
        if (!builder) return;
        builder.loadHtml('');
        setCanvasBlocks([]);
    }, []);

    // ── 블록 순서 변경 ──────────────────────────────────────────────────
    /**
     * 순서 패널의 ▲▼ 버튼으로 블록을 이동합니다.
     * canvasBlocksRef 대신 builder.html()로 항상 최신 DOM 상태를 읽습니다.
     */
    const handleMoveBlock = useCallback(
        (from: number, to: number) => {
            const builder = builderRef.current;
            if (!builder) return;

            const liveBlocks = parseBuilderBlocks(builder.html() ?? '', financeComponentsMapRef.current);
            if (from < 0 || from >= liveBlocks.length || to < 0 || to > liveBlocks.length) return;
            if (from === to || from === to - 1) return; // 위치 변동 없음

            // "to"는 원본 배열 기준 "이 인덱스 앞에 삽입" 의미
            // from 제거 후 배열이 짧아지므로 from < to일 때 to를 1 감소
            const next = liveBlocks.filter((_, i) => i !== from);
            const insertAt = from < to ? to - 1 : to;
            next.splice(insertAt, 0, liveBlocks[from]);
            builder.loadHtml(next.map((b) => b.outerHtml).join('\n'));

            // 플러그인 재초기화 + ContentBuilder 편집 핸들러 재연결 + 블록 목록 갱신
            setTimeout(async () => {
                await runtimeRef.current?.reinitialize();
                builderRef.current?.applyBehavior();
                patchPmIconWrap();
                patchMaxChars();
                const updatedHtml = builderRef.current?.html() ?? '';
                setCanvasBlocks(parseBuilderBlocks(updatedHtml, financeComponentsMapRef.current));
            }, 300);
        },
        [patchPmIconWrap, patchMaxChars],
    );

    // ── 오버레이 드롭 핸들러 ──────────────────────────────────────────────
    // 오버레이가 ContentBuilder DOM 위에 직접 렌더링되므로
    // ContentBuilder 내부의 stopPropagation과 무관하게 이벤트를 수신합니다.

    function handleOverlayDragOver(e: React.DragEvent) {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'copy';
        setIsDropOver(true);

        // ── 마우스 Y 위치로 삽입 인덱스 계산 ──────────────────────────
        const container = document.querySelector('.container') as HTMLElement | null;
        if (!container) {
            dropInsertIdxRef.current = -1;
            setDropLineY(null);
            return;
        }

        // ':scope > .row'로 직계 자식 .row만 선택 (ContentBuilder 내부 UI 요소 제외)
        // rows가 없으면 container 직계 자식 전체를 폴백으로 사용
        let rows = Array.from(container.querySelectorAll<HTMLElement>(':scope > .row'));
        if (rows.length === 0) {
            rows = Array.from(container.children).filter(
                (c) => !(c as HTMLElement).classList.contains('is-tool'),
            ) as HTMLElement[];
        }

        const mouseY = e.clientY;
        let insertIdx = rows.length; // 기본값: 끝

        for (let i = 0; i < rows.length; i++) {
            const rect = rows[i].getBoundingClientRect();
            const midY = rect.top + rect.height / 2;
            if (mouseY < midY) {
                insertIdx = i;
                break;
            }
        }

        dropInsertIdxRef.current = insertIdx;

        // 삽입 선 Y 위치 계산 (viewport 기준)
        if (rows.length === 0) {
            setDropLineY(null);
        } else if (insertIdx === 0) {
            setDropLineY(rows[0].getBoundingClientRect().top);
        } else if (insertIdx >= rows.length) {
            const lastRect = rows[rows.length - 1].getBoundingClientRect();
            setDropLineY(lastRect.bottom);
        } else {
            const prevBottom = rows[insertIdx - 1].getBoundingClientRect().bottom;
            const nextTop = rows[insertIdx].getBoundingClientRect().top;
            setDropLineY((prevBottom + nextTop) / 2);
        }
    }

    function handleOverlayDragLeave(e: React.DragEvent) {
        // 오버레이 자식 요소(메시지 배지)로 이동할 때 false-positive 방지
        if ((e.currentTarget as HTMLElement).contains(e.relatedTarget as Node)) return;
        setIsDropOver(false);
        setDropLineY(null);
    }

    function handleOverlayDrop(e: React.DragEvent) {
        e.preventDefault();
        const insertIdx = dropInsertIdxRef.current;
        setIsDropOver(false);
        setDropLineY(null);
        isDraggingRef.current = false;
        setIsDraggingComponent(false);

        const html = e.dataTransfer.getData('text/plain');
        if (html) {
            // 오버레이를 먼저 unmount한 뒤 삽입
            setTimeout(() => handleInsertComponent(html, insertIdx >= 0 ? insertIdx : undefined), 0);
        }
    }

    // ── 탭 추가 ──────────────────────────────────────────────────────────
    // ── 탭 닫기 ──────────────────────────────────────────────────────────
    // DB 삭제 없이 세션 탭 목록에서만 제거. 실제 삭제는 대시보드에서 수행.
    function handleCloseTab() {
        const currentLabel = tabs.find((t) => t.id === bank)?.label ?? bank;
        if (!confirm(`'${currentLabel}' 탭을 닫으시겠습니까?\n페이지 내용은 대시보드에서 확인할 수 있습니다.`)) return;

        const remaining = tabs.filter((t) => t.id !== bank);
        setTabs(remaining);

        if (remaining.length > 0) {
            // 남은 탭 중 첫 번째로 이동
            window.location.href = nextApi(`/edit?bank=${remaining[0].id}`);
        } else {
            // 탭이 없으면 대시보드로 이동
            window.location.href = nextApi('/dashboard');
        }
    }

    // ── 페이지 삭제 ──────────────────────────────────────────────────────
    async function handleDeletePage() {
        const currentLabel = tabs.find((t) => t.id === bank)?.label ?? bank;
        if (!confirm(`'${currentLabel}' 페이지를 삭제하시겠습니까?\n저장된 내용도 함께 삭제됩니다.`)) return;

        try {
            if (!canWrite) return;
            const res = await fetch(nextApi(`/api/builder/pages?pageId=${encodeURIComponent(bank)}`), {
                method: 'DELETE',
            });
            const data = await res.json();
            if (!data.ok) {
                alert('페이지 삭제에 실패했습니다.');
                return;
            }
        } catch (err: unknown) {
            console.error('페이지 삭제 실패:', err);
            alert('페이지 삭제 중 오류가 발생했습니다.');
            return;
        }

        // 삭제 후 탭 제거 후 이동
        const remaining = tabs.filter((t) => t.id !== bank);
        setTabs(remaining);
        window.location.href = remaining.length > 0 ? nextApi(`/edit?bank=${remaining[0].id}`) : nextApi('/dashboard');
    }

    // ── 저장 / 미리보기 / HTML 보기 ──────────────────────────────────────
    const save = async () => {
        if (!builderRef.current) return;
        const builder = builderRef.current;
        const html = builder.html();
        const pageName = tabs.find((tab) => tab.id === bank)?.label;

        if (!canWrite) return;
        const response = await fetch(nextApi('/api/builder/save'), {
            method: 'POST',
            body: JSON.stringify({ html, bank, pageName, thumbnail: '' }),
            headers: { 'Content-Type': 'application/json' },
        });
        const result = await response.json();
        if (result.error) {
            if (result.errorCode === 'PAGE_NOT_FOUND') {
                removeCurrentTabAndRedirect('삭제되었거나 존재하지 않는 페이지입니다.');
                return;
            }
            throw new Error(result.error);
        }
    };

    async function handleSave() {
        const html = builderRef.current?.html() ?? '';
        if (!html.trim()) {
            alert('저장할 콘텐츠가 없습니다.');
            return;
        }
        try {
            await save();
            alert('저장이 완료되었습니다.');
        } catch (err: unknown) {
            console.error('저장 실패:', err);
            alert('저장에 실패했습니다.\n다시 시도해 주세요.');
        }
    }

    async function handlePreview() {
        const html = builderRef.current?.html() ?? '';
        if (!html.trim()) {
            alert('저장할 콘텐츠가 없습니다.\n에디터에 내용을 추가한 후 미리보기를 사용해 주세요.');
            return;
        }
        try {
            await save();
            window.open(nextApi(`/view?bank=${bank}&preview=1`), '_blank');
        } catch (err: unknown) {
            console.error('저장 실패:', err);
            alert('저장에 실패했습니다.\n다시 시도해 주세요.');
        }
    }

    function handleViewHtml() {
        builderRef.current?.viewHtml();
    }

    return (
        <>
            {/* ── 상단 네비바 ── */}
            <nav
                style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    height: '52px',
                    zIndex: 100,
                    background: '#ffffff',
                    borderBottom: '1px solid #e5e7eb',
                    display: 'flex',
                    alignItems: 'center',
                    padding: '0 16px',
                    gap: '4px',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
                }}
            >
                {/* 대시보드 이동 버튼 */}
                <a
                    href={nextApi('/dashboard')}
                    title="대시보드로 돌아가기"
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '5px',
                        textDecoration: 'none',
                        color: '#0046A4',
                        fontSize: '13px',
                        fontWeight: 600,
                        whiteSpace: 'nowrap',
                        padding: '5px 12px 5px 8px',
                        borderRadius: '6px',
                        border: '1px solid #c7d8f4',
                        background: '#EBF4FF',
                        flexShrink: 0,
                    }}
                    className="nav-btn"
                >
                    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                        <path
                            d="M9 2L4 7l5 5"
                            stroke="currentColor"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                    </svg>
                    대시보드
                </a>

                {/* 버튼 / 탭 구분선 */}
                <div style={{ width: '1px', height: '20px', background: '#e5e7eb', flexShrink: 0, margin: '0 6px' }} />

                {/* 페이지 탭 */}
                <div style={{ display: 'flex', gap: '2px', flex: 1, overflowX: 'auto', alignItems: 'center' }}>
                    {tabsLoading ? (
                        <span style={{ fontSize: '12px', color: '#9ca3af', padding: '5px 14px' }}>로딩 중...</span>
                    ) : (
                        tabs.map((b) => (
                            <a
                                key={b.id}
                                href={nextApi(`/edit?bank=${b.id}`)}
                                style={{
                                    padding: '5px 14px',
                                    borderRadius: '6px',
                                    background: bank === b.id ? '#0046A4' : 'transparent',
                                    color: bank === b.id ? '#ffffff' : '#374151',
                                    textDecoration: 'none',
                                    fontSize: '13px',
                                    fontWeight: bank === b.id ? 600 : 400,
                                    whiteSpace: 'nowrap',
                                    transition: 'background 0.15s',
                                }}
                            >
                                {b.label}
                            </a>
                        ))
                    )}

                    {/* 새 페이지 추가 버튼 — 로딩 중에는 숨김 */}
                    {!tabsLoading && (
                        <button
                            onClick={() => setShowAddTab(true)}
                            title="새 페이지 추가"
                            style={{
                                width: '28px',
                                height: '28px',
                                borderRadius: '6px',
                                border: '1px solid #e5e7eb',
                                background: 'transparent',
                                color: '#6b7280',
                                fontSize: '18px',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                marginLeft: '4px',
                                lineHeight: 1,
                                flexShrink: 0,
                            }}
                        >
                            +
                        </button>
                    )}
                </div>

                {/* 현재 뷰 모드 뱃지 (읽기 전용 — 생성 시 결정, 변경 불가) */}
                <span
                    title={`이 페이지는 ${VIEW_MODE_CONFIG[viewMode].label} 레이아웃입니다`}
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px',
                        padding: '3px 10px',
                        borderRadius: '12px',
                        marginLeft: '8px',
                        background: '#f0f4ff',
                        border: '1px solid #c7d8f4',
                        color: '#0046A4',
                        fontSize: '12px',
                        fontWeight: 600,
                        whiteSpace: 'nowrap',
                        userSelect: 'none',
                    }}
                >
                    <span style={{ fontSize: '13px' }}>{VIEW_MODE_CONFIG[viewMode].icon}</span>
                    {VIEW_MODE_CONFIG[viewMode].label}
                </span>

                {/* 액션 버튼 */}
                <div style={{ display: 'flex', gap: '6px', marginLeft: '8px' }}>
                    <button onClick={handleViewHtml} className="nav-btn" style={btnStyle}>
                        HTML
                    </button>
                    <button onClick={handlePreview} className="nav-btn" style={btnStyle}>
                        미리보기
                    </button>
                    <button
                        onClick={handleCloseTab}
                        style={{
                            ...btnStyle,
                            color: '#6b7280',
                            borderColor: '#e5e7eb',
                        }}
                        title="탭 목록에서 제거 (페이지는 대시보드에서 확인 가능)"
                    >
                        탭 닫기
                    </button>
                    <button
                        onClick={handleDeletePage}
                        style={{
                            ...btnStyle,
                            color: '#dc2626',
                            borderColor: '#fca5a5',
                        }}
                        title="현재 페이지 삭제"
                    >
                        삭제
                    </button>
                    <button
                        onClick={handleSave}
                        style={{ ...btnStyle, background: '#0046A4', color: '#fff', borderColor: '#0046A4' }}
                    >
                        저장
                    </button>
                </div>
            </nav>

            {/* ── ContentBuilder 캔버스 + 드롭 오버레이 ── */}
            <div
                style={{
                    marginTop: '52px', // 네비바 높이
                    paddingTop: '56px', // RTE 툴바 높이(~44px) + 여백 12px — container margin 대신 여기에 설정
                    marginRight: `${PANEL_WIDTH_OPEN}px`,
                    minHeight: 'calc(100vh - 52px)',
                    position: 'relative',
                    background: viewMode === 'responsive' ? '#ffffff' : '#dde1e7', // 모바일 기기 배경색
                    overflowX: 'visible',
                }}
            >
                {/*
              드래그 중 캔버스 위에 덮는 실제 드롭 타겟 오버레이.
              ContentBuilder 내부 DOM이 dragover에 stopPropagation을 호출해도
              오버레이는 ContentBuilder 위에 직접 렌더링되므로 이벤트를 수신합니다.
            */}
                {isDraggingComponent && (
                    <div
                        onDragOver={handleOverlayDragOver}
                        onDragLeave={handleOverlayDragLeave}
                        onDrop={handleOverlayDrop}
                        style={{
                            position: 'absolute',
                            inset: 0,
                            zIndex: 85,
                            border: isDropOver ? '2px dashed #0046A4' : '2px dashed #93c5fd',
                            background: isDropOver ? 'rgba(0,70,164,0.06)' : 'rgba(0,70,164,0.02)',
                            transition: 'border-color 0.15s, background 0.15s',
                            borderRadius: '4px',
                            cursor: 'copy',
                        }}
                    >
                        {/* 드롭 안내 배지 */}
                        <div
                            style={{
                                position: 'absolute',
                                top: '50%',
                                left: '50%',
                                transform: 'translate(-50%, -50%)',
                                background: isDropOver ? '#0046A4' : 'rgba(0,70,164,0.75)',
                                color: '#ffffff',
                                padding: '10px 24px',
                                borderRadius: '24px',
                                fontSize: '13px',
                                fontWeight: 600,
                                whiteSpace: 'nowrap',
                                boxShadow: '0 4px 16px rgba(0,70,164,0.25)',
                                pointerEvents: 'none',
                                transition: 'background 0.15s',
                            }}
                        >
                            {isDropOver ? '놓으면 캔버스에 추가됩니다' : '이곳에 놓아 추가하세요'}
                        </div>

                        {/* 삽입 위치 표시 선 (드래그오버 시) — 모바일 컨테이너 너비에 맞춤 */}
                        {dropLineY !== null && (
                            <div
                                style={{
                                    position: 'fixed',
                                    top: dropLineY,
                                    left: 0,
                                    right: `${PANEL_WIDTH_OPEN}px`,
                                    display: 'flex',
                                    justifyContent: 'center',
                                    zIndex: 86,
                                    pointerEvents: 'none',
                                }}
                            >
                                <div
                                    style={{
                                        width: VIEW_MODE_CONFIG[viewMode].maxWidth,
                                        maxWidth: '100%',
                                        height: '3px',
                                        background: '#0046A4',
                                        borderRadius: '2px',
                                        boxShadow: '0 0 6px rgba(0,70,164,0.5)',
                                    }}
                                />
                            </div>
                        )}
                    </div>
                )}

                {/* 모바일 앱 캔버스
                 * 390px (iPhone 표준) 크기로 모바일 앱 화면을 시뮬레이션
                 * 행도구는 컨테이너 오른쪽 바깥(right: -40px)에 표시 —
                 *   좁은 컨테이너가 가운데 정렬되므로 패널과 충분한 간격 확보
                 * padding-bottom: 240px — 마지막 컴포넌트 아래 드롭 공간
                 */}
                <div
                    className="container"
                    style={{
                        maxWidth: VIEW_MODE_CONFIG[viewMode].maxWidth,
                        width: '100%',
                        margin: '0 auto', // 상단 간격은 wrapper paddingTop으로 처리
                        padding: '0 0 240px 0',
                        background: '#ffffff',
                        minHeight: '700px',
                        boxShadow: viewMode === 'responsive' ? 'none' : '0 8px 48px rgba(0,70,164,0.10)',
                        transition: 'opacity 0.6s ease, max-width 0.3s ease',
                        opacity: containerOpacity,
                    }}
                />
            </div>

            {/* ── 우측 컴포넌트 패널 ── */}
            <ComponentPanel
                onInsert={handleInsertComponent}
                blocks={canvasBlocks}
                onMoveBlock={handleMoveBlock}
                onDelete={handleDelete}
                onDeleteAll={handleDeleteAllBlocks}
                onActivate={handleActivate}
                viewMode={viewMode}
                basicBlocks={basicBlocks}
                financeComponents={financeComponents}
                basicBlocksLoading={basicBlocksLoading}
                financeComponentsLoading={financeComponentsLoading}
                basicBlocksError={basicBlocksError}
                financeComponentsError={financeComponentsError}
                onDragStart={() => {
                    // ref는 즉시 갱신 (dragover 핸들러에서 동기 참조)
                    isDraggingRef.current = true;
                    // state는 리렌더링 트리거 (오버레이 표시)
                    setIsDraggingComponent(true);
                }}
                onDragEnd={() => {
                    isDraggingRef.current = false;
                    setIsDraggingComponent(false);
                    setIsDropOver(false);
                    setDropLineY(null);
                }}
                onComponentUpdate={() => {
                    setFinanceComponentsLoading(true);
                    setFinanceComponentsError(null);
                    fetch(nextApi(`/api/components?type=finance&viewMode=${viewMode}`))
                        .then((res) => res.json())
                        .then((data) => {
                            if (data.ok) {
                                setFinanceComponents(data.components);
                                setFinanceComponentsLoading(false);
                            } else {
                                setFinanceComponentsError(data.error ?? '금융 컴포넌트를 불러오지 못했습니다.');
                                setFinanceComponentsLoading(false);
                            }
                        })
                        .catch((err) => {
                            console.error('금융 컴포넌트 재로드 오류:', err);
                            setFinanceComponentsError('금융 컴포넌트를 불러오지 못했습니다.');
                            setFinanceComponentsLoading(false);
                        });
                }}
            />

            {/* ── product-menu 아이콘 편집 모달 ── */}
            {productMenuBlock && (
                <ProductMenuIconEditor blockEl={productMenuBlock} onClose={() => setProductMenuBlock(null)} />
            )}

            {/* ── 슬라이드 편집 모달 (promo-banner / product-gallery) ── */}
            {slideEditorBlock && (
                <SlideEditorModal blockEl={slideEditorBlock} onClose={() => setSlideEditorBlock(null)} />
            )}

            {/* ── media-video 영상 URL 편집 모달 ── */}
            {mediaVideoBlock && <MediaVideoEditor blockEl={mediaVideoBlock} onClose={() => setMediaVideoBlock(null)} />}

            {/* ── info-accordion 항목 편집 모달 ── */}
            {infoAccordionBlock && (
                <InfoAccordionEditor blockEl={infoAccordionBlock} onClose={() => setInfoAccordionBlock(null)} />
            )}

            {/* ── menu-tab-grid 탭 항목 편집 모달 ── */}
            {menuTabGridBlock && (
                <MenuTabGridEditor
                    blockEl={menuTabGridBlock}
                    canvasBlocks={canvasBlocks}
                    onClose={() => setMenuTabGridBlock(null)}
                />
            )}

            {/* ── benefit-card 혜택 카드 편집 모달 ── */}
            {benefitCardBlock && (
                <BenefitCardEditor blockEl={benefitCardBlock} onClose={() => setBenefitCardBlock(null)} />
            )}

            {/* ── flex-list 가변 리스트 편집 모달 ── */}
            {flexListBlock && <FlexListEditor blockEl={flexListBlock} onClose={() => setFlexListBlock(null)} />}

            {/* ── status-card 현황 카드 편집 모달 ── */}
            {statusCardBlock && <StatusCardEditor blockEl={statusCardBlock} onClose={() => setStatusCardBlock(null)} />}
            {myDataAssetBlock && (
                <MyDataAssetEditor blockEl={myDataAssetBlock} onClose={() => setMyDataAssetBlock(null)} />
            )}
            {financeCalendarBlock && (
                <FinanceCalendarEditor blockEl={financeCalendarBlock} onClose={() => setFinanceCalendarBlock(null)} />
            )}
            {/* ── event-banner 이벤트 배너 편집 모달 ── */}
            {eventBannerBlock && (
                <EventBannerEditor blockEl={eventBannerBlock} onClose={() => setEventBannerBlock(null)} />
            )}

            {/* ── info-card-slide 정보 카드 편집 모달 ── */}
            {infoCardBlock && <InfoCardSlideEditor blockEl={infoCardBlock} onClose={() => setInfoCardBlock(null)} />}

            {/* ── popup-banner 이미지 팝업 편집 패널 ── */}
            {popupBannerBlock && (
                <PopupBannerEditor
                    blockEl={popupBannerBlock}
                    cbOnChange={popupBannerOnChange}
                    onClose={() => {
                        setPopupBannerBlock(null);
                        setPopupBannerOnChange(null);
                    }}
                />
            )}

            {/* ── site-footer 드롭다운 편집 패널 ── */}
            {siteFooterBlock && (
                <SiteFooterSelectEditor blockEl={siteFooterBlock} onClose={() => setSiteFooterBlock(null)} />
            )}

            {/* ── auth-center 아이콘 편집 패널 ── */}
            {authCenterBlock && (
                <AuthCenterIconEditor blockEl={authCenterBlock} onClose={() => setAuthCenterBlock(null)} />
            )}

            {/* ── app-header 구분선 편집 패널 ── */}
            {appHeaderBlock && (
                <AppHeaderBorderEditor blockEl={appHeaderBlock} onClose={() => setAppHeaderBlock(null)} />
            )}

            {/* ── branch-locator 지점 편집 패널 ── */}
            {branchLocatorBlock && (
                <BranchLocatorEditor blockEl={branchLocatorBlock} onClose={() => setBranchLocatorBlock(null)} />
            )}

            {/* ── 새 페이지 추가 모달 ── */}
            {showAddTab && <CreatePageModal onClose={() => setShowAddTab(false)} canWrite={canWrite} />}

            {/* ── 이미지 교체 picker 모달 (iframe으로 /cms/files 렌더) ──
                - #fileEmbedImage 클릭 인터셉트 시 열림
                - iframe 내부에서 완료/닫기 시 postMessage로 모달 닫기 요청 수신
                - 초기 크기: 1280×900 (화면이 좁으면 95vw/95vh로 축소), 이후 8방향 드래그로 리사이즈 가능
                - z-index: 편집 패널(99998/99999)과 TableEditorModal(100001)보다 위로 → 1,000,000 */}
            {imagePickerOpen && (
                <div
                    className="fixed inset-0 z-[1000000] bg-black/50"
                    onClick={(e) => {
                        // 바깥(오버레이) 클릭 시 닫기 — 내부 iframe 클릭은 제외
                        if (e.target === e.currentTarget) {
                            imageReplaceTargetRef.current = null;
                            setImagePickerOpen(false);
                        }
                    }}
                >
                    <div
                        ref={imagePickerBoxRef}
                        // 절대 위치 + 초기 중앙 배치는 useEffect에서 계산
                        className="absolute overflow-hidden rounded-lg bg-white shadow-xl"
                    >
                        <iframe
                            ref={imagePickerIframeRef}
                            src={nextApi('/files')}
                            className="h-full w-full border-0"
                            title="이미지 선택"
                        />
                    </div>
                </div>
            )}

            {infoToast && <Toast message={infoToast} />}
        </>
    );
}
