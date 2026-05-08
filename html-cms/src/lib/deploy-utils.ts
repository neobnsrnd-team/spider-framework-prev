// src/lib/deploy-utils.ts
// 배포 공통 유틸 — 운영 서버 전송·삭제 헬퍼 및 HTML 조립 로직

import { createHash } from 'crypto';
import { readFile } from 'fs/promises';
import path from 'path';

const DEPLOY_SECRET = process.env.DEPLOY_SECRET ?? '';

const CMS_BASE_URL = process.env.CMS_BASE_URL || 'http://localhost:3000';
const CMS_PATH_PREFIX = process.env.NEXT_PUBLIC_CMS_BASE_PATH ?? '/cms';
const BASE = `${CMS_BASE_URL}${CMS_PATH_PREFIX}`;

// 배포 HTML 렌더링에 필요한 레이아웃 CSS (globals.css 56~106행)
const LAYOUT_CSS = `
.is-container [data-cb-type] {
    display: block;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
}
.is-container .is-col:has(> [data-cb-type]),
.is-container .column:has(> [data-cb-type]),
.is-container [class*="col"]:has(> [data-cb-type]),
.is-container .column.spw-finance-col {
    padding-left: 0 !important;
    padding-right: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    flex: 0 0 100% !important;
}
.is-container .row {
    margin-left: 0 !important;
    margin-right: 0 !important;
}
`;

/** SHA-256 앞 16자리로 무결성 값 생성 */
export function calcCrc(content: string): string {
    return createHash('sha256').update(content, 'utf8').digest('hex').slice(0, 16);
}

/** HTML 프래그먼트 → 완전한 HTML 문서로 조립 (CSS 인라인 + 경로 치환 + 트래커 포함) */
export async function buildDeployHtml(fragment: string, pageId: string, pageName: string): Promise<string> {
    // 1. ContentBuilder 런타임 CSS 읽기 — public/runtime 사용 (standalone 빌드 환경 호환)
    let runtimeCss = '';
    try {
        const cssPath = path.join(process.cwd(), 'public', 'runtime', 'contentbuilder-runtime.css');
        runtimeCss = await readFile(cssPath, 'utf8');
    } catch {
        throw new Error('ContentBuilder 런타임 CSS를 찾을 수 없습니다. public/runtime/ 디렉토리를 확인해주세요.');
    }

    // 2. 에셋 경로 치환 — CMS 서버 절대 URL로 변환 (basePath 포함)
    // 선행 슬래시 유무·역슬래시(Windows), 큰따옴표·작은따옴표 모두 처리
    const html = fragment
        .replace(/src=(['"])\/?(assets|uploads)[\/\\]/g, `src=$1${BASE}/$2/`)
        .replace(/url\((['"]?)\/?(assets|uploads)[\/\\]/g, `url($1${BASE}/$2/`)
        .replace(/src=(['"])\/api\/assets\//g, `src=$1${BASE}/api/assets/`);

    // 3. 완전한 HTML 문서 조립 — 스크립트 URL에 basePath 반영
    return `<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <title>${pageName}</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>${runtimeCss}</style>
    <style>${LAYOUT_CSS}</style>
</head>
<body class="is-container">
${html}
    <script src="${BASE}/runtime/contentbuilder-runtime.min.js"></script>
    <script>
    // ContentBuilder 런타임 초기화 — 플러그인 동적 로드 + mount
    document.addEventListener('DOMContentLoaded', function() {
        if (typeof ContentBuilderRuntime === 'undefined') return;
        var base = '${BASE}';
        var pluginNames = [
            'logo-loop','click-counter','card-list','accordion','hero-animation',
            'animated-stats','timeline','before-after-slider','more-info','social-share',
            'pendulum','browser-mockup','hero-background','cta-buttons',
            'media-slider','media-grid','particle-constellation','vector-force',
            'aurora-glow','simple-stats','faq','callout-box','code','video-embed',
            'swiper-slider','exchange-board','loan-calculator'
        ];
        var plugins = {};
        pluginNames.forEach(function(name) {
            plugins[name] = {
                url: base + '/assets/plugins/' + name + '/index.js',
                css: base + '/assets/plugins/' + name + '/style.css'
            };
        });
        var runtime = new ContentBuilderRuntime({ plugins: plugins });
        runtime.init();

        // 인라인 스크립트 재실행 (dangerouslySetInnerHTML과 동일 이슈)
        // data-card-slide-inited 등 inited 가드 초기화 — 에디터에서 설정된 채 저장된 경우 대비
        document.querySelectorAll('[data-spw-block][data-card-slide-inited]').forEach(function(el) {
            el.removeAttribute('data-card-slide-inited');
        });
        document.querySelectorAll('[data-spw-block] script').forEach(function(oldScript) {
            var newScript = document.createElement('script');
            newScript.textContent = oldScript.textContent;
            oldScript.parentNode.replaceChild(newScript, oldScript);
        });

        // ── 금융 web 변형 컴포넌트 그리드 보장 ──────────────────────────────
        // inline script가 없는 web 변형은 스크립트 재실행으로 처리되지 않으므로 직접 적용
        // ContentBuilder가 inline style을 덮어쓴 경우도 여기서 복원됨

        // product-gallery-web: data-pg-grid flex-row 3열 그리드
        document.querySelectorAll('[data-pg-grid]').forEach(function(grid) {
            grid.style.cssText =
                'display:flex;flex-direction:row;flex-wrap:wrap;gap:12px;padding:4px 20px 20px;box-sizing:border-box;';
            Array.from(grid.children).forEach(function(card) {
                card.style.flex = '1';
                card.style.minWidth = '260px';
                card.style.boxSizing = 'border-box';
            });
        });

        // benefit-card-web / responsive: data-bc-container flex-row 그리드
        document.querySelectorAll('[data-component-id^="benefit-card"]').forEach(function(root) {
            var compId = root.getAttribute('data-component-id') || '';
            if (!compId.endsWith('-web') && !compId.endsWith('-responsive')) return;
            var container = root.querySelector('[data-bc-container]');
            if (!container) return;
            if (compId.endsWith('-web')) {
                // 외부 래퍼 max-width 제거 — 컨테이너 너비에 맞게 100% 채움
                root.style.maxWidth = '';
                root.style.margin = '0';
                root.style.width = '100%';
                root.style.boxSizing = 'border-box';
                container.style.cssText = 'display:flex;flex-direction:row;gap:12px;';
                Array.from(container.querySelectorAll(':scope > a')).forEach(function(card) {
                    card.style.flex = '1';
                    card.style.minWidth = '0';
                });
            } else {
                container.style.cssText = 'display:flex;flex-wrap:wrap;gap:12px;';
            }
        });

        // benefit-card-mobile: data-bc-track scroll-snap 슬라이더
        document.querySelectorAll('[data-bc-track]').forEach(function(track) {
            track.className = (track.className || '').replace(/\bflex(?:-col)?\b/g, '').trim();
            track.style.cssText =
                'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x mandatory;' +
                '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                'gap:0;padding:4px 0 8px;';
            track.querySelectorAll('[data-bc-slide]').forEach(function(slide) {
                slide.style.cssText =
                    'flex-shrink:0;width:80%;scroll-snap-align:start;padding:0 8px;box-sizing:border-box;';
            });
        });

        // info-card-slide-web: SLIDE_SCRIPT가 grid를 적용한 경우 scroll-snap 슬라이더로 교정
        // (구버전 DB 레코드 대비 — 인라인 스크립트 재실행 이후 override)
        document.querySelectorAll('[data-component-id="info-card-slide-web"]').forEach(function(root) {
            // 외부 래퍼 max-width 제거 — 컨테이너 너비에 맞게 100% 채움
            root.style.maxWidth = '';
            root.style.margin = '0';
            root.style.width = '100%';
            root.style.boxSizing = 'border-box';
            var track = root.querySelector('[data-card-track]');
            if (!track) return;
            track.style.cssText =
                'display:flex;flex-direction:row;overflow-x:auto;scroll-snap-type:x proximity;' +
                '-webkit-overflow-scrolling:touch;scrollbar-width:none;-ms-overflow-style:none;' +
                'gap:20px;padding:12px 0 20px;scroll-padding:0 2%;';
            track.querySelectorAll('[data-card-item]').forEach(function(card) {
                card.style.flex = '0 0 min(480px,46vw)';
                card.style.width = 'min(480px,46vw)';
                card.style.maxWidth = '';
                card.style.minWidth = '0';
                card.style.scrollSnapAlign = 'start';
            });
        });
    });
    </script>
    <script src="${BASE}/cms-tracker.js" data-page-id="${pageId}" data-cms-url="${BASE}"></script>
</body>
</html>`;
}

/** 운영 서버 base URL 생성 — 포트 null 안전 처리 */
export function buildServerUrl(ip: string | null, port: number | null, path: string): string {
    const host = port ? `${ip}:${port}` : (ip ?? '');
    return `http://${host}${path}`;
}

/** 배포 대상 서버로 HTML + 트래커 JS 전송 */
export async function sendToServer(
    serverUrl: string,
    pageId: string,
    html: string,
    trackerJs?: string | null,
): Promise<void> {
    const res = await fetch(serverUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'x-deploy-token': DEPLOY_SECRET,
        },
        body: JSON.stringify({ pageId, html, trackerJs }),
    });
    if (!res.ok) {
        const text = await res.text().catch(() => '응답 없음');
        throw new Error(`서버 전송 실패 (${res.status}): ${text}`);
    }
}

/** 배포 대상 서버에서 페이지 HTML 파일 삭제 요청 */
export async function deleteFromServer(serverUrl: string, pageId: string): Promise<void> {
    const res = await fetch(serverUrl, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'x-deploy-token': DEPLOY_SECRET,
        },
        body: JSON.stringify({ pageId }),
    });
    if (!res.ok) {
        const text = await res.text().catch(() => '응답 없음');
        throw new Error(`서버 파일 삭제 실패 (${res.status}): ${text}`);
    }
}
