// src/components/dashboard/DashboardClient.tsx
'use client';

import { useState, useEffect, useRef, useTransition } from 'react';
import { useRouter } from 'next/navigation';

import CreatePageModal from '@/components/ui/CreatePageModal';
import PageCard, { APPROVE_DEFAULT_LABELS, formatDate } from '@/components/ui/PageCard';
import type { ViewMode, ApproveStateValue } from '@/components/ui/PageCard';
import { nextApi } from '@/lib/api-url';

import ApprovalRequestModal from './ApprovalRequestModal';
import RejectedReasonModal from './RejectedReasonModal';

// 정렬 옵션 목록
const SORT_OPTIONS: { value: SortBy; label: string }[] = [
    { value: 'date', label: '최신 수정순' },
    { value: 'name', label: '이름순' },
];

function displayDate(date: string | null): string {
    return date || '-';
}

type SortBy = 'date' | 'name';

export interface DashboardPageCard {
    id: string;
    label: string;
    viewMode: ViewMode;
    thumbnail: string | null;
    lastModifiedDtime: string | null;
    approveState: ApproveStateValue;
    beginningDate: string | null;
    expiredDate: string | null;
    rejectedReason: string | null;
    hasFile: boolean;
    isExpired: boolean;
    isPublic: string;
}

export interface DashboardClientProps {
    userId: string;
    initialPages: DashboardPageCard[];
    totalCount: number;
    currentPage: number;
    search: string;
    sortBy: SortBy;
    viewMode: ViewMode | null;
    canWrite: boolean;
    /** FWK_CODE 조회 승인 상태 레이블 (서버에서 전달) */
    approveLabels?: Partial<Record<ApproveStateValue, string>>;
}

const PAGE_SIZE = 12;

// 뷰 모드 필터 옵션
const VIEW_MODE_FILTERS: { value: ViewMode | null; label: string }[] = [
    { value: null, label: '전체' },
    { value: 'mobile', label: '모바일' },
    { value: 'web', label: '웹' },
    { value: 'responsive', label: '반응형' },
];

export default function DashboardClient({
    userId,
    initialPages,
    totalCount,
    currentPage,
    search: initialSearch,
    sortBy: initialSortBy,
    viewMode: initialViewMode,
    canWrite,
    approveLabels: initialApproveLabels,
}: DashboardClientProps) {
    const router = useRouter();
    const [isPending, startTransition] = useTransition();

    // 검색·정렬·필터는 로컬 상태로 관리 후 URL 반영
    const [search, setSearch] = useState(initialSearch);
    const [sortBy, setSortBy] = useState<SortBy>(initialSortBy);
    const [viewModeFilter, setViewModeFilter] = useState<ViewMode | null>(initialViewMode);
    const [showExpired, setShowExpired] = useState(false);

    // 삭제 후 낙관적 업데이트용 로컬 페이지 목록
    const [pages, setPages] = useState<DashboardPageCard[]>(initialPages);
    const [localTotalCount, setLocalTotalCount] = useState(totalCount);

    // 승인 상태 레이블 (기본값과 서버 전달값 병합)
    const approveLabels = { ...APPROVE_DEFAULT_LABELS, ...initialApproveLabels };

    // 서버에서 새 데이터가 내려올 때 동기화
    useEffect(() => {
        setPages(initialPages);
        setLocalTotalCount(totalCount);
    }, [initialPages, totalCount]);

    // 승인 요청 모달
    const [approvalTarget, setApprovalTarget] = useState<DashboardPageCard | null>(null);
    const [rejectedTarget, setRejectedTarget] = useState<DashboardPageCard | null>(null);

    // 새 페이지 생성 모달
    const [showCreateModal, setShowCreateModal] = useState(false);

    const [sortDropdownOpen, setSortDropdownOpen] = useState(false);
    const sortDropdownRef = useRef<HTMLDivElement>(null);
    const searchDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // 드롭다운 외부 클릭 시 닫기
    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (sortDropdownRef.current && !sortDropdownRef.current.contains(e.target as Node)) {
                setSortDropdownOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // URL 업데이트 헬퍼 — searchParams 변경 시 서버 컴포넌트 재렌더링 유도
    function navigate(params: { page?: number; search?: string; sortBy?: SortBy; viewMode?: ViewMode | null }) {
        const sp = new URLSearchParams();
        const nextPage = params.page ?? 1;
        const nextSearch = params.search ?? search;
        const nextSortBy = params.sortBy ?? sortBy;
        const nextViewMode = 'viewMode' in params ? params.viewMode : viewModeFilter;

        if (nextPage > 1) sp.set('page', String(nextPage));
        if (nextSearch) sp.set('search', nextSearch);
        if (nextSortBy !== 'date') sp.set('sortBy', nextSortBy);
        if (nextViewMode) sp.set('viewMode', nextViewMode);

        const query = sp.toString();
        startTransition(() => {
            router.push(`/dashboard${query ? `?${query}` : ''}`);
        });
    }

    // 검색어 디바운스 (300ms) → URL 반영
    useEffect(() => {
        if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
        searchDebounceRef.current = setTimeout(() => {
            navigate({ page: 1, search, sortBy });
        }, 300);
        return () => {
            if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [search]);

    // 정렬 변경 → URL 반영
    function handleSortChange(value: SortBy) {
        setSortBy(value);
        navigate({ page: 1, search, sortBy: value });
    }

    // 뷰 모드 필터 변경 (동일 값 재클릭 시 해제 → 전체)
    function handleViewModeChange(value: ViewMode | null) {
        const next = viewModeFilter === value ? null : value;
        setViewModeFilter(next);
        navigate({ page: 1, search, sortBy, viewMode: next });
    }

    // 페이지 이동 → URL 반영
    function handlePageChange(page: number) {
        navigate({ page, search, sortBy });
    }

    // 승인 요청 — 낙관적 업데이트 후 API 호출
    async function handleApprovalRequest(
        approverId: string,
        approverName: string,
        beginningDate: string,
        expiredDate: string,
    ) {
        if (!approvalTarget || !canWrite) return;

        const { id: targetId, approveState: originalApproveState } = approvalTarget;

        // 낙관적 업데이트
        setPages((prev) => prev.map((p) => (p.id === targetId ? { ...p, approveState: 'PENDING' } : p)));
        setApprovalTarget(null);

        try {
            const res = await fetch(nextApi(`/api/builder/pages/${encodeURIComponent(targetId)}/approve-request`), {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approverId, approverName, beginningDate, expiredDate }),
            });
            const data = await res.json();
            if (!data.ok) {
                // 실패 시 해당 카드만 원래 상태로 롤백
                setPages((prev) =>
                    prev.map((p) => (p.id === targetId ? { ...p, approveState: originalApproveState } : p)),
                );
            }
        } catch (err: unknown) {
            console.error('승인 요청 실패:', err);
            setPages((prev) => prev.map((p) => (p.id === targetId ? { ...p, approveState: originalApproveState } : p)));
        }
    }

    // 페이지 삭제 — 낙관적 업데이트 후 API 호출, 성공 시 서버 재조회로 다음 페이지 항목 보충
    async function handleDeletePage(pageId: string, label: string) {
        if (!confirm(`'${label}' 페이지를 삭제하시겠습니까?\n저장된 내용도 함께 삭제됩니다.`)) return;

        // 낙관적 업데이트 (API 응답 전 즉시 피드백)
        setPages((prev) => prev.filter((p) => p.id !== pageId));
        setLocalTotalCount((prev) => prev - 1);

        try {
            const res = await fetch(nextApi(`/api/builder/pages?pageId=${encodeURIComponent(pageId)}`), {
                method: 'DELETE',
            });
            const data = await res.json();
            if (!data.ok) {
                // 실패 시 서버 데이터로 복구
                setPages(initialPages);
                setLocalTotalCount(totalCount);
                return;
            }
            // 성공 시 서버 재조회 — 다음 페이지 항목을 당겨와 빈 자리 보충
            // 현재 페이지의 마지막 항목을 삭제한 경우 이전 페이지로 이동
            const nextPage = pages.length === 1 && currentPage > 1 ? currentPage - 1 : currentPage;
            navigate({ page: nextPage, search, sortBy });
        } catch (err: unknown) {
            console.error('페이지 삭제 실패:', err);
            setPages(initialPages);
            setLocalTotalCount(totalCount);
        }
    }

    function handleModalClose() {
        setShowCreateModal(false);
    }

    const totalPages = Math.ceil(localTotalCount / PAGE_SIZE);

    return (
        <div className="min-h-screen bg-[#f8faff]">
            {/* ── 헤더 ── */}
            <header className="bg-white border-b border-[#e5e7eb] px-8 h-[60px] flex items-center gap-3 shadow-[0_1px_4px_rgba(0,0,0,0.06)] sticky top-0 z-[100]">
                <span className="font-bold text-base text-[#0046A4] tracking-[-0.3px]">Springware CMS</span>
                <span className="text-[#d1d5db] text-sm">/</span>
                <span className="text-sm text-[#374151]">대시보드</span>
            </header>

            {/* ── 본문 ── */}
            <main
                className={`max-w-[1280px] mx-auto px-8 pt-8 pb-16 transition-opacity duration-200 ${isPending ? 'opacity-60' : 'opacity-100'}`}
            >
                {/* ── 툴바: 타이틀 / 뷰모드 필터 / 검색 / 정렬 ── */}
                <div className="mb-7">
                    {/* 타이틀 행 */}
                    <div className="flex items-center justify-between mb-5 gap-3">
                        <div className="flex items-baseline gap-2.5">
                            <h1 className="m-0 text-[22px] font-bold text-[#111827]">대시보드</h1>
                            <span className="text-[13px] text-[#9ca3af]">{localTotalCount.toLocaleString()}개</span>
                        </div>
                        <button
                            onClick={() => setShowCreateModal(true)}
                            disabled={!canWrite}
                            className="inline-flex items-center gap-1.5 px-[18px] py-[9px] rounded-lg bg-[#0046A4] text-white border-0 text-[13px] font-semibold cursor-pointer whitespace-nowrap shrink-0"
                        >
                            + 새 페이지
                        </button>
                    </div>

                    {/* 뷰 모드 필터 버튼 */}
                    <div className="flex gap-1.5 mb-3">
                        {VIEW_MODE_FILTERS.map((f) => {
                            const active = viewModeFilter === f.value;
                            return (
                                <button
                                    key={String(f.value)}
                                    onClick={() => handleViewModeChange(f.value)}
                                    className={`px-[14px] py-[5px] rounded-full border text-[12px] cursor-pointer transition-all duration-150 ${
                                        active
                                            ? 'border-[#0046A4] bg-[#0046A4] text-white font-semibold'
                                            : 'border-[#e5e7eb] bg-white text-[#6b7280] font-normal'
                                    }`}
                                >
                                    {f.label}
                                </button>
                            );
                        })}
                        <button
                            onClick={() => setShowExpired(!showExpired)}
                            className={`px-[14px] py-[5px] rounded-full border text-[12px] cursor-pointer transition-all duration-150 ${
                                showExpired
                                    ? 'border-[#dc2626] bg-[#fef2f2] text-[#dc2626] font-semibold'
                                    : 'border-[#e5e7eb] bg-white text-[#6b7280] font-normal'
                            }`}
                        >
                            만료 포함
                        </button>
                    </div>

                    {/* 검색 + 정렬 행 */}
                    <div className="flex gap-2 items-center">
                        {/* 검색 인풋 */}
                        <div className="relative flex-1 max-w-[400px]">
                            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-[#9ca3af] pointer-events-none">
                                🔍
                            </span>
                            <input
                                type="text"
                                placeholder="페이지 이름 검색"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                className="w-full box-border py-[9px] pr-[14px] pl-9 rounded-lg border border-[#e5e7eb] text-[13px] outline-none bg-white text-[#111827]"
                            />
                        </div>

                        {/* 정렬 드롭다운 */}
                        <div ref={sortDropdownRef} className="relative shrink-0">
                            <button
                                onClick={() => setSortDropdownOpen((v) => !v)}
                                className={`inline-flex items-center gap-1.5 px-[14px] py-[9px] rounded-lg border text-[13px] cursor-pointer whitespace-nowrap transition-all duration-150 font-medium ${
                                    sortDropdownOpen
                                        ? 'border-[#0046A4] bg-[#f0f4ff] text-[#0046A4]'
                                        : 'border-[#e5e7eb] bg-white text-[#374151]'
                                }`}
                            >
                                {SORT_OPTIONS.find((o) => o.value === sortBy)?.label}
                                <svg
                                    width="12"
                                    height="12"
                                    viewBox="0 0 12 12"
                                    fill="none"
                                    className={`transition-transform duration-150 shrink-0 ${sortDropdownOpen ? 'rotate-180' : 'rotate-0'}`}
                                >
                                    <path
                                        d="M2 4l4 4 4-4"
                                        stroke="currentColor"
                                        strokeWidth="1.5"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    />
                                </svg>
                            </button>

                            {sortDropdownOpen && (
                                <div className="absolute top-[calc(100%+4px)] right-0 min-w-[140px] bg-white rounded-lg border border-[#e5e7eb] shadow-[0_4px_16px_rgba(0,0,0,0.08)] overflow-hidden z-50">
                                    {SORT_OPTIONS.map((opt) => (
                                        <button
                                            key={opt.value}
                                            onClick={() => {
                                                handleSortChange(opt.value);
                                                setSortDropdownOpen(false);
                                            }}
                                            className={`flex items-center justify-between w-full px-[14px] py-[9px] border-0 bg-white text-[13px] cursor-pointer text-left ${
                                                sortBy === opt.value
                                                    ? 'text-[#0046A4] font-semibold'
                                                    : 'text-[#374151] font-normal'
                                            }`}
                                        >
                                            {opt.label}
                                            {sortBy === opt.value && (
                                                <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                                                    <path
                                                        d="M2 6.5l3 3 6-6"
                                                        stroke="#0046A4"
                                                        strokeWidth="1.8"
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                    />
                                                </svg>
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* 카드 그리드 */}
                {pages.length === 0 ? (
                    <div className="text-center py-20 text-[#9ca3af] text-sm">
                        {search
                            ? `'${search}'에 대한 검색 결과가 없습니다.`
                            : '아직 페이지가 없습니다. 새 페이지를 만들어 보세요.'}
                    </div>
                ) : (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-5 mb-8">
                        {pages
                            .filter((p) => showExpired || !p.isExpired)
                            .map((page) => {
                                return (
                                    <PageCard
                                        key={page.id}
                                        page={page}
                                        approveLabels={approveLabels}
                                        onClick={() => {
                                            if (page.isExpired) {
                                                alert('만료된 페이지는 수정할 수 없습니다.');
                                                return;
                                            }
                                            if (!canWrite) {
                                                return;
                                            }
                                            window.location.href = nextApi(`/edit?bank=${page.id}`);
                                        }}
                                        overlay={{ label: '편집하기', color: 'rgba(0,70,164,0.45)' }}
                                        authorSlot={
                                            <div className="flex flex-col gap-1">
                                                <p className="m-0 text-[11px] text-[#9ca3af]">
                                                    수정: {formatDate(page.lastModifiedDtime)}
                                                </p>
                                                <p className="m-0 text-[11px] text-[#6b7280]">
                                                    노출: {displayDate(page.beginningDate)} ~{' '}
                                                    {displayDate(page.expiredDate)}
                                                </p>
                                            </div>
                                        }
                                        footerSlot={
                                            page.isExpired ? (
                                                <div className="px-4 py-2 border-t border-[#f3f4f6] flex justify-end">
                                                    <span className="text-xs text-[#dc2626] font-medium">
                                                        만료된 페이지
                                                    </span>
                                                </div>
                                            ) : (
                                                <div
                                                    className="px-4 py-2 border-t border-[#f3f4f6] flex justify-end gap-1.5"
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    {page.approveState === 'REJECTED' && (
                                                        <button
                                                            onClick={() => setRejectedTarget(page)}
                                                            className="px-2.5 py-1 rounded-md border border-[#fca5a5] bg-transparent text-[#dc2626] text-xs cursor-pointer"
                                                        >
                                                            반려 사유
                                                        </button>
                                                    )}
                                                    {canWrite &&
                                                        (page.approveState === 'WORK' ||
                                                            page.approveState === 'REJECTED' ||
                                                            page.approveState === 'APPROVED') && (
                                                            <button
                                                                onClick={() => setApprovalTarget(page)}
                                                                className="px-2.5 py-1 rounded-md border border-[#93c5fd] bg-transparent text-[#0046A4] text-xs cursor-pointer"
                                                            >
                                                                {page.approveState === 'APPROVED' ||
                                                                page.approveState === 'REJECTED'
                                                                    ? '재승인요청'
                                                                    : '승인 요청'}
                                                            </button>
                                                        )}
                                                    {canWrite && (
                                                        <button
                                                            onClick={() => handleDeletePage(page.id, page.label)}
                                                            className="px-2.5 py-1 rounded-md border border-[#fca5a5] bg-transparent text-[#dc2626] text-xs cursor-pointer"
                                                        >
                                                            삭제
                                                        </button>
                                                    )}
                                                </div>
                                            )
                                        }
                                    />
                                );
                            })}
                    </div>
                )}

                {/* 페이지네이션 */}
                {totalPages > 1 && (
                    <div className="flex justify-center gap-1">
                        <button
                            onClick={() => handlePageChange(Math.max(1, currentPage - 1))}
                            disabled={currentPage === 1}
                            className={`px-3.5 py-1.5 rounded-md border border-[#e5e7eb] text-[13px] ${
                                currentPage === 1
                                    ? 'bg-[#f9fafb] text-[#d1d5db] cursor-not-allowed'
                                    : 'bg-white text-[#374151] cursor-pointer'
                            }`}
                        >
                            이전
                        </button>

                        {Array.from({ length: Math.min(7, totalPages) }, (_, i) => {
                            const half = 3;
                            let start = Math.max(1, currentPage - half);
                            const end = Math.min(totalPages, start + 6);
                            start = Math.max(1, end - 6);
                            return start + i;
                        }).map((p) => (
                            <button
                                key={p}
                                onClick={() => handlePageChange(p)}
                                className={`px-3 py-1.5 rounded-md border text-[13px] cursor-pointer ${
                                    currentPage === p
                                        ? 'border-[#0046A4] bg-[#0046A4] text-white font-semibold'
                                        : 'border-[#e5e7eb] bg-white text-[#374151] font-normal'
                                }`}
                            >
                                {p}
                            </button>
                        ))}

                        <button
                            onClick={() => handlePageChange(Math.min(totalPages, currentPage + 1))}
                            disabled={currentPage === totalPages}
                            className={`px-3.5 py-1.5 rounded-md border border-[#e5e7eb] text-[13px] ${
                                currentPage === totalPages
                                    ? 'bg-[#f9fafb] text-[#d1d5db] cursor-not-allowed'
                                    : 'bg-white text-[#374151] cursor-pointer'
                            }`}
                        >
                            다음
                        </button>
                    </div>
                )}
            </main>

            {/* ── 승인 요청 모달 ── */}
            {approvalTarget && (
                <ApprovalRequestModal
                    page={approvalTarget}
                    onClose={() => setApprovalTarget(null)}
                    onSubmit={handleApprovalRequest}
                />
            )}

            {/* ── 반려 사유 확인 모달 ── */}
            {rejectedTarget && <RejectedReasonModal page={rejectedTarget} onClose={() => setRejectedTarget(null)} />}

            {/* ── 새 페이지 생성 모달 ── */}
            {showCreateModal && <CreatePageModal onClose={handleModalClose} canWrite={canWrite} />}
        </div>
    );
}
