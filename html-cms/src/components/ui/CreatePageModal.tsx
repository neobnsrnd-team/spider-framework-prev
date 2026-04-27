'use client';

import { useEffect, useState } from 'react';

import { createCmsPage, type CmsPageViewMode } from '@/lib/cms-page-create';
import { nextApi } from '@/lib/api-url';

import Modal from './Modal';

interface PageTemplateOption {
    id: string;
    label: string;
    description: string;
    viewMode?: string;
}

const VIEW_MODE_OPTIONS: Array<{
    id: CmsPageViewMode;
    label: string;
    icon: string;
    description: string;
}> = [
    { id: 'mobile', label: '모바일', icon: '📱', description: '390px 고정 너비\n모바일 앱 화면에 최적화' },
    { id: 'web', label: '웹', icon: '🖥️', description: '1280px 고정 너비\n데스크톱 웹 페이지용' },
    { id: 'responsive', label: '반응형', icon: '🔄', description: '전체 너비 사용\n모든 기기에 대응' },
];

const BLANK_TEMPLATE: PageTemplateOption = {
    id: 'blank',
    label: '빈 페이지',
    description: '아무 컴포넌트도 없는 빈 화면에서 시작합니다.',
};

export interface CreatePageModalProps {
    onClose: () => void;
    canWrite: boolean;
}

export default function CreatePageModal({ onClose, canWrite }: CreatePageModalProps) {
    const [pageName, setPageName] = useState('');
    const [viewMode, setViewMode] = useState<CmsPageViewMode>('mobile');
    const [templateId, setTemplateId] = useState('blank');
    const [templateOpen, setTemplateOpen] = useState(true);
    const [templates, setTemplates] = useState<PageTemplateOption[]>([BLANK_TEMPLATE]);
    const [templatesLoading, setTemplatesLoading] = useState(true);
    const [templatesError, setTemplatesError] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);

    function matchesTemplateViewMode(templateViewMode: string | undefined, selectedViewMode: CmsPageViewMode) {
        return !templateViewMode
            || templateViewMode === selectedViewMode
            || (selectedViewMode === 'web' && templateViewMode === 'PC');
    }

    useEffect(() => {
        let cancelled = false;

        async function loadTemplates() {
            setTemplatesLoading(true);
            setTemplatesError(null);
            try {
                const res = await fetch(nextApi('/api/builder/templates'));
                const data = await res.json();
                if (!res.ok || !data.ok) {
                    throw new Error(data.error || '템플릿 목록을 불러오지 못했습니다.');
                }
                if (cancelled) return;

                const nextTemplates = Array.isArray(data.templates)
                    ? data.templates.map((template: { pageId: string; pageName: string; viewMode: string }) => ({
                          id: template.pageId,
                          label: template.pageName,
                          description: '미리 구성된 템플릿으로 시작합니다.',
                          viewMode: template.viewMode,
                      }))
                    : [];

                setTemplates([BLANK_TEMPLATE, ...nextTemplates]);
            } catch (err: unknown) {
                if (cancelled) return;
                setTemplates([BLANK_TEMPLATE]);
                setTemplatesError(err instanceof Error ? err.message : '템플릿 목록을 불러오지 못했습니다.');
            } finally {
                if (!cancelled) {
                    setTemplatesLoading(false);
                }
            }
        }

        void loadTemplates();
        return () => {
            cancelled = true;
        };
    }, []);

    const filteredTemplates = templates.filter((template) => matchesTemplateViewMode(template.viewMode, viewMode));
    const selectedTemplate = filteredTemplates.find((template) => template.id === templateId) ?? BLANK_TEMPLATE;

    useEffect(() => {
        if (!filteredTemplates.some((template) => template.id === templateId)) {
            setTemplateId('blank');
        }
    }, [filteredTemplates, templateId]);

    async function handleCreate() {
        const trimmedName = pageName.trim();
        if (!trimmedName || creating || !canWrite) return;

        setCreating(true);
        try {
            const created = await createCmsPage({
                pageName: trimmedName,
                viewMode,
                templateId,
            });
            window.location.href = created.editorUrl;
        } catch (err: unknown) {
            console.error('페이지 생성 실패:', err);
            alert(err instanceof Error ? err.message : '페이지 생성에 실패했습니다.');
            setCreating(false);
        }
    }

    return (
        <Modal title="새 페이지 만들기" onClose={onClose} showCloseButton={false} width="480px" className="p-8">
            <label className="block text-[13px] font-semibold text-[#374151] mb-1.5">페이지 이름</label>
            <input
                autoFocus
                value={pageName}
                onChange={(e) => setPageName(e.target.value)}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' && pageName.trim()) {
                        void handleCreate();
                    }
                }}
                placeholder="예: 메인 페이지"
                className="w-full h-10 box-border px-3 rounded-lg border border-[#d1d5db] text-sm outline-none"
            />

            <label className="block text-[13px] font-semibold text-[#374151] mt-5 mb-2.5">레이아웃 선택</label>
            <div className="flex gap-3">
                {VIEW_MODE_OPTIONS.map((option) => {
                    const selected = viewMode === option.id;
                    return (
                        <button
                            key={option.id}
                            type="button"
                            onClick={() => setViewMode(option.id)}
                            className={`flex-1 px-3 py-4 rounded-xl border-2 text-center cursor-pointer transition-all ${
                                selected
                                    ? 'border-[#0046A4] bg-[#f0f4ff] text-[#0046A4]'
                                    : 'border-[#e5e7eb] bg-white text-[#374151]'
                            }`}
                        >
                            <span className="block text-[32px] mb-2">{option.icon}</span>
                            <span className="block text-sm font-bold mb-1.5">{option.label}</span>
                            <span className="block text-[11px] leading-[1.4] text-[#6b7280] whitespace-pre-line">
                                {option.description}
                            </span>
                        </button>
                    );
                })}
            </div>

            <p className="text-[11px] text-[#9ca3af] mt-3 mb-0 leading-[1.4]">
                레이아웃은 페이지 생성 후 변경할 수 없습니다.
            </p>

            <div className="mt-[22px]">
                <button
                    type="button"
                    onClick={() => setTemplateOpen((open) => !open)}
                    className="w-full flex items-center justify-between px-[14px] py-3 rounded-lg border border-[#e5e7eb] bg-white cursor-pointer"
                >
                    <span className="text-[13px] font-bold text-[#374151]">템플릿 선택</span>
                    <span className="flex items-center gap-2 text-xs text-[#6b7280]">
                        <span className="max-w-[180px] overflow-hidden text-ellipsis whitespace-nowrap">
                            {selectedTemplate.label}
                        </span>
                        <span
                            className="transition-transform"
                            style={{ transform: templateOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}
                        >
                            ▼
                        </span>
                    </span>
                </button>

                {templateOpen && (
                    <div className="mt-2 flex flex-col gap-2 max-h-[220px] overflow-y-auto pr-1">
                        {filteredTemplates.map((template) => {
                            const selected = templateId === template.id;
                            return (
                                <button
                                    key={template.id}
                                    type="button"
                                    onClick={() => setTemplateId(template.id)}
                                    className={`w-full flex gap-[10px] items-start px-[14px] py-3 rounded-lg text-left cursor-pointer ${
                                        selected
                                            ? 'border-2 border-[#0046A4] bg-[#f0f4ff] text-[#374151]'
                                            : 'border border-[#e5e7eb] bg-white text-[#374151]'
                                    }`}
                                >
                                    <span
                                        className="w-4 h-4 mt-[1px] rounded-full bg-white shrink-0"
                                        style={{ border: selected ? '5px solid #0046A4' : '1px solid #d1d5db' }}
                                    />
                                    <span>
                                        <span
                                            className={`block text-[13px] font-bold ${
                                                selected ? 'text-[#0046A4]' : 'text-[#111827]'
                                            }`}
                                        >
                                            {template.label}
                                        </span>
                                        <span className="block mt-[3px] text-[11px] leading-[1.4] text-[#6b7280]">
                                            {template.description}
                                        </span>
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                )}

                {templatesLoading && <p className="text-xs text-[#9ca3af] mt-2 mb-0">템플릿 목록을 불러오는 중...</p>}
                {templatesError && <p className="text-xs text-[#dc2626] mt-2 mb-0">{templatesError}</p>}
            </div>

            <div className="flex justify-end gap-2 mt-6">
                <button
                    onClick={onClose}
                    className="px-5 py-2 rounded-lg border border-[#e5e7eb] bg-white text-[#374151] text-sm cursor-pointer"
                >
                    취소
                </button>
                <button
                    onClick={() => void handleCreate()}
                    disabled={!pageName.trim() || creating || !canWrite}
                    className={`px-5 py-2 rounded-lg text-sm font-semibold text-white ${
                        !pageName.trim() || creating || !canWrite
                            ? 'bg-[#93c5fd] cursor-not-allowed'
                            : 'bg-[#0046A4] cursor-pointer'
                    }`}
                >
                    {creating ? '생성 중...' : '만들기'}
                </button>
            </div>
        </Modal>
    );
}
