/**
 * @file createModal.ts
 * @description Figma Modal 컴포넌트 세트 생성.
 * BottomBtnCnt(0|1|2) = 3 variants. (Size / TitleAlign 제거)
 *
 * TEXT properties:
 *   - title           — 모달 제목 (기본값: '모달 제목')
 *   - content         — 본문 내용 (기본값: '모달 내용 영역입니다...')
 *   - bottomBtn1Label — BottomBtnCnt=1: 확인 버튼 / BottomBtnCnt=2: 취소 버튼
 *   - bottomBtn2Label — BottomBtnCnt=2: 확인 버튼
 *
 * [레이아웃]
 *   comp(VERTICAL, FIXED 360×280, surface, radiusXl)
 *     Header(HORIZONTAL, SPACE_BETWEEN): [title(grow=1)] [CloseButton(32×32)]
 *     content(TEXT, grow=1, FILL)
 *     [Footer — BottomBtnCnt > 0]
 *       [SecondaryButton(grow=1)?] [PrimaryButton(grow=1)]
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(header) 이후 title 바인딩 (2단계: comp → header → title).
 *   comp.appendChild(footer) 이후 bottomBtn1Label / bottomBtn2Label 바인딩.
 *
 * React 대응: packages/component-library/modules/common/Modal/index.tsx
 */
import { COLOR, BRAND, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFillWithVar, addTextWithVar, setFloatVar, clearFill, solid,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

const MODAL_W = 360;
const MODAL_H = 280;
const BTN_H   = 44;

async function createModalVariant(
  bottomBtnCnt: 0 | 1 | 2,
): Promise<ComponentNode> {
  const comp = createComponent(`BottomBtnCnt=${bottomBtnCnt}`);
  setAutoLayout(comp, 'VERTICAL', SPACING.md);
  setPadding(comp, SPACING.xl, SPACING.xl);
  comp.resize(MODAL_W, MODAL_H);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  await setFloatVar(comp, 'cornerRadius', SIZE_VAR.radiusXl, RADIUS.xl);
  await setFillWithVar(comp, COLOR_VAR.surface, COLOR.surface);

  /* ── 헤더: title + X 버튼 ─────────────────────────────────── */
  const header = figma.createFrame();
  header.name = 'Header';
  setAutoLayout(header, 'HORIZONTAL', 0, 'CENTER');
  header.primaryAxisAlignItems = 'SPACE_BETWEEN';
  header.fills = [];

  /* comp.appendChild(header) 이후에 title TEXT property 바인딩 (2단계 ✓) */
  comp.appendChild(header);
  header.layoutSizingHorizontal = 'FILL';

  const title = await addTextWithVar(
    header, '모달 제목', FONT_SIZE.base,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeBase, 'title', comp,
  );
  title.layoutGrow = 1;

  /* X 닫기 버튼 (32×32) */
  const closeBtn = figma.createFrame();
  closeBtn.name = 'CloseButton';
  setAutoLayout(closeBtn, 'HORIZONTAL', 0, 'CENTER');
  closeBtn.resize(32, 32);
  closeBtn.primaryAxisSizingMode = 'FIXED';
  closeBtn.counterAxisSizingMode = 'FIXED';
  await setFloatVar(closeBtn, 'cornerRadius', SIZE_VAR.radiusSm, RADIUS.sm);
  closeBtn.fills = [];
  closeBtn.appendChild(createIcon('X', 16, COLOR.textMuted));
  header.appendChild(closeBtn);

  /* ── 본문 텍스트 (comp 직접 자식, 자동 바인딩) ─────────────── */
  const content = await addTextWithVar(
    comp,
    '모달 내용 영역입니다.\n확인이 필요한 정보를 표시합니다.',
    FONT_SIZE.sm,
    COLOR_VAR.textBase, COLOR.textBase,
    false, SIZE_VAR.fontSizeSm, 'content',
  );
  content.layoutSizingHorizontal = 'FILL';
  content.layoutGrow = 1;

  /* ── 하단 버튼 (BottomBtnCnt > 0) ─────────────────────────── */
  if (bottomBtnCnt > 0) {
    const footer = figma.createFrame();
    footer.name = 'Footer';
    setAutoLayout(footer, 'HORIZONTAL', SPACING.sm);
    footer.fills = [];

    /* 버튼 생성 헬퍼 — footer.appendChild 후 layoutGrow 설정, TextNode 반환 */
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
        btn.strokes      = [solid(BRAND.primary)];
        btn.strokeWeight = 1;
        btn.strokeAlign  = 'INSIDE';
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

    /* comp.appendChild(footer) 이후 TEXT property 바인딩 */
    comp.appendChild(footer);
    footer.layoutSizingHorizontal = 'FILL';

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
 * Modal ComponentSet 생성.
 * BottomBtnCnt(0|1|2) = 3 variants, cols=3.
 */
export async function createModal(): Promise<ComponentSetNode> {
  const variants: ComponentNode[] = [];
  for (const bottomBtnCnt of [0, 1, 2] as (0 | 1 | 2)[]) {
    variants.push(await createModalVariant(bottomBtnCnt));
  }
  return combineVariants(variants, 'Modal', 3);
}
