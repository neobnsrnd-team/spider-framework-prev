/**
 * @file createBalanceToggle.ts
 * @description Figma BalanceToggle 컴포넌트 세트 생성.
 *
 * 잔액 숨김/표시 토글 컴포넌트를 2개의 variant로 구성한다.
 * - Hidden=False : 레이블 "숨기기", 비활성 pill (surfaceRaised), thumb 왼쪽
 * - Hidden=True  : 레이블 "보이기", 활성 pill (brand/primary), thumb 오른쪽
 *
 * React 대응 컴포넌트: packages/component-library/modules/common/BalanceToggle
 * React props: hidden(boolean), onToggle(() => void)
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../tokens';
import {
  createComponent,
  combineVariants,
  setAutoLayout,
  setPadding,
  setFillWithVar,
  clearFill,
  addTextWithVar,
  setFloatVar,
} from '../../../helpers';

/**
 * Hidden 상태에 따라 단일 BalanceToggle ComponentNode를 생성한다.
 *
 * @param hidden - true = 잔액 숨김(토글 활성), false = 잔액 표시(토글 비활성)
 */
async function createBalanceToggleVariant(hidden: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Hidden=${hidden ? 'True' : 'False'}`);

  /* 바깥 컨테이너: 세로 flex, 오른쪽 정렬 (React: flex-col items-end gap-xs) */
  setAutoLayout(comp, 'VERTICAL', SPACING.xs, 'MAX');
  await setFloatVar(comp, 'itemSpacing', SIZE_VAR.spacingXs, SPACING.xs);
  comp.primaryAxisSizingMode = 'AUTO';
  comp.counterAxisSizingMode = 'AUTO';
  clearFill(comp);

  /* 레이블 텍스트: hidden 상태에 따라 "보이기" / "숨기기" 전환 */
  await addTextWithVar(
    comp,
    hidden ? '보이기' : '숨기기',
    FONT_SIZE.xs,        // 12px — React: text-[10px] 의 최근접 토큰
    COLOR_VAR.textMuted, // Figma 변수 경로
    COLOR.textMuted,     // fallback RGB
    true,                // bold
    SIZE_VAR.fontSizeXs,
  );

  /* 토글 pill (React: w-12=48px, h-6=24px, rounded-full, p-1=4px) */
  const pill = figma.createFrame();
  pill.name = 'pill';
  setAutoLayout(pill, 'HORIZONTAL', 0);
  /* FIXED로 고정해야 resize(48, 24)가 유지됨.
   * AUTO(hug) 상태에서는 thumb 크기(16px) + padding(4+4)=24px로 축소되어
   * pill이 정사각형처럼 보이고 thumb 좌우 이동 공간이 사라진다. */
  pill.primaryAxisSizingMode = 'FIXED';
  pill.counterAxisSizingMode = 'FIXED';
  pill.resize(48, 24);
  await setFloatVar(pill, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full); // rounded-full
  setPadding(pill, SPACING.xs, SPACING.xs); // p-1 = 4px
  await setFloatVar(pill, 'paddingTop',    SIZE_VAR.spacingXs, SPACING.xs);
  await setFloatVar(pill, 'paddingRight',  SIZE_VAR.spacingXs, SPACING.xs);
  await setFloatVar(pill, 'paddingBottom', SIZE_VAR.spacingXs, SPACING.xs);
  await setFloatVar(pill, 'paddingLeft',   SIZE_VAR.spacingXs, SPACING.xs);

  /* pill 색상: hidden=True → 브랜드(활성), hidden=False → surfaceRaised(비활성) */
  if (hidden) {
    await setFillWithVar(pill, COLOR_VAR.brandPrimary, BRAND.primary);
  } else {
    await setFillWithVar(pill, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  }

  /* thumb 위치: FIXED 크기 덕분에 MAX/MIN이 48px 내 좌우 공간을 실제로 활용함
   * hidden=True → 오른쪽(MAX), hidden=False → 왼쪽(MIN) */
  pill.primaryAxisAlignItems = hidden ? 'MAX' : 'MIN';
  pill.counterAxisAlignItems = 'CENTER';

  /* thumb (React: size-4=16px, rounded-full, bg-surface) */
  const thumb = figma.createEllipse();
  thumb.name = 'thumb';
  thumb.resize(16, 16);
  await setFillWithVar(thumb, COLOR_VAR.surface, COLOR.surface);
  pill.appendChild(thumb);

  comp.appendChild(pill);
  return comp;
}

/**
 * BalanceToggle ComponentSet을 생성하고 캔버스에 추가한다.
 * Hidden=False / Hidden=True 2종 variant를 포함한다.
 *
 * @returns Figma ComponentSetNode ('BalanceToggle')
 */
export async function createBalanceToggle(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([
      createBalanceToggleVariant(false), // Hidden=False
      createBalanceToggleVariant(true),  // Hidden=True
    ]),
    'BalanceToggle',
    2, // 한 행에 2개 variant 나열
  );
}
