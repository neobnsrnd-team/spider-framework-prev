/**
 * @file createAlertBanner.ts
 * @description Figma AlertBanner 컴포넌트 세트 생성.
 * intent(warning|danger|success|info)를 Figma variant로 매핑한다.
 *
 * TEXT properties:
 *   - message — 알림 메시지 본문 (기본값: '알림 메시지입니다.')
 *
 * 컴포넌트 이름: "AlertBanner"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE, COLOR_VAR, SIZE_VAR } from '../../../utils/tokens';
import {
  createComponent, combineVariants, setAutoLayout, setPadding,
  setFill, setStroke, addTextWithVar,
} from '../../../utils/helpers';
import { createIcon, type IconName } from '../../../utils/icons';

type AlertIntent = 'Warning' | 'Danger' | 'Success' | 'Info';

const INTENT_ICON: Record<AlertIntent, IconName> = {
  Warning: 'AlertTriangle',
  Danger:  'AlertCircle',
  Success: 'CheckCircle2',
  Info:    'Info',
};

const INTENT_CONFIG: Record<AlertIntent, {
  bg:       Parameters<typeof setFill>[1];
  border:   Parameters<typeof setFill>[1];
  text:     Parameters<typeof setFill>[1];
  colorVar: string;
}> = {
  Warning: { bg: COLOR.warningSurface, border: COLOR.warningBorder, text: COLOR.warningText, colorVar: COLOR_VAR.warningText },
  Danger:  { bg: COLOR.dangerSurface,  border: COLOR.dangerBorder,  text: COLOR.dangerText,  colorVar: COLOR_VAR.dangerText  },
  Success: { bg: COLOR.successSurface, border: COLOR.successBorder, text: COLOR.successText, colorVar: COLOR_VAR.successText },
  Info:    { bg: COLOR.primarySurface, border: COLOR.border,        text: COLOR.primaryText, colorVar: COLOR_VAR.primaryText },
};

async function createAlertVariant(intent: AlertIntent): Promise<ComponentNode> {
  const { bg, border, text, colorVar } = INTENT_CONFIG[intent];
  const comp = createComponent(`Intent=${intent}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(328, 52);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  comp.counterAxisAlignItems = 'MIN';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, bg);
  setStroke(comp, border);

  comp.appendChild(createIcon(INTENT_ICON[intent], 18, text));

  /* message TEXT property — comp 직접 자식(1단계)이므로 addTextWithVar 내부에서 바인딩 가능 */
  const msg = await addTextWithVar(
    comp, '알림 메시지입니다.', FONT_SIZE.sm,
    colorVar, text,
    false, SIZE_VAR.fontSizeSm, 'message',
  );
  msg.layoutGrow = 1;

  return comp;
}

export async function createAlertBanner(): Promise<ComponentSetNode> {
  const intents: AlertIntent[] = ['Warning', 'Danger', 'Success', 'Info'];
  return combineVariants(await Promise.all(intents.map(createAlertVariant)), 'AlertBanner', 1);
}
