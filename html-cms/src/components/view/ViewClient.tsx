// src/components/view/ViewClient.tsx
'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { nextApi } from '@/lib/api-url';

// Runtime library for rendering ContentBuilder-generated content
import ContentBuilderRuntime from '@innovastudio/contentbuilder-runtime';
import '@innovastudio/contentbuilder-runtime/dist/contentbuilder-runtime.css';

// 반응형 미리보기 슬라이더 프리셋
const RESPONSIVE_PRESETS = [
    { label: '모바일', width: 390 },
    { label: '태블릿', width: 768 },
    { label: '데스크탑', width: 1280 },
];

const RESPONSIVE_MIN = 320;
const RESPONSIVE_MAX = 1440;

type Props = {
    html: string;
    viewMode: 'mobile' | 'web' | 'responsive';
    /** 반응형 iframe src 생성에 필요한 페이지 ID */
    bank?: string;
    /** true이면 iframe 내부 렌더링 — 툴바 없이 콘텐츠만 표시 */
    embed?: boolean;
};

export default function ViewClient({ html, viewMode, bank, embed }: Props) {
    // 반응형 모드 툴바용 너비 상태
    const [responsiveWidth, setResponsiveWidth] = useState<number>(RESPONSIVE_MAX);
    const iframeWrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (viewMode === 'responsive' && !embed) {
            const initial = Math.min(window.innerWidth, RESPONSIVE_MAX);
            setResponsiveWidth(initial);
            if (iframeWrapperRef.current) iframeWrapperRef.current.style.width = `${initial}px`;
        }
    }, [viewMode, embed]);

    // 뷰 모드를 body 속성으로 노출 — 플러그인(popup-banner 등)에서 CSS로 감지
    useEffect(() => {
        // responsive + !embed는 iframe 껍데기 — 실제 콘텐츠는 embed iframe 내부에서 처리
        if (viewMode === 'responsive' && !embed) return;
        document.body.setAttribute('data-view-mode', viewMode);
        return () => {
            document.body.removeAttribute('data-view-mode');
        };
    }, [viewMode, embed]);

    const applyWidth = useCallback((width: number) => {
        setResponsiveWidth(width);
        if (iframeWrapperRef.current) iframeWrapperRef.current.style.width = `${width}px`;
    }, []);

    useEffect(() => {
        // 반응형 모드에서 툴바 쪽(비embed)은 iframe을 사용하므로 런타임 초기화 불필요
        if (viewMode === 'responsive' && !embed) return;

        const basePath = window.location.href.substring(0, window.location.href.lastIndexOf('/'));

        // Initialize runtime
        const runtime = new ContentBuilderRuntime({
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

                // ── 금융 모바일 컴포넌트 (플러그인 유지 대상) ──
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
        runtime.init();

        // dangerouslySetInnerHTML은 <script> 태그를 실행하지 않으므로
        // [data-spw-block] 컴포넌트 내 인라인 스크립트를 직접 재실행
        // replaceChild로 동일 위치에 삽입 → document.currentScript.parentElement가 컴포넌트 div를 가리킴
        // 스크립트 실행 전 dot 컨테이너를 초기화하는 코드를 주입 —
        // runtime.init() 이중 실행, React StrictMode 이중 실행 등 어떤 경로로든
        // 스크립트가 여러 번 실행되어도 dot 개수가 누적되지 않도록 보장
        const clearDotsCode = `(function() {
    const block = document.currentScript?.closest('[data-spw-block]');
    if (block) {
        block.querySelectorAll('[data-pg-dots], [data-pb-dots]').forEach(function(dotsContainer) {
            dotsContainer.innerHTML = '';
        });
    }
})();`;
        // data-accordion-inited / data-menu-tab-inited guard 초기화 — 에디터에서 설정된 채
        // 저장된 경우 스크립트가 즉시 return해 이벤트 리스너가 등록되지 않으므로,
        // 재실행 전에 속성을 제거한다.
        document.querySelectorAll<HTMLElement>('[data-spw-block][data-accordion-inited]').forEach((el) => {
            el.removeAttribute('data-accordion-inited');
        });
        document.querySelectorAll<HTMLElement>('[data-spw-block][data-menu-tab-inited]').forEach((el) => {
            el.removeAttribute('data-menu-tab-inited');
        });
        document.querySelectorAll<HTMLElement>('[data-spw-block][data-card-slide-inited]').forEach((el) => {
            el.removeAttribute('data-card-slide-inited');
        });

        // ── 뷰어 전용 슬라이더 컴포넌트 직접 변환 (Issue #6) ─────────────────
        // 원인: PR #348 (menu-tab-grid sticky 수정) 의 iframe 레이아웃 변경 이후
        //       인라인 스크립트 내 document.currentScript 가 null 을 반환하는 경우가 발생해
        //       슬라이더/자동재생 컴포넌트가 초기화되지 않고 세로로 펼쳐져 보임.
        // 조치: benefit-card 처럼 ViewClient 에서 직접 data-* 속성 기반으로 변환.
        //       각 루트 처리 후 내부 <script> 를 제거해 아래 스크립트 재실행 루프와
        //       autoplay 중복 등록을 방지. setInterval 핸들은 cleanup 에서 해제.
        const sliderCleanups: (() => void)[] = [];

        // promo-banner: 배너 카드 가로 스와이프 + dots + 카운터 + autoplay(5s)
        document.querySelectorAll<HTMLElement>('[data-component-id^="promo-banner"]').forEach((root) => {
            const track = root.querySelector<HTMLElement>('[data-pb-track]');
            if (!track) return;

            const slides = Array.from(track.querySelectorAll<HTMLElement>('[data-pb-slide]'));
            if (!slides.length) return;

            // 트랙을 가로 스냅 슬라이더로 변환 — 마이그레이션 스크립트와 동일 스펙
            track.style.cssText =
                'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                'gap:0;padding:12px 12px 4px;';
            slides.forEach((s) => {
                s.style.cssText =
                    'flex-shrink:0;width:100%;scroll-snap-align:start;padding:0 8px;box-sizing:border-box;';
            });

            const dotsEl = root.querySelector<HTMLElement>('[data-pb-dots]');
            const counterCur = root.querySelector<HTMLElement>('[data-pb-cur]');
            let cur = 0;
            if (counterCur) counterCur.textContent = '1';

            const updateDots = (i: number) => {
                if (!dotsEl) return;
                Array.from(dotsEl.children).forEach((d, j) => {
                    (d as HTMLElement).style.background = j === i ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.4)';
                });
            };
            // Gemini 리뷰 반영: track.clientWidth 대신 실제 슬라이드 위치/너비 사용.
            // 트랙에 padding 이 있으면 slide.width(=트랙 content 너비)와 clientWidth 이 달라
            // i * track.clientWidth 로 스크롤하면 인덱스마다 오차가 누적됨.
            const goTo = (i: number) => {
                const targetSlide = slides[i];
                if (!targetSlide) return;
                cur = i;
                track.scrollTo({ left: targetSlide.offsetLeft - track.offsetLeft, behavior: 'smooth' });
                updateDots(i);
                if (counterCur) counterCur.textContent = String(i + 1);
            };

            // dots 구성 (이미 채워져 있으면 한 번 비우고 재구성해 중복 방지)
            if (dotsEl) {
                dotsEl.innerHTML = '';
                slides.forEach((_, i) => {
                    const d = document.createElement('button');
                    d.setAttribute('aria-label', `슬라이드 ${i + 1}`);
                    d.style.cssText =
                        'width:6px;height:6px;border-radius:50%;border:none;padding:0;cursor:pointer;' +
                        'flex-shrink:0;display:block;line-height:0;font-size:0;overflow:hidden;background:' +
                        (i === 0 ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.4)') +
                        ';';
                    d.addEventListener('click', () => goTo(i));
                    dotsEl.appendChild(d);
                });
            }

            // scroll → cur 인덱스 갱신 (80ms 디바운스)
            // slides[0].offsetWidth 로 실제 슬라이드 너비 계산, 0 또는 숨김 상태 방어
            let scrollTimer: ReturnType<typeof setTimeout> | undefined;
            const onScroll = () => {
                if (scrollTimer) clearTimeout(scrollTimer);
                scrollTimer = setTimeout(() => {
                    const slideWidth = slides[0]?.offsetWidth;
                    if (!slideWidth) return;
                    const i = Math.round(track.scrollLeft / slideWidth);
                    if (i !== cur) {
                        cur = i;
                        updateDots(i);
                        if (counterCur) counterCur.textContent = String(i + 1);
                    }
                }, 80);
            };
            track.addEventListener('scroll', onScroll, { passive: true });

            // autoplay 5s, 터치 시 해제
            const timer = setInterval(() => goTo((cur + 1) % slides.length), 5000);
            const onTouchStart = () => clearInterval(timer);
            track.addEventListener('touchstart', onTouchStart, { passive: true, once: true });

            sliderCleanups.push(() => {
                clearInterval(timer);
                if (scrollTimer) clearTimeout(scrollTimer);
                track.removeEventListener('scroll', onScroll);
                track.removeEventListener('touchstart', onTouchStart);
            });

            // 내부 <script> 제거 — 아래 재실행 루프와 중복 초기화 방지
            root.querySelectorAll('script').forEach((s) => s.remove());
        });

        // product-gallery: 상품 카드 가로 스와이프 + dots + autoplay(4s)
        // responsive variant 은 768px 이상에서 그리드 레이아웃으로 전환
        document.querySelectorAll<HTMLElement>('[data-component-id^="product-gallery"]').forEach((root) => {
            const track = root.querySelector<HTMLElement>('[data-pg-track]');
            if (!track) {
                // product-gallery-web: data-pg-grid 기반 — 슬라이더 없음, flex-row 그리드 강제 적용
                // ContentBuilder가 inline style을 덮어쓰는 경우를 대비해 명시적으로 재적용
                const grid = root.querySelector<HTMLElement>('[data-pg-grid]');
                if (grid) {
                    grid.style.cssText =
                        'display:flex;flex-direction:row;flex-wrap:wrap;gap:12px;padding:4px 20px 20px;box-sizing:border-box;';
                    grid.querySelectorAll<HTMLElement>(':scope > div').forEach((card) => {
                        card.style.flex = '1';
                        card.style.minWidth = '260px';
                        card.style.boxSizing = 'border-box';
                    });
                }
                return;
            }
            const slides = Array.from(track.querySelectorAll<HTMLElement>('[data-pg-slide]'));
            if (!slides.length) return;

            const dotsEl = root.querySelector<HTMLElement>('[data-pg-dots]');
            const componentId = root.getAttribute('data-component-id') ?? '';
            const isResponsive = componentId.endsWith('-responsive');
            let cur = 0;
            let timer: ReturnType<typeof setInterval> | null = null;
            let scrollTimer: ReturnType<typeof setTimeout> | undefined;

            const updateDots = (i: number) => {
                if (!dotsEl) return;
                Array.from(dotsEl.children).forEach((d, j) => {
                    (d as HTMLElement).style.background = j === i ? '#0046A4' : 'rgba(0,70,164,0.25)';
                });
            };
            // promo-banner 와 동일하게 슬라이드 실제 위치/너비 기반 — clientWidth 누적 오차 방지
            const goTo = (i: number) => {
                const targetSlide = slides[i];
                if (!targetSlide) return;
                cur = i;
                track.scrollTo({ left: targetSlide.offsetLeft - track.offsetLeft, behavior: 'smooth' });
                updateDots(i);
            };

            // dots 재구성
            if (dotsEl) {
                dotsEl.innerHTML = '';
                slides.forEach((_, i) => {
                    const d = document.createElement('button');
                    d.setAttribute('aria-label', `슬라이드 ${i + 1}`);
                    d.style.cssText =
                        'width:8px;height:8px;border-radius:50%;border:none;padding:0;cursor:pointer;' +
                        'margin:0 4px;display:block;line-height:0;font-size:0;overflow:hidden;flex-shrink:0;background:' +
                        (i === 0 ? '#0046A4' : 'rgba(0,70,164,0.25)') +
                        ';';
                    d.addEventListener('click', () => goTo(i));
                    dotsEl.appendChild(d);
                });
            }

            const onScroll = () => {
                if (scrollTimer) clearTimeout(scrollTimer);
                scrollTimer = setTimeout(() => {
                    const slideWidth = slides[0]?.offsetWidth;
                    if (!slideWidth) return;
                    const i = Math.round(track.scrollLeft / slideWidth);
                    if (i !== cur) {
                        cur = i;
                        updateDots(i);
                    }
                }, 80);
            };
            track.addEventListener('scroll', onScroll, { passive: true });

            const onTouchStart = () => {
                if (timer) {
                    clearInterval(timer);
                    timer = null;
                }
            };

            const applyGrid = () => {
                if (timer) {
                    clearInterval(timer);
                    timer = null;
                }
                track.style.cssText =
                    'display:flex;flex-direction:row;flex-wrap:wrap;gap:12px;padding:4px 20px 20px;box-sizing:border-box;';
                slides.forEach((s) => {
                    s.style.cssText = 'flex:0 0 calc(33.333% - 8px);min-width:0;box-sizing:border-box;';
                });
                if (dotsEl) dotsEl.style.display = 'none';
            };
            const applySlider = () => {
                track.style.cssText =
                    'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                    '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;padding:4px 0 8px;gap:0;';
                slides.forEach((s) => {
                    s.style.cssText =
                        'flex-shrink:0;width:100%;scroll-snap-align:start;padding:0 20px;box-sizing:border-box;';
                });
                if (dotsEl) dotsEl.style.display = 'flex';
                if (!timer) {
                    timer = setInterval(() => goTo((cur + 1) % slides.length), 4000);
                    track.addEventListener('touchstart', onTouchStart, { passive: true, once: true });
                }
            };

            const applyLayout = () => {
                if (isResponsive && window.innerWidth >= 768) applyGrid();
                else applySlider();
            };
            applyLayout();

            let onResize: (() => void) | null = null;
            if (isResponsive) {
                onResize = applyLayout;
                window.addEventListener('resize', onResize);
            }

            sliderCleanups.push(() => {
                if (timer) clearInterval(timer);
                if (scrollTimer) clearTimeout(scrollTimer);
                track.removeEventListener('scroll', onScroll);
                track.removeEventListener('touchstart', onTouchStart);
                if (onResize) window.removeEventListener('resize', onResize);
            });

            root.querySelectorAll('script').forEach((s) => s.remove());
        });

        // event-banner: 가로 스와이프 + prev/next/pause 버튼 + 자동재생(interval 속성) + 호버 시 일시정지
        document.querySelectorAll<HTMLElement>('[data-component-id^="event-banner"]').forEach((root) => {
            const track = root.querySelector<HTMLElement>('[data-banner-track]');
            if (!track) return;
            const items = Array.from(track.querySelectorAll<HTMLElement>('[data-banner-item]'));
            if (!items.length) return;

            // 세로 나열 → 가로 스크롤 변환
            track.style.cssText =
                'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;';

            // 스크롤바 숨김용 style 태그 (한 번만)
            if (!track.getAttribute('data-eb-id')) {
                const styleId = 'eb-hide-' + Math.random().toString(36).slice(2, 8);
                track.setAttribute('data-eb-id', styleId);
                const styleEl = document.createElement('style');
                styleEl.textContent = `[data-eb-id="${styleId}"]::-webkit-scrollbar{display:none}`;
                root.appendChild(styleEl);
            }

            items.forEach((item) => {
                item.style.flex = '0 0 100%';
                item.style.scrollSnapAlign = 'start';
            });

            const total = items.length;
            let current = 0;
            let timer: ReturnType<typeof setInterval> | null = null;
            let paused = false;
            const indicator = root.querySelector<HTMLElement>('[data-banner-indicator]');
            const prevBtn = root.querySelector<HTMLElement>('[data-banner-prev]');
            const nextBtn = root.querySelector<HTMLElement>('[data-banner-next]');
            const pauseBtn = root.querySelector<HTMLElement>('[data-banner-pause]');
            const interval = parseInt(root.getAttribute('data-banner-interval') || '3000', 10);

            // 다른 슬라이더들과 동일 패턴 — 아이템 실제 offsetLeft 기반 (패딩 변경 대비)
            const goTo = (idx: number) => {
                current = ((idx % total) + total) % total;
                const targetItem = items[current];
                if (!targetItem) return;
                track.scrollTo({ left: targetItem.offsetLeft - track.offsetLeft, behavior: 'smooth' });
                if (indicator) indicator.textContent = `${current + 1} / ${total}`;
            };
            const startTimer = () => {
                if (paused) return;
                timer = setInterval(() => goTo(current + 1), interval);
            };
            const stopTimer = () => {
                if (timer) clearInterval(timer);
                timer = null;
            };
            startTimer();

            const cleanups: (() => void)[] = [];
            if (prevBtn) {
                const onClick = () => {
                    stopTimer();
                    goTo(current - 1);
                    startTimer();
                };
                prevBtn.addEventListener('click', onClick);
                cleanups.push(() => prevBtn.removeEventListener('click', onClick));
            }
            if (nextBtn) {
                const onClick = () => {
                    stopTimer();
                    goTo(current + 1);
                    startTimer();
                };
                nextBtn.addEventListener('click', onClick);
                cleanups.push(() => nextBtn.removeEventListener('click', onClick));
            }
            if (pauseBtn) {
                const onClick = () => {
                    paused = !paused;
                    if (paused) {
                        stopTimer();
                        pauseBtn.innerHTML = '&#9654;';
                    } else {
                        pauseBtn.innerHTML = '&#10073;&#10073;';
                        startTimer();
                    }
                };
                pauseBtn.addEventListener('click', onClick);
                cleanups.push(() => pauseBtn.removeEventListener('click', onClick));
            }
            const onMouseOver = () => stopTimer();
            const onMouseLeave = () => {
                if (!paused) startTimer();
            };
            root.addEventListener('mouseover', onMouseOver);
            root.addEventListener('mouseleave', onMouseLeave);
            cleanups.push(() => root.removeEventListener('mouseover', onMouseOver));
            cleanups.push(() => root.removeEventListener('mouseleave', onMouseLeave));

            sliderCleanups.push(() => {
                stopTimer();
                cleanups.forEach((fn) => fn());
            });

            root.querySelectorAll('script').forEach((s) => s.remove());
        });

        // info-card-slide: view-mode(mobile/web/responsive) 별 레이아웃 + 카드 높이 균등화 + 복사 버튼
        // 자동재생 없음 — 슬라이더 변환과 복사 기능만 재현
        document.querySelectorAll<HTMLElement>('[data-component-id^="info-card-slide"]').forEach((root) => {
            const track = root.querySelector<HTMLElement>('[data-card-track]');
            if (!track) return;

            // view-mode 결정: 속성 우선, 없으면 component-id 꼬리 기반 추론
            const componentId = root.getAttribute('data-component-id') ?? '';
            const modeFromAttr = root.getAttribute('data-card-view-mode');
            const mode =
                modeFromAttr ??
                (componentId.endsWith('-web') ? 'web' : componentId.endsWith('-responsive') ? 'responsive' : 'mobile');

            // web 변형: 외부 래퍼 max-width 제거 — 컨테이너 너비에 맞게 100% 채움
            if (mode === 'web') {
                root.style.maxWidth = '';
                root.style.margin = '0';
                root.style.width = '100%';
                root.style.boxSizing = 'border-box';
            }

            // 레이아웃 적용
            if (mode === 'web') {
                track.style.cssText =
                    'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;' +
                    '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                    'gap:20px;padding:12px 0 20px;scroll-padding:0 2%;';
            } else if (mode === 'responsive') {
                track.style.cssText =
                    'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;' +
                    '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                    'gap:14px;padding:10px 0 16px;scroll-padding:0 2%;';
            } else {
                track.style.cssText =
                    'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                    '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                    'gap:0;padding:8px 0 12px;scroll-padding:0 4%;';
            }

            // 카드 높이 균등화
            let maxH = 0;
            track.querySelectorAll<HTMLElement>('[data-card-item] > div').forEach((inner) => {
                inner.style.minHeight = '0';
                if (inner.scrollHeight > maxH) maxH = inner.scrollHeight;
            });
            track.querySelectorAll<HTMLElement>('[data-card-item] > div').forEach((inner) => {
                inner.style.minHeight = `${maxH}px`;
            });

            // 카드 너비·스냅 정렬 (모드별 다름)
            // mobile: 카드 1개이면 여백 없이 100%, 2개 이상이면 92%로 다음 카드 살짝 노출
            const cardItems = track.querySelectorAll<HTMLElement>('[data-card-item]');
            const isSingle = cardItems.length === 1;
            cardItems.forEach((card) => {
                if (mode === 'web') {
                    card.style.flex = '0 0 min(480px,46vw)';
                    card.style.width = 'min(480px,46vw)';
                    card.style.maxWidth = '';
                    card.style.minWidth = '0';
                    card.style.scrollSnapAlign = 'start';
                } else if (mode === 'responsive') {
                    card.style.flex = '0 0 min(440px,78vw)';
                    card.style.width = 'min(440px,78vw)';
                    card.style.scrollSnapAlign = 'start';
                } else {
                    const w = isSingle ? '100%' : '92%';
                    card.style.flex = `0 0 ${w}`;
                    card.style.width = w;
                    card.style.scrollSnapAlign = 'center';
                }
            });

            // 하단 버튼 텍스트 넘침 처리
            track.querySelectorAll<HTMLElement>('[data-card-item] a').forEach((btn) => {
                if (!btn.style.borderRadius) return;
                btn.style.minWidth = '0';
                btn.style.maxWidth = '100%';
                btn.style.whiteSpace = 'normal';
                btn.style.overflowWrap = 'anywhere';
                btn.style.wordBreak = 'break-all';
                btn.style.boxSizing = 'border-box';
            });

            // 스크롤바 숨김용 style 태그 (mobile/responsive 에서만 의미 있음)
            if (!track.getAttribute('data-ics-id')) {
                const styleId = 'ics-hide-' + Math.random().toString(36).slice(2, 8);
                track.setAttribute('data-ics-id', styleId);
                const styleEl = document.createElement('style');
                styleEl.textContent = `[data-ics-id="${styleId}"]::-webkit-scrollbar{display:none}`;
                root.appendChild(styleEl);
            }

            // 복사 버튼 — 제목 클립보드 복사 + SVG 일시 색상 변경
            const copyCleanups: (() => void)[] = [];
            root.querySelectorAll<HTMLElement>('[data-card-copy]').forEach((btn) => {
                const onClick = (e: Event) => {
                    e.preventDefault();
                    const card = btn.closest('[data-card-item]');
                    const titleEl = card?.querySelector('[data-card-title]');
                    if (titleEl && navigator.clipboard) {
                        navigator.clipboard.writeText(titleEl.textContent || '');
                        const svg = btn.querySelector('svg');
                        if (svg) {
                            svg.setAttribute('stroke', '#059669');
                            setTimeout(() => svg.setAttribute('stroke', '#9CA3AF'), 1500);
                        }
                    }
                };
                btn.addEventListener('click', onClick);
                copyCleanups.push(() => btn.removeEventListener('click', onClick));
            });

            sliderCleanups.push(() => {
                copyCleanups.forEach((fn) => fn());
            });

            root.querySelectorAll('script').forEach((s) => s.remove());
        });

        document.querySelectorAll<HTMLScriptElement>('[data-spw-block] script').forEach((oldScript) => {
            // 외부 스크립트(src), 비 JS 타입(type="text/html" 등), HTML 템플릿 스크립트를 제외합니다.
            if (
                oldScript.src ||
                (oldScript.type && !/javascript|ecmascript/i.test(oldScript.type)) ||
                (oldScript.textContent ?? '').trimStart().startsWith('<')
            ) {
                return;
            }
            const newScript = document.createElement('script');
            newScript.textContent = clearDotsCode + (oldScript.textContent ?? '');
            oldScript.parentNode?.replaceChild(newScript, oldScript);
        });

        // benefit-card mobile scroll-snap 직접 변환
        // dangerouslySetInnerHTML + replaceChild 환경에서 document.currentScript가
        // null을 반환하는 경우를 대비해 ViewClient에서 직접 처리
        document.querySelectorAll<HTMLElement>('[data-bc-track]').forEach((track) => {
            track.className = (track.className || '').replace(/\bflex(?:-col)?\b/g, '').trim();
            track.style.cssText =
                'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                'gap:0;padding:4px 0 8px;';
            track.querySelectorAll<HTMLElement>('[data-bc-slide]').forEach((slide) => {
                slide.style.cssText =
                    'flex-shrink:0;width:80%;scroll-snap-align:start;padding:0 8px;box-sizing:border-box;';
            });
        });

        // benefit-card-web / responsive: data-bc-container flex-row 그리드 강제 적용
        // data-bc-track이 없는 web/responsive 변형은 위 루프에서 처리되지 않음
        // ContentBuilder가 inline style을 덮어쓰는 경우를 대비해 명시적으로 재적용
        document.querySelectorAll<HTMLElement>('[data-component-id^="benefit-card"]').forEach((root) => {
            const compId = root.getAttribute('data-component-id') ?? '';
            if (!compId.endsWith('-web') && !compId.endsWith('-responsive')) return;
            const container = root.querySelector<HTMLElement>('[data-bc-container]');
            if (!container) return;
            if (compId.endsWith('-web')) {
                // 외부 래퍼 max-width 제거 — 컨테이너 너비에 맞게 100% 채움
                root.style.maxWidth = '';
                root.style.margin = '0';
                root.style.width = '100%';
                root.style.boxSizing = 'border-box';
                container.style.cssText = 'display:flex;flex-direction:row;gap:12px;';
                container.querySelectorAll<HTMLElement>(':scope > a').forEach((card) => {
                    card.style.flex = '1';
                    card.style.minWidth = '0';
                });
            } else {
                // responsive: flex-wrap 2열
                container.style.cssText = 'display:flex;flex-wrap:wrap;gap:12px;';
            }
        });

        // ── branch-locator 필터 버튼 ──
        // 인라인 <script>가 dangerouslySetInnerHTML 환경에서 실행 안 되므로 직접 처리
        const blCleanups: (() => void)[] = [];
        document.querySelectorAll<HTMLElement>('[data-component-id^="branch-locator"]').forEach((root) => {
            const filterBtns = Array.from(root.querySelectorAll<HTMLElement>('[data-bl-filter]'));
            const branchItems = Array.from(root.querySelectorAll<HTMLElement>('[data-bl-item]'));
            // ── 바텀시트 드래그 핸들 ──
            // flex 흐름 유지: 지도(flex:1) → 필터(고정) → 시트(명시 높이)
            // 시트 줄이면 지도가 자연 확장 → 지도가 필터 뒤로 침범하지 않음
            const sheet = root.querySelector<HTMLElement>('[data-bl-sheet]');
            const handle = root.querySelector<HTMLElement>('[data-bl-handle]');
            const mapArea = root.querySelector<HTMLElement>('[data-bl-map]')?.parentElement ?? null;
            if (sheet && handle) {
                let dragY = 0;
                let dragH = 0;
                let dragging = false;
                let moved = false;

                // 원본 style 속성 저장 — 복원 시 완벽하게 되돌리기 위해
                const origRootStyle = root.getAttribute('style') ?? '';
                const origMapStyle = mapArea?.getAttribute('style') ?? '';
                const origSheetStyle = sheet.getAttribute('style') ?? '';

                const activateLayout = () => {
                    root.style.height = `${root.offsetHeight}px`;
                    root.style.minHeight = '0';
                    root.style.overflow = 'hidden';
                    if (mapArea) {
                        mapArea.style.aspectRatio = 'unset';
                        mapArea.style.flex = '1';
                    }
                    sheet.style.flex = 'none';
                    sheet.style.minHeight = '0';
                    sheet.style.height = `${dragH}px`;
                    sheet.style.transition = 'none';
                };
                const onStart = (e: MouseEvent | TouchEvent) => {
                    e.preventDefault();
                    dragging = true;
                    moved = false;
                    dragY = 'touches' in e ? e.touches[0].clientY : e.clientY;
                    dragH = sheet.offsetHeight;
                };
                const onMove = (e: MouseEvent | TouchEvent) => {
                    if (!dragging) return;
                    const y = 'touches' in e ? e.touches[0].clientY : e.clientY;
                    if (!moved) {
                        if (Math.abs(y - dragY) < 25) return;
                        moved = true;
                        activateLayout();
                    }
                    e.preventDefault();
                    const maxH = root.offsetHeight * 0.85;
                    const newH = Math.max(24, Math.min(maxH, dragH + (dragY - y)));
                    sheet.style.height = `${newH}px`;
                };
                const restoreAll = () => {
                    root.setAttribute('style', origRootStyle);
                    if (mapArea) mapArea.setAttribute('style', origMapStyle);
                    sheet.setAttribute('style', origSheetStyle);
                };
                const initH = sheet.offsetHeight;
                const onEnd = () => {
                    if (!dragging) return;
                    dragging = false;
                    if (!moved) return;
                    sheet.style.transition = 'height 0.25s ease';
                    const h = sheet.offsetHeight;
                    const diff = Math.abs(h - dragH);
                    if (diff < 20) {
                        restoreAll();
                    } else if (h < initH * 0.4) {
                        sheet.style.height = '24px';
                    } else if (h > initH * 1.2) {
                        sheet.style.height = `${Math.round(root.offsetHeight * 0.75)}px`;
                    } else {
                        restoreAll();
                    }
                };

                handle.addEventListener('mousedown', onStart);
                handle.addEventListener('touchstart', onStart, { passive: false });
                document.addEventListener('mousemove', onMove);
                document.addEventListener('touchmove', onMove, { passive: false });
                document.addEventListener('mouseup', onEnd);
                document.addEventListener('touchend', onEnd);
                blCleanups.push(() => {
                    handle.removeEventListener('mousedown', onStart);
                    handle.removeEventListener('touchstart', onStart);
                    document.removeEventListener('mousemove', onMove);
                    document.removeEventListener('touchmove', onMove);
                    document.removeEventListener('mouseup', onEnd);
                    document.removeEventListener('touchend', onEnd);
                });
            }

            filterBtns.forEach((btn) => {
                const onClick = () => {
                    const filterType = btn.getAttribute('data-bl-filter');
                    filterBtns.forEach((x) => {
                        const isActive = x === btn;
                        x.style.background = isActive ? '#0046A4' : '#fff';
                        x.style.color = isActive ? '#fff' : '#6B7280';
                    });
                    branchItems.forEach((item) => {
                        item.style.display =
                            filterType === 'all' || item.getAttribute('data-bl-item') === filterType ? 'flex' : 'none';
                    });
                };
                btn.addEventListener('click', onClick);
                blCleanups.push(() => btn.removeEventListener('click', onClick));
            });

            // ── 아이템 클릭 → 지도 src 교체 (Issue #332) ──
            // VIEWER_SCRIPT와 달리 dangerouslySetInnerHTML 환경에서 확실히 동작
            const mapIframe = root.querySelector<HTMLIFrameElement>('[data-bl-map]');
            const mapPlaceholder = root.querySelector<HTMLElement>('[data-bl-map-ph]');
            const defaultMapSrc = mapIframe?.getAttribute('src') ?? 'about:blank';

            const sanitizeMapSrc = (url: string): string => {
                const t = url.trim();
                if (
                    t.startsWith('https://www.google.com/maps/embed') ||
                    t.startsWith('https://maps.google.com/maps?') ||
                    t.startsWith('https://map.kakao.com/link/embed')
                )
                    return t;
                return 'about:blank';
            };

            branchItems.forEach((item) => {
                item.style.cursor = 'pointer';
                const onItemClick = (e: MouseEvent) => {
                    if ((e.target as HTMLElement).closest('a[href^="tel:"]')) return;
                    const src = sanitizeMapSrc(item.getAttribute('data-bl-map-src') ?? '');
                    const effectiveSrc = src !== 'about:blank' ? src : sanitizeMapSrc(defaultMapSrc);
                    if (mapIframe) mapIframe.setAttribute('src', effectiveSrc);
                    if (mapPlaceholder) mapPlaceholder.style.display = effectiveSrc !== 'about:blank' ? 'none' : 'flex';
                    branchItems.forEach((x) => {
                        x.style.background = x === item ? '#EEF4FF' : '';
                    });
                };
                item.addEventListener('click', onItemClick);
                blCleanups.push(() => item.removeEventListener('click', onItemClick));
            });
        });

        // menu-tab-grid sticky 강제 보장
        // dangerouslySetInnerHTML + script 재실행 환경에서 ContentBuilderRuntime이
        // 비동기로 .row 스타일을 재처리할 수 있으므로, 이중 rAF로 보장
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                document.querySelectorAll<HTMLElement>('[data-menu-sticky="true"]').forEach((block) => {
                    const row = block.closest<HTMLElement>('.row');
                    if (row) {
                        row.style.position = 'sticky';
                        row.style.top = '0';
                        row.style.zIndex = '100';
                        row.style.background = '#ffffff';
                    }
                });
            });
        });

        // 금융 컴포넌트 내 더미 링크(href="#") 클릭 시 상단 이동 차단
        // ContentBuilder가 onclick 속성을 제거하므로 이벤트 위임으로 처리
        const handleDummyLink = (e: MouseEvent) => {
            const anchor = (e.target as HTMLElement).closest<HTMLAnchorElement>('[data-spw-block] a[href="#"]');
            if (anchor) e.preventDefault();
        };
        document.addEventListener('click', handleDummyLink);

        return () => {
            sliderCleanups.forEach((fn) => fn());
            blCleanups.forEach((fn) => fn());
            document.removeEventListener('click', handleDummyLink);
            runtime.destroy();
        };
    }, [viewMode, embed, html]);

    // ── 반응형 모드: 툴바 + iframe ─────────────────────────────────────────
    if (viewMode === 'responsive' && !embed) {
        const iframeSrc = nextApi(`/view?bank=${bank ?? 'ibk'}&embed=1`);

        return (
            // height:100vh + overflow:hidden → 툴바를 제외한 나머지 높이를 iframe이 flex로 채움
            // (magic number calc(100vh - Npx) 없이 툴바 높이를 자동 반영)
            <div
                style={{
                    background: '#dde1e7',
                    height: '100vh',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                }}
            >
                {/* 툴바 — flexShrink:0으로 높이 고정, iframe 영역이 나머지를 채움 */}
                <div
                    style={{
                        flexShrink: 0,
                        position: 'sticky',
                        top: 0,
                        zIndex: 50,
                        background: '#ffffff',
                        borderBottom: '1px solid #e5e7eb',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '12px',
                        padding: '10px 24px',
                        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
                    }}
                >
                    <div style={{ display: 'flex', gap: '4px' }}>
                        {RESPONSIVE_PRESETS.map(({ label, width }) => (
                            <button
                                key={width}
                                onClick={() => applyWidth(width)}
                                style={{
                                    padding: '4px 10px',
                                    borderRadius: '5px',
                                    border: '1px solid',
                                    cursor: 'pointer',
                                    fontSize: '12px',
                                    fontWeight: 500,
                                    borderColor: responsiveWidth === width ? '#0046A4' : '#e5e7eb',
                                    background: responsiveWidth === width ? '#EEF4FF' : '#ffffff',
                                    color: responsiveWidth === width ? '#0046A4' : '#6b7280',
                                }}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                    <input
                        type="range"
                        min={RESPONSIVE_MIN}
                        max={RESPONSIVE_MAX}
                        value={responsiveWidth}
                        onChange={(e) => applyWidth(Number(e.target.value))}
                        style={{ width: '200px', accentColor: '#0046A4' }}
                    />
                    <span
                        style={{
                            fontSize: '13px',
                            color: '#374151',
                            fontVariantNumeric: 'tabular-nums',
                            minWidth: '60px',
                        }}
                    >
                        {responsiveWidth}px
                    </span>
                </div>

                {/* iframe 영역 — flex:1로 툴바 이후 나머지 높이 전부 차지, iframe이 100% 채움 */}
                <div style={{ flex: 1, minHeight: 0, overflow: 'hidden', padding: '24px 0' }}>
                    <div
                        ref={iframeWrapperRef}
                        style={{
                            width: `${RESPONSIVE_MAX}px`, // useEffect에서 window.innerWidth로 교정
                            maxWidth: '100%',
                            height: '100%',
                            margin: '0 auto',
                            transition: 'width 0.1s ease',
                            boxShadow: '0 8px 48px rgba(0,70,164,0.10)',
                            background: '#ffffff',
                        }}
                    >
                        <iframe
                            src={iframeSrc}
                            style={{
                                width: '100%',
                                height: '100%',
                                border: 'none',
                                display: 'block',
                            }}
                        />
                    </div>
                </div>
            </div>
        );
    }

    // ── mobile / web 모드: 직접 렌더링 ───────────────────────────────────
    return (
        <div
            style={{
                background: embed ? 'transparent' : '#dde1e7',
                minHeight: '100vh',
                padding: viewMode === 'web' || embed ? '0' : '32px 0 80px',
            }}
        >
            <div
                suppressHydrationWarning
                className="is-container"
                style={{
                    maxWidth: viewMode === 'mobile' ? '390px' : '100%',
                    margin: viewMode === 'mobile' ? '0 auto' : '0',
                    width: '100%',
                    background: '#ffffff',
                    minHeight: '100vh',
                    boxShadow: viewMode === 'mobile' ? '0 8px 48px rgba(0,70,164,0.10)' : 'none',
                    padding: 0,
                }}
                dangerouslySetInnerHTML={{ __html: html || '' }}
            />
        </div>
    );
}
