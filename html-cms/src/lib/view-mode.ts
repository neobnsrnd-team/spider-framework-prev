import type { CmsPageViewMode } from '@/lib/cms-page-create';

export const LEGACY_PC_VIEW_MODE = 'PC';

export function isDesktopWebViewMode(value: unknown): boolean {
    return value === 'web' || value === LEGACY_PC_VIEW_MODE;
}

export function normalizeCmsViewMode(value: unknown, fallback: CmsPageViewMode = 'mobile'): CmsPageViewMode {
    if (isDesktopWebViewMode(value)) return 'web';
    if (value === 'responsive') return 'responsive';
    if (value === 'mobile') return 'mobile';
    return fallback;
}

export function isTemplateCompatibleViewMode(
    selectedViewMode: string | undefined,
    templateViewMode: string | undefined,
): boolean {
    return (
        !selectedViewMode ||
        templateViewMode === selectedViewMode ||
        (selectedViewMode === 'web' && templateViewMode === LEGACY_PC_VIEW_MODE)
    );
}
