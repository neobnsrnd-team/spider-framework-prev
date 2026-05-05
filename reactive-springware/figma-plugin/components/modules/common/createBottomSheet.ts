/**
 * @file createBottomSheet.ts
 * @description Figma BottomSheet 컴포넌트 세트 생성.
 *
 * HideCloseButton(False|True) × BottomBtnCnt(0|1|2) = 6 variants.
 *
 * TEXT properties:
 *   - title            — 바텀시트 제목 (모든 variant)
 *   - bottomBtn1Label  — BottomBtnCnt=1: 확인 버튼 / BottomBtnCnt=2: 취소 버튼
 *   - bottomBtn2Label  — BottomBtnCnt=2: 확인 버튼
 *
 * [레이아웃 구조]
 *   DragHandle
 *   Header: [Spacer(32) | title(grow, CENTER) | CloseButton or Placeholder(32)]
 *   Content (Slot, layoutGrow=1)
 *   Footer (BottomBtnCnt > 0): [SecondaryButton(grow)?] [PrimaryButton(grow)]
 *
 * HideCloseButton은 Variant 속성으로 제어 (BOOLEAN property 아님).
 * → HideCloseButton=True variant에서는 CloseButton 자리에 빈 Placeholder를 배치해 title 중앙 정렬을 유지한다.
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(header) 이후에 title, comp.appendChild(footer) 이후에 버튼 레이블 바인딩.
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  clearFill, setFillWithVar, addTextWithVar, setFloatVar, solid,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const SHEET_W   = 390;
const SHEET_H   = 480;
const BTN_H     = 48;

async function createBottomSheetVariant(
  hideCloseButton: boolean,
  bottomBtnCnt: 0 | 1 | 2,
): Promise<ComponentNode> {
  const comp = createComponent(
    `HideCloseButton=${hideCloseButton ? 'True' : 'False'}, BottomBtnCnt=${bottomBtnCnt}`,
  );

  setAutoLayout(comp, 'VERTICAL', SPACING.sm, 'CENTER');
  comp.paddingTop = SPACING.standard; /* drag handle 상단 여백 */
  comp.resize(SHEET_W, SHEET_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  /* rounded-t-2xl → RADIUS.lg */
  await setFloatVar(comp, 'topLeftRadius',  SIZE_VAR.radiusLg, RADIUS.lg);
  await setFloatVar(comp, 'topRightRadius', SIZE_VAR.radiusLg, RADIUS.lg);
  comp.bottomLeftRadius  = 0;
  comp.bottomRightRadius = 0;
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* ── Drag Handle ─────────────────────────────────────────── */
  const handle = figma.createFrame();
  handle.name = 'DragHandle';
  handle.resize(40, 4);
  handle.cornerRadius = RADIUS.full;
  await setFillWithVar(handle, COLOR_VAR.border, COLOR.border);
  comp.appendChild(handle);

  /* ── Header ──────────────────────────────────────────────── */
  const header = figma.createFrame();
  header.name = 'Header';
  setAutoLayout(header, 'HORIZONTAL', 0, 'CENTER');
  setPadding(header, SPACING.sm, SPACING.xl, SPACING.md, SPACING.xl);
  clearFill(header);
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  /* 좌측 Spacer — CloseButton과 너비를 맞춰 title 중앙 정렬 유지 */
  const spacer = figma.createFrame();
  spacer.name = 'Spacer';
  spacer.resize(32, 32);
  clearFill(spacer);
  header.appendChild(spacer);

  /* title TEXT property (comp → header → titleText, 2단계) */
  const titleText = await addTextWithVar(
    header, '바텀시트 제목', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase,
  );
  titleText.layoutGrow = 1;
  titleText.textAlignHorizontal = 'CENTER';
  const titleKey = comp.addComponentProperty('title', 'TEXT', '바텀시트 제목');
  titleText.componentPropertyReferences = { characters: titleKey };

  /* 우측: CloseButton or Placeholder (32×32, 동일 너비로 title 중앙 유지) */
  const rightSlot = figma.createFrame();
  rightSlot.name = hideCloseButton ? 'Placeholder' : 'CloseButton';
  setAutoLayout(rightSlot, 'HORIZONTAL', 0, 'CENTER');
  rightSlot.resize(32, 32);
  rightSlot.primaryAxisSizingMode = 'FIXED';
  rightSlot.counterAxisSizingMode = 'FIXED';
  rightSlot.cornerRadius = RADIUS.sm;
  clearFill(rightSlot);
  if (!hideCloseButton) {
    rightSlot.appendChild(createIcon('X', 16, COLOR.textMuted));
  }
  header.appendChild(rightSlot);

  /* ── Content Slot (header와 footer 사이 자유 영역) ─────────── */
  const slot = comp.createSlot();
  slot.name = 'Content';
  clearFill(slot);
  slot.layoutSizingHorizontal = 'FILL';
  slot.layoutGrow = 1;

  /* ── Footer (BottomBtnCnt > 0) ───────────────────────────── */
  if (bottomBtnCnt > 0) {
    const footer = figma.createFrame();
    footer.name = 'Footer';
    setAutoLayout(footer, 'HORIZONTAL', SPACING.sm);
    setPadding(footer, SPACING.md, SPACING.xl, SPACING.xl, SPACING.xl);
    clearFill(footer);
    /* border-t border-border-subtle 시뮬레이션 */
    footer.strokes          = [solid(COLOR.borderSubtle)];
    footer.strokeTopWeight  = 1;
    footer.strokeBottomWeight = 0;
    footer.strokeLeftWeight = 0;
    footer.strokeRightWeight = 0;
    footer.strokeAlign = 'INSIDE';
    comp.appendChild(footer);
    footer.layoutSizingHorizontal = 'FILL';

    /* 버튼 생성 헬퍼 — footer.appendChild 후 호출, btn은 level 2, text는 level 3 */
    const makeBtn = async (
      style: 'primary' | 'outline',
      label: string,
    ): Promise<TextNode> => {
      const btn = figma.createFrame();
      btn.name = style === 'primary' ? 'PrimaryButton' : 'SecondaryButton';
      setAutoLayout(btn, 'HORIZONTAL', 0, 'CENTER');
      btn.resize(80, BTN_H);
      btn.counterAxisSizingMode = 'FIXED';
      await setFloatVar(btn, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

      if (style === 'primary') {
        await setFillWithVar(btn, COLOR_VAR.brandPrimary, BRAND.primary);
      } else {
        clearFill(btn);
        btn.strokes     = [solid(BRAND.primary)];
        btn.strokeWeight = 1;
        btn.strokeAlign = 'INSIDE';
      }

      const textColor    = style === 'primary' ? COLOR_VAR.brandFg   : COLOR_VAR.brandText;
      const textFallback = style === 'primary' ? BRAND.fg            : BRAND.text;
      const text = await addTextWithVar(
        btn, label, FONT_SIZE.sm,
        textColor, textFallback, true, SIZE_VAR.fontSizeSm,
      );
      text.textAlignHorizontal = 'CENTER';

      footer.appendChild(btn);
      btn.layoutGrow = 1;
      return text;
    };

    let btn1Text: TextNode;
    let btn2Text: TextNode | undefined;

    if (bottomBtnCnt === 1) {
      btn1Text = await makeBtn('primary', '확인');
    } else {
      /* 2개일 때: outline 취소(btn2)가 왼쪽, primary 확인(btn1)이 오른쪽 */
      btn2Text = await makeBtn('outline', '취소');
      btn1Text = await makeBtn('primary', '확인');
    }
    
    /* TEXT property 바인딩 — comp.appendChild(footer) 완료 후 수행 */
    /* BottomBtnCnt=1: btn1=단독 확인 / BottomBtnCnt=2: btn1=확인, btn2=취소 */
    const btn1Key = comp.addComponentProperty('bottomBtn1Label', 'TEXT', '확인');
    btn1Text.componentPropertyReferences = { characters: btn1Key };
    
    if (btn2Text) {
      const btn2Key = comp.addComponentProperty('bottomBtn2Label', 'TEXT', '취소');
      btn2Text.componentPropertyReferences = { characters: btn2Key };
    }
  }

  return comp;
}

/**
 * BottomSheet ComponentSet 생성.
 * HideCloseButton(False|True) × BottomBtnCnt(0|1|2) = 6 variants, cols=3.
 * Row 1: HideCloseButton=False — BottomBtnCnt 0/1/2
 * Row 2: HideCloseButton=True  — BottomBtnCnt 0/1/2
 */
export async function createBottomSheet(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];
  for (const hideCloseButton of [false, true]) {
    for (const bottomBtnCnt of [0, 1, 2] as (0 | 1 | 2)[]) {
      variants.push(await createBottomSheetVariant(hideCloseButton, bottomBtnCnt));
    }
  }
  return combineVariants(variants, 'BottomSheet', 3);
}
