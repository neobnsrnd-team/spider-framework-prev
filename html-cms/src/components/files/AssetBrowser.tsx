'use client';

import { useEffect, useMemo, useState } from 'react';

import { nextApi } from '@/lib/api-url';

interface AssetItem {
    assetId: string;
    assetName: string;
    businessCategory: string | null;
    mimeType: string;
    fileSize: number | null;
    assetDesc: string | null;
    url: string | null;
    path: string | null;
    useYn: string | null;
}

/**
 * 물리 경로에서 "이미지가 있는 폴더명" 추출
 * 예) "public/deployed/img/foo.jpg" → "img", "/data/deployed/img/foo.jpg" → "img"
 *     "public/uploads/bar.png" → "uploads". 경로 미상이면 빈 문자열.
 * 향후 하위 디렉터리가 늘어나면 구분 가능한 단위로 자동 확장된다.
 */
function extractFolderName(path: string | null): string {
    if (!path) return '';
    const normalized = path.replace(/\\/g, '/');
    const parts = normalized.split('/').filter(Boolean);
    // 마지막 요소는 파일명 — 그 직전 세그먼트를 폴더명으로 사용
    if (parts.length < 2) return '';
    return parts[parts.length - 2];
}

interface CodeItem {
    code: string;
    codeName: string;
    sortOrder: number;
}

const PAGE_SIZE_OPTIONS = [20, 30, 50, 100, 200];

/**
 * 자산 이미지 src 해결
 * - DB의 ASSET_URL은 보통 `/deployed/static/<filename>` · `/static/<filename>` · `/uploads/<filename>` 등
 *   루트 상대 경로. 운영 nginx가 실제 파일을 서빙한다.
 * - 로컬 dev는 공유 DB가 가리키는 실제 파일이 로컬에 없으므로, 서버에서 받은 assetOrigin(CMS_BASE_URL)
 *   을 상대 경로 앞에 붙여 원격 nginx에서 직접 로드한다. assetOrigin이 비어 있으면 상대 경로 그대로 사용.
 */
function resolveAssetSrc(url: string | null, assetOrigin: string): string | undefined {
    if (!url) return undefined;
    if (/^https?:\/\//i.test(url)) return url;

    const normalized = url.startsWith('/') ? url : `/${url}`;
    return assetOrigin ? `${assetOrigin}${normalized}` : normalized;
}

interface AssetBrowserProps {
    assetOrigin?: string;
}

export default function AssetBrowser({ assetOrigin = '' }: AssetBrowserProps) {
    const [assets, setAssets] = useState<AssetItem[]>([]);
    const [categories, setCategories] = useState<CodeItem[]>([]);
    // 단일 선택 — 한 번에 이미지 하나만 고를 수 있다.
    const [selectedUrl, setSelectedUrl] = useState<string | null>(null);
    const [category, setCategory] = useState('');
    const [search, setSearch] = useState('');
    const [draftSearch, setDraftSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [totalCount, setTotalCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    // 좌측 폴더 사이드바 토글 — 상단 우측 햄버거 버튼으로 열고 닫는다.
    const [sidebarOpen, setSidebarOpen] = useState(true);
    // 선택된 폴더 — 빈 값이면 전체. 현재는 자산 전부가 한 폴더(예: img)라 실질적 필터는 없고,
    // 향후 여러 폴더로 나뉠 때 API 단에서 경로 필터를 추가하면 이 상태를 활용한다.
    const [folder, setFolder] = useState('');

    // 승인+배포 필터는 서버(`deployedOnly=true`)가 담당하므로 클라이언트에선 추가 필터 없이 그대로 사용.
    // (이전에는 클라가 isDeployedAsset 으로 재필터 → 서버 totalCount 와 실제 표시 건수가 어긋나는 버그가 있었음)

    // 현재 페이지 자산에서 추출한 distinct 폴더명 — 사이드바 리스트 소스
    const folders = useMemo(() => {
        const set = new Set<string>();
        assets.forEach((asset) => {
            const name = extractFolderName(asset.path);
            if (name) set.add(name);
        });
        return Array.from(set).sort();
    }, [assets]);

    // 선택된 폴더가 있으면 현재 페이지 내에서 클라이언트 필터 — 다중 폴더 대비 placeholder 동작
    const visibleAssets = useMemo(() => {
        if (!folder) return assets;
        return assets.filter((asset) => extractFolderName(asset.path) === folder);
    }, [assets, folder]);

    // 카테고리 코드 → codeName 매핑. FWK_CODE 응답만을 단일 출처로 사용 (하드코딩 폴백 없음).
    // codeName 이 비어 있는 코드는 의도적으로 매핑하지 않아, 카드 뱃지에서 '-' 로 표시되도록 한다.
    const categoryMap = useMemo(
        () =>
            categories.reduce<Record<string, string>>((acc, item) => {
                if (item.codeName) acc[item.code] = item.codeName;
                return acc;
            }, {}),
        [categories],
    );

    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    const visiblePages = useMemo(() => {
        const length = Math.min(7, totalPages);
        const half = 3;
        let start = Math.max(1, page - half);
        const end = Math.min(totalPages, start + 6);
        start = Math.max(1, end - 6);
        return Array.from({ length }, (_, index) => start + index);
    }, [page, totalPages]);

    useEffect(() => {
        let cancelled = false;

        async function loadCategories() {
            try {
                // FWK_CODE(CMS00001) 단일 출처. cache:no-store + 서버 force-dynamic 으로 항상 최신 조회.
                const res = await fetch(nextApi('/api/cms-admin/asset-categories'), { cache: 'no-store' });
                const json = await res.json();
                const list: CodeItem[] = Array.isArray(json.categories) ? json.categories : [];
                if (cancelled) return;
                setCategories(list);
            } catch {
                // DB 호출 실패 시 빈 목록으로 — 하드코딩 폴백을 두지 않아 운영 변경이 즉시 반영된다.
                if (!cancelled) setCategories([]);
            }
        }

        void loadCategories();
        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        void fetchAssets(page);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, pageSize, category, search]);

    async function fetchAssets(nextPage: number) {
        try {
            setError(null);
            setLoading(true);

            const params = new URLSearchParams({
                page: String(nextPage),
                pageSize: String(pageSize),
                assetState: 'APPROVED',
                // 배포 완료(ASSET_URL LIKE '/deployed/%') 자산만 반환 — totalCount 도 동일 조건 기준
                deployedOnly: 'true',
            });

            if (category) {
                params.set('category', category);
            }
            if (search) {
                params.set('search', search);
            }

            const res = await fetch(nextApi(`/api/assets?${params.toString()}`), { cache: 'no-store' });
            const json = await res.json();
            const raw: AssetItem[] = Array.isArray(json.assets) ? json.assets : [];
            // 숨김 처리된 자산(USE_YN='N')은 서버 SQL 에서도 걸러지지만 캐시·지연 반영 대비 이중 필터링
            const list = raw.filter((item) => !item.useYn || item.useYn === 'Y');
            const nextTotalCount = Number(json.totalCount ?? 0);

            setAssets(list);
            setTotalCount(nextTotalCount);
        } catch {
            setError('이미지 목록을 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    function moveToFirstPage() {
        setPage(1);
    }

    function handleCategoryChange(nextCategory: string) {
        setCategory(nextCategory);
        moveToFirstPage();
    }

    function handleSearchSubmit() {
        setSearch(draftSearch.trim());
        moveToFirstPage();
    }

    function handleReset() {
        setCategory('');
        setDraftSearch('');
        setSearch('');
        setPageSize(20);
        setPage(1);
        setFolder('');
    }

    // 단일 선택 토글 — 같은 이미지를 다시 누르면 선택 해제, 다른 이미지를 누르면 그것으로 교체
    function handleSelect(url: string | null) {
        if (!url) return;
        setSelectedUrl((prev) => (prev === url ? null : url));
    }

    // 우측 상단 X — 팝업 창 또는 iframe 모달 닫기
    // - 팝업(window.opener 존재) → window.close()
    // - iframe(window.parent !== window) → 부모에 PICKER_CLOSE postMessage 전송
    // window.history.back()은 사용하지 않음:
    //   Next.js SSR 인증 redirect 등으로 opener가 null이 되는 경우,
    //   history.back()이 에디터 메인 창이 아닌 팝업 창의 히스토리를 이동시켜
    //   에디터 화면으로 돌아오지 못하는 버그가 생긴다.
    function handleClose() {
        if (typeof window === 'undefined') return;

        // 팝업 창 — 브라우저가 opener를 신뢰하는 경우
        if (window.opener) {
            window.close();
            return;
        }

        // iframe 모달 — 부모(EditClient)에게 닫기 요청
        if (window.parent && window.parent !== window) {
            window.parent.postMessage({ type: 'PICKER_CLOSE' }, '*');
            return;
        }

        // 단독 창으로 접근 시 fallback — close() 시도
        window.close();
    }

    function handleConfirm() {
        if (!selectedUrl) return;

        // 에디터가 <img src>로 바로 사용하므로 assetOrigin이 붙은 완전 해석 URL을 전달.
        // DB 원본 URL은 `/deployed/static/<file>` 같은 루트 상대 경로인데, 로컬 dev에선
        // localhost 에 그 경로가 없어 404가 나 편집 화면에서 이미지가 깨지거나 사라져 보인다.
        // resolveAssetSrc()가 절대 URL로 변환해주므로 dev/prod 모두 동일하게 동작한다.
        const outboundUrl = resolveAssetSrc(selectedUrl, assetOrigin) ?? selectedUrl;

        // cms-file-picker는 ASSETS_SELECTED.urls[0] 만 읽으므로 배열 1건으로 전달해 기존 consumer 호환 유지
        const msg = { type: 'ASSETS_SELECTED', urls: [outboundUrl] };
        if (window.opener) {
            window.opener.postMessage(msg, '*');
            window.close();
            return;
        }

        if (window.parent && window.parent !== window) {
            window.parent.postMessage(msg, '*');
        }
    }

    return (
        // 루트는 iframe/뷰포트 정확히 맞추기 — 상단 검색 바와 하단 페이지네이션은 고정,
        // 이미지 그리드만 내부 스크롤되도록 flex column 구조로 고정.
        <div className="flex h-screen overflow-hidden bg-slate-50">
            {/* ── 좌측 사이드바 — 이미지가 있는 폴더 리스트 ───────────── */}
            <aside
                className={`shrink-0 overflow-hidden border-r border-slate-200 bg-white transition-[width] duration-300 ease-in-out ${
                    sidebarOpen ? 'w-56' : 'w-0'
                }`}
            >
                <div className="h-full w-56 overflow-y-auto p-4">
                    <div className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-500">폴더</div>
                    <ul className="space-y-1">
                        <li>
                            <button
                                type="button"
                                onClick={() => setFolder('')}
                                className={`w-full rounded px-3 py-2 text-left text-sm ${
                                    folder === ''
                                        ? 'bg-[#EBF4FF] font-semibold text-[#0046A4]'
                                        : 'text-slate-700 hover:bg-slate-50'
                                }`}
                            >
                                전체
                            </button>
                        </li>
                        {folders.map((name) => (
                            <li key={name}>
                                <button
                                    type="button"
                                    onClick={() => setFolder(name)}
                                    className={`flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm ${
                                        folder === name
                                            ? 'bg-[#EBF4FF] font-semibold text-[#0046A4]'
                                            : 'text-slate-700 hover:bg-slate-50'
                                    }`}
                                >
                                    <svg
                                        className="h-4 w-4 shrink-0"
                                        fill="none"
                                        viewBox="0 0 24 24"
                                        stroke="currentColor"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"
                                        />
                                    </svg>
                                    <span className="truncate">{name}</span>
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>
            </aside>

            <div className="flex min-w-0 flex-1 flex-col px-4 py-6 md:px-8">
                {/* 상단 바 — 햄버거(왼쪽, 폴더 사이드바 토글) + 닫기 X (오른쪽) */}
                <div className="mb-3 flex shrink-0 items-center justify-between gap-1">
                    <button
                        type="button"
                        onClick={() => setSidebarOpen((prev) => !prev)}
                        className="rounded p-2 text-slate-600 hover:bg-slate-100"
                        title="카테고리 패널 토글"
                        aria-label="카테고리 패널 토글"
                    >
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M4 6h16M4 12h16M4 18h16"
                            />
                        </svg>
                    </button>
                    <button
                        type="button"
                        onClick={handleClose}
                        className="rounded p-2 text-slate-600 hover:bg-slate-100"
                        title="닫기"
                        aria-label="닫기"
                    >
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M6 18L18 6M6 6l12 12"
                            />
                        </svg>
                    </button>
                </div>

                {/* 검색 조건 카드 — cms-admin/asset-approvals 스타일 */}
                <div className="mb-3 shrink-0 rounded-md border border-slate-200 bg-white p-2">
                    <div className="flex flex-wrap items-center gap-2">
                        <label className="m-0 text-xs font-medium text-slate-700">카테고리</label>
                        <select
                            value={category}
                            onChange={(e) => handleCategoryChange(e.target.value)}
                            className="h-8 w-[130px] rounded border border-slate-300 bg-white px-2 text-xs"
                        >
                            <option value="">전체</option>
                            {categories.map((item) => (
                                <option key={item.code} value={item.code}>
                                    {item.codeName}
                                </option>
                            ))}
                        </select>

                        <input
                            type="text"
                            value={draftSearch}
                            onChange={(e) => setDraftSearch(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    handleSearchSubmit();
                                }
                            }}
                            placeholder="이미지명으로 검색"
                            className="ml-2 h-8 w-[200px] rounded border border-slate-300 px-2 text-xs"
                        />

                        <div className="flex-1" />

                        <select
                            value={pageSize}
                            onChange={(e) => {
                                setPageSize(Number(e.target.value));
                                moveToFirstPage();
                            }}
                            className="h-8 w-[72px] rounded border border-slate-300 bg-white px-2 text-xs"
                        >
                            {PAGE_SIZE_OPTIONS.map((option) => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
                        </select>
                        <label className="m-0 mr-1 text-xs font-medium text-slate-700">건씩</label>

                        <button
                            type="button"
                            onClick={handleSearchSubmit}
                            className="h-8 rounded bg-[#0046A4] px-3 text-xs font-medium text-white hover:bg-[#003399]"
                        >
                            조회
                        </button>
                        <button
                            type="button"
                            onClick={handleReset}
                            className="h-8 rounded border border-slate-300 bg-white px-3 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                            초기화
                        </button>
                        <button
                            type="button"
                            onClick={handleConfirm}
                            disabled={!selectedUrl}
                            className="h-8 rounded bg-[#0046A4] px-3 text-xs font-medium text-white hover:bg-[#003399] disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            선택 완료
                        </button>
                    </div>
                </div>

                {/* 건수 */}
                <div className="mb-3 flex shrink-0 justify-end text-xs text-slate-500">
                    <p>
                        총 {totalCount}건, {page} / {totalPages} 페이지
                    </p>
                </div>

                {/* 스크롤 영역 — 이미지 그리드(또는 로딩/에러/빈 상태)만 내부 스크롤 */}
                <div className="flex-1 overflow-y-auto">
                    {loading ? (
                        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center text-slate-500">
                            이미지 목록을 불러오는 중입니다.
                        </div>
                    ) : error ? (
                        <div className="rounded-2xl border border-red-200 bg-red-50 p-12 text-center text-red-600">
                            {error}
                        </div>
                    ) : visibleAssets.length === 0 ? (
                        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center text-slate-500">
                            조건에 맞는 이미지가 없습니다.
                        </div>
                    ) : (
                        // 고정폭 카드 — auto-fill로 가용 폭에 맞춰 열 개수만 바뀌고 개별 카드 크기는 일정
                        <div className="grid grid-cols-[repeat(auto-fill,180px)] justify-start gap-4">
                            {visibleAssets.map((asset) => {
                                const isSelected = asset.url ? selectedUrl === asset.url : false;
                                // 카테고리 라벨은 FWK_CODE codeName 만 사용. 매핑이 없으면 '-' (코드값 노출 금지).
                                const label = (asset.businessCategory && categoryMap[asset.businessCategory]) || '-';

                                return (
                                    <button
                                        key={asset.assetId}
                                        type="button"
                                        onClick={() => handleSelect(asset.url)}
                                        className={`overflow-hidden rounded-2xl border bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${
                                            isSelected
                                                ? 'border-[#0046A4] ring-2 ring-[#0046A4]/20'
                                                : 'border-slate-200'
                                        }`}
                                    >
                                        <div className="aspect-square bg-slate-100">
                                            {asset.url ? (
                                                // eslint-disable-next-line @next/next/no-img-element
                                                <img
                                                    src={resolveAssetSrc(asset.url, assetOrigin)}
                                                    alt={asset.assetName}
                                                    className="h-full w-full object-cover"
                                                />
                                            ) : (
                                                <div className="flex h-full items-center justify-center text-sm text-slate-400">
                                                    미리보기 없음
                                                </div>
                                            )}
                                        </div>
                                        <div className="space-y-1.5 p-3">
                                            <div className="flex items-center justify-between gap-2">
                                                <span className="inline-flex rounded-full bg-slate-100 px-2 py-1 text-[11px] font-medium text-slate-600">
                                                    {label}
                                                </span>
                                                {isSelected ? (
                                                    <span className="text-[11px] font-semibold text-[#0046A4]">
                                                        선택됨
                                                    </span>
                                                ) : null}
                                            </div>
                                            <div className="truncate text-sm font-semibold text-slate-900">
                                                {asset.assetName}
                                            </div>
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* 페이지네이션 — 1페이지 상태에서도 노출 (이전/다음 버튼 자동 disabled)
                    단, 로딩/에러/빈 목록 상태에서는 숨김 — 시각적 혼선 방지 */}
                {!loading && !error && visibleAssets.length > 0 ? (
                    <div className="mt-6 flex shrink-0 justify-center gap-1">
                        <button
                            type="button"
                            onClick={() => setPage(Math.max(1, page - 1))}
                            disabled={page === 1}
                            className={`px-3.5 py-1.5 rounded-md border border-[#e5e7eb] text-[13px] ${
                                page === 1
                                    ? 'bg-[#f9fafb] text-[#d1d5db] cursor-not-allowed'
                                    : 'bg-white text-[#374151] cursor-pointer'
                            }`}
                        >
                            이전
                        </button>

                        {visiblePages.map((visiblePage) => (
                            <button
                                key={visiblePage}
                                type="button"
                                onClick={() => setPage(visiblePage)}
                                className={`px-3 py-1.5 rounded-md border text-[13px] cursor-pointer ${
                                    page === visiblePage
                                        ? 'border-[#0046A4] bg-[#0046A4] text-white font-semibold'
                                        : 'border-[#e5e7eb] bg-white text-[#374151] font-normal'
                                }`}
                            >
                                {visiblePage}
                            </button>
                        ))}

                        <button
                            type="button"
                            onClick={() => setPage(Math.min(totalPages, page + 1))}
                            disabled={page === totalPages}
                            className={`px-3.5 py-1.5 rounded-md border border-[#e5e7eb] text-[13px] ${
                                page === totalPages
                                    ? 'bg-[#f9fafb] text-[#d1d5db] cursor-not-allowed'
                                    : 'bg-white text-[#374151] cursor-pointer'
                            }`}
                        >
                            다음
                        </button>
                    </div>
                ) : null}
            </div>
        </div>
    );
}
