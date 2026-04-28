// scripts/migrate-popup-banner.ts
// 하단 슬라이드 이미지 팝업(Bottom Sheet) 플러그인 컴포넌트 DB 등록 (Issue #281)
// 실행: npx tsx scripts/migrate-popup-banner.ts

import 'dotenv/config';
import { getComponentById, updateComponent, createComponent } from '../src/db/repository/component.repository';
import { closePool } from '../src/db/connection';

const THUMBNAIL = '/assets/minimalist-blocks/preview/ibk-popup-banner.svg';

/** 기본 이미지 데이터 — 빈 배열로 시작하여 편집 모달에서 이미지를 직접 추가하도록 유도 */
const DEFAULT_IMAGES = JSON.stringify([]);

/** 뷰 모드별 초기 HTML — data-component-id 루트 + data-cb-type(ContentBuilder 플러그인 식별용) */
function buildHtml(viewMode: string): string {
    return `<div data-component-id="popup-banner-${viewMode}" data-cb-type="popup-banner" data-spw-block data-images='${DEFAULT_IMAGES}' data-hide-days="3" data-view-mode="${viewMode}" style="min-height:40px;"></div>`;
}

const VARIANTS: Array<{
    id: string;
    viewMode: 'mobile' | 'web' | 'responsive';
    label: string;
    description: string;
}> = [
    {
        id: 'popup-banner-mobile',
        viewMode: 'mobile',
        label: '이미지 팝업 배너',
        description: '하단에서 올라오는 이미지 슬라이드 팝업 (모바일)',
    },
    {
        id: 'popup-banner-web',
        viewMode: 'web',
        label: '이미지 팝업 배너',
        description: '하단에서 올라오는 이미지 슬라이드 팝업 (웹)',
    },
    {
        id: 'popup-banner-responsive',
        viewMode: 'responsive',
        label: '이미지 팝업 배너',
        description: '하단에서 올라오는 이미지 슬라이드 팝업 (반응형)',
    },
];

async function main(): Promise<void> {
    for (const variant of VARIANTS) {
        const html = buildHtml(variant.viewMode);
        const existing = await getComponentById(variant.id);

        if (existing) {
            await updateComponent({
                componentId:        variant.id,
                componentType:      existing.COMPONENT_TYPE,
                viewMode:           existing.VIEW_MODE,
                componentThumbnail: existing.COMPONENT_THUMBNAIL ?? undefined,
                data: {
                    ...(existing.DATA ?? {}) as Record<string, unknown>,
                    html,
                },
                lastModifierId: 'system',
            });
            process.stdout.write(`[업데이트] ${variant.id}\n`);
        } else {
            await createComponent({
                componentId:        variant.id,
                componentType:      'finance',
                viewMode:           variant.viewMode,
                componentThumbnail: THUMBNAIL,
                data: {
                    id:          variant.id.replace(`-${variant.viewMode}`, ''),
                    label:       variant.label,
                    description: variant.description,
                    preview:     THUMBNAIL,
                    html,
                    viewMode:    variant.viewMode,
                },
                createUserId:   'system',
                createUserName: '시스템',
            });
            process.stdout.write(`[생성] ${variant.id}\n`);
        }
    }

    await closePool();
    process.stdout.write('완료\n');
}

main().catch((err: unknown) => {
    process.stderr.write(`실패: ${err}\n`);
    process.exit(1);
});
