/**
 * @file createRecentRecipientItem.ts
 * @description Figma RecentRecipientItem 컴포넌트 생성.
 * 최근 이체 수취인 목록 단일 항목: [아바타] [이름 + 은행명 + 마스킹 계좌] [ChevronRight].
 * 단일 variant.
 *
 * TEXT properties:
 *   - name          — 수취인 이름 (기본값: '홍길동')
 *   - bankName      — 은행명 (기본값: '하나은행')
 *   - maskedAccount — 마스킹 처리된 계좌번호 (기본값: '123-****-789012')
 *
 * TEXT property 바인딩 타이밍:
 *   comp.appendChild(textGroup) 이후 name / bankName / maskedAccount 바인딩
 *   (2단계: comp → textGroup → text).
 *
 * 컴포넌트 이름: "RecentRecipientItem"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, setAutoLayout, setPadding, clearFill,
  setFillWithVar, addTextWithVar, setFloatVar,
} from '../../../utils/helpers';
import { createIcon } from '../../../utils/icons';

export async function createRecentRecipientItem(): Promise<ComponentNode> {
  const comp = createComponent('RecentRecipientItem');
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.sm, SPACING.md);
  comp.resize(390, 60);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  clearFill(comp);

  /* 아바타 원형 프레임 (36×36) */
  const avatar = figma.createFrame();
  avatar.name = 'Avatar';
  setAutoLayout(avatar, 'HORIZONTAL', 0, 'CENTER');
  avatar.resize(36, 36);
  avatar.primaryAxisSizingMode = 'FIXED';
  avatar.counterAxisSizingMode = 'FIXED';
  await setFloatVar(avatar, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);
  await setFillWithVar(avatar, COLOR_VAR.surfaceRaised, COLOR.surfaceRaised);
  avatar.appendChild(createIcon('User', 16, COLOR.textMuted));
  comp.appendChild(avatar);

  /* 텍스트 그룹 (이름 + 은행명/계좌) — comp.appendChild 이후 TEXT property 바인딩 (2단계 ✓) */
  const textGroup = figma.createFrame();
  textGroup.name = 'TextGroup';
  setAutoLayout(textGroup, 'VERTICAL', SPACING.xs, 'MIN');
  textGroup.primaryAxisSizingMode = 'AUTO';
  textGroup.counterAxisSizingMode = 'AUTO';
  clearFill(textGroup);
  comp.appendChild(textGroup);
  textGroup.layoutGrow = 1;

  /* 수취인 이름 */
  await addTextWithVar(
    textGroup, '홍길동', FONT_SIZE.sm,
    COLOR_VAR.textHeading, COLOR.textHeading,
    true, SIZE_VAR.fontSizeSm, 'name', comp,
  );

  /* 은행명 + 마스킹 계좌를 한 행에 표시하는 서브 텍스트 행 */
  const subRow = figma.createFrame();
  subRow.name = 'SubRow';
  setAutoLayout(subRow, 'HORIZONTAL', SPACING.xs, 'MIN');
  subRow.primaryAxisSizingMode = 'AUTO';
  subRow.counterAxisSizingMode = 'AUTO';
  clearFill(subRow);
  textGroup.appendChild(subRow);

  /* bankName — subRow 내부(3단계)이므로 addComponentProperty 수동 바인딩 */
  const bankNameText = await addTextWithVar(
    subRow, '하나은행', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  const bankNameKey = comp.addComponentProperty('bankName', 'TEXT', '하나은행');
  bankNameText.componentPropertyReferences = { characters: bankNameKey };

  /* maskedAccount */
  const maskedAccountText = await addTextWithVar(
    subRow, '123-****-789012', FONT_SIZE.xs,
    COLOR_VAR.textMuted, COLOR.textMuted,
    false, SIZE_VAR.fontSizeXs,
  );
  const maskedAccountKey = comp.addComponentProperty('maskedAccount', 'TEXT', '123-****-789012');
  maskedAccountText.componentPropertyReferences = { characters: maskedAccountKey };

  /* ChevronRight 아이콘 */
  comp.appendChild(createIcon('ChevronRight', 16, COLOR.textMuted));

  return comp;
}
