/**
 * @file createAppBrandHeader.ts
 * @description Figma AppBrandHeader 컴포넌트 생성.
 * 로그인·온보딩 화면 최상단 브랜드 로고 헤더.
 * BlankPageLayout과 함께 사용되며, 브랜드 이니셜 원형 배지 + 브랜드명을 중앙 정렬로 표시한다.
 * - 브랜드 이니셜 배지: 24×24px rounded-full, bg-brand, text-brand-fg
 * - 브랜드명: text-lg font-bold text-brand-text
 * 단일 variant.
 * 컴포넌트 이름: "AppBrandHeader"
 */
import { BRAND, COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../tokens';
import {
  createComponent, setAutoLayout, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../helpers';

const HEADER_WIDTH  = 390;
const HEADER_HEIGHT = 56; /* h-14 */

export async function createAppBrandHeader(): Promise<ComponentNode> {
  const comp = createComponent('Default');
  setAutoLayout(comp, 'HORIZONTAL', 0, 'CENTER');
  comp.primaryAxisAlignItems = 'CENTER';
  comp.resize(HEADER_WIDTH, HEADER_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* flex items-center gap-xs 내부 그룹 */
  const inner = figma.createFrame();
  setAutoLayout(inner, 'HORIZONTAL', SPACING.xs, 'CENTER');
  inner.primaryAxisSizingMode = 'AUTO';
  inner.counterAxisSizingMode = 'AUTO';
  clearFill(inner);

  /* 브랜드 이니셜 원형 배지: size-6(24px) rounded-full bg-brand text-brand-fg text-[10px] font-bold */
  const badge = figma.createFrame();
  setAutoLayout(badge, 'HORIZONTAL', 0, 'CENTER');
  badge.primaryAxisAlignItems = 'CENTER';
  badge.resize(24, 24);
  badge.primaryAxisSizingMode = 'FIXED';
  badge.counterAxisSizingMode = 'FIXED';
  await setFloatVar(badge, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(badge, COLOR_VAR.brandPrimary, BRAND.primary);
  /* text-[10px]: tokens에 10px 항목이 없어 xs(12px)로 근사, fallback 10 */
  const initial = await addTextWithVar(badge, 'H', 10, COLOR_VAR.brandFg, BRAND.fg, true, SIZE_VAR.fontSizeXs);
  initial.textAlignHorizontal = 'CENTER';
  inner.appendChild(badge);

  /* 브랜드명: text-lg font-bold text-brand-text tracking-tight */
  await addTextWithVar(inner, '하나은행', FONT_SIZE.lg, COLOR_VAR.brandText, BRAND.text, true, SIZE_VAR.fontSizeLg);

  comp.appendChild(inner);
  figma.currentPage.appendChild(comp);
  return comp;
}
